package com.kexu.app.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.AppWidgetId
import com.kexu.app.KexuApplication
import com.kexu.app.data.KexuDatabase
import com.kexu.app.data.model.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Debug-only real AppWidgetHost for repeatable size/theme and PendingIntent acceptance checks. */
class WidgetQaActivity : ComponentActivity() {
    private lateinit var host: AppWidgetHost
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host=AppWidgetHost(this,8170)
        host.startListening()
        lifecycleScope.launch {
            val app=application as KexuApplication
            if(intent.getBooleanExtra("seed",false)) withContext(Dispatchers.IO) {seed(app)}
            if(intent.getBooleanExtra("focus_test",false)) {
                val prefs=getSharedPreferences("widget_qa",MODE_PRIVATE)
                if(!prefs.contains("original_timer")) {
                    val t=app.settingsRepository.snapshot().focusTimer
                    val json=org.json.JSONObject().put("positive",t.positiveMode).put("duration",t.durationMinutes)
                        .put("remaining",t.remainingSeconds).put("elapsed",t.elapsedSeconds).put("running",t.running)
                        .put("started",t.startedAtMillis).put("anchor",t.anchorAtMillis).put("task",t.selectedTaskId ?: -1L)
                    prefs.edit().putString("original_timer",json.toString()).commit()
                    app.settingsRepository.setFocusTimer(com.kexu.app.settings.FocusTimerSettings(durationMinutes=1,remainingSeconds=60,
                        selectedTaskId=prefs.getString("tasks","").orEmpty().split(',').firstOrNull()?.toLongOrNull()))
                }
            }
            if(intent.getBooleanExtra("cleanup",false)) {
                withContext(Dispatchers.IO) {cleanup(app)}
                host.deleteHost();getSharedPreferences("widget_qa",MODE_PRIVATE).edit().clear().apply()
                updateAllHomeWidgets(this@WidgetQaActivity);finish();return@launch
            }
            val kind=intent.getStringExtra("widget") ?: "today"
            val receiver=when(kind) {
                "habit"->HabitWidgetReceiver::class.java
                "countdown"->CountdownWidgetReceiver::class.java
                "countdown_list"->CountdownListWidgetReceiver::class.java
                "stats"->TaskStatsWidgetReceiver::class.java
                "pomodoro"->PomodoroRemoteWidgetReceiver::class.java
                else->TodayWidgetReceiver::class.java
            }
            val manager=AppWidgetManager.getInstance(this@WidgetQaActivity)
            val prefs=getSharedPreferences("widget_qa",MODE_PRIVATE)
            var id=prefs.getInt("host_$kind",0)
            if(id==0 || manager.getAppWidgetInfo(id)==null) {
                id=host.allocateAppWidgetId()
                check(manager.bindAppWidgetIdIfAllowed(id,ComponentName(this@WidgetQaActivity,receiver))) {"Grant debug widget binding first"}
                prefs.edit().putInt("host_$kind",id).apply()
            }
            val width=intent.getIntExtra("width",if(kind in listOf("today","countdown_list","pomodoro")) 350 else 170)
            val height=intent.getIntExtra("height",if(kind=="pomodoro") 64 else 190)
            // Publish the new bounds before Glance starts from the preference update.
            // Otherwise a reused test host can initially render its previous size.
            manager.updateAppWidgetOptions(id,Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height)
                if(android.os.Build.VERSION.SDK_INT>=31) putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,arrayListOf(android.util.SizeF(width.toFloat(),height.toFloat())))
            })
            val settings=WidgetPreferences.settings(this@WidgetQaActivity,id).copy(
                theme=if(intent.getBooleanExtra("dark",false)) WidgetTheme.DARK else WidgetTheme.LIGHT,
                opacity=intent.getIntExtra("opacity",100),selectedItemId=intent.getLongExtra("selected",0),
            )
            WidgetPreferences.save(this@WidgetQaActivity,id,settings)
            if(kind!="pomodoro") androidx.glance.appwidget.state.updateAppWidgetState(this@WidgetQaActivity,widgetGlanceId(this@WidgetQaActivity, id)) {
                it[WidgetThemeStateKey]=settings.theme.name;it[WidgetOpacityStateKey]=settings.opacity
            }
            val density=resources.displayMetrics.density
            val body=LinearLayout(this@WidgetQaActivity).apply {
                orientation=LinearLayout.VERTICAL;setPadding(20,40,20,40);setBackgroundColor(android.graphics.Color.rgb(222,216,220))
            }
            body.addView(TextView(this@WidgetQaActivity).apply {text="QA · $kind · ${width}×${height}dp · ${settings.theme}";textSize=14f;setPadding(12,24,12,30)})
            val view=host.createView(this@WidgetQaActivity,id,manager.getAppWidgetInfo(id))
            body.addView(view,LinearLayout.LayoutParams((width*density).toInt(),(height*density).toInt()))
            setContentView(ScrollView(this@WidgetQaActivity).apply {addView(body)})
            view.updateAppWidgetSize(Bundle(),width,height,width,height)
            updateAllHomeWidgets(this@WidgetQaActivity)
        }
    }

    private suspend fun seed(app: KexuApplication) {
        val prefs=getSharedPreferences("widget_qa",MODE_PRIVATE)
        if(prefs.getBoolean("seeded",false)) return
        val today=LocalDate.now();val zone=ZoneId.systemDefault()
        val ids=mutableListOf<Long>()
        listOf("整理今天的课堂笔记","读完一本书里的一个小章节","把数据库实验最后的查询结果检查一遍，这是长标题显示测试","给自己留十五分钟休息","准备明天的课堂材料","今天也有认真向前走").forEachIndexed { i,title ->
            val task=TaskEntity(title="[QA] $title",dueAt=today.atTime(18,i*5).atZone(zone).toInstant().toEpochMilli())
            val id=app.repository.saveTask(task);ids+=id
            if(i==5) app.repository.updateTaskIfUnchanged(task.copy(id=id),task.copy(id=id,isCompleted=true,completedAt=System.currentTimeMillis()))
        }
        prefs.edit().putString("tasks",ids.joinToString(",")).apply()
        val habit=app.repository.saveHabit(HabitEntity(title="[QA] 每天读几页喜欢的书",symbol="📖"))
        (1L..4L).forEach {app.repository.saveHabitCheck(HabitCheckEntity(habit,today.minusDays(it).toEpochDay()))}
        prefs.edit().putLong("habit",habit).apply()
        val countdowns=listOf(-6L,0L,7L,35L,80L).map { days ->
            app.repository.saveCountdown(CountdownEntity(name="[QA] "+when(days) {-6L->"第一次出发";0L->"今天的好日子";7L->"期待下一次周末旅行";35L->"一个很长很长值得期待的纪念日名称";else->"学期结束"},
                targetDateEpochDay=today.plusDays(days).toEpochDay(),symbol="✦"))
        }
        prefs.edit().putString("countdowns",countdowns.joinToString(",")).putBoolean("seeded",true).apply()
        val dao=KexuDatabase.get(app).taskDao()
        (1L..6L).forEach { ago -> repeat((7-ago).toInt()%4) {
            dao.insertCompletion(TaskCompletionEntity(taskId=ids.first(),title="[QA] 过去的一小步",completedAt=today.minusDays(ago).atTime(19,0).atZone(zone).toInstant().toEpochMilli()))
        } }
    }

    private suspend fun cleanup(app: KexuApplication) {
        val prefs=getSharedPreferences("widget_qa",MODE_PRIVATE)
        val ids=prefs.getString("tasks","").orEmpty().split(',').mapNotNull {it.toLongOrNull()}
        ids.forEach {id ->
            app.repository.task(id)?.let {app.repository.deleteTask(it)}
            KexuDatabase.get(app).openHelper.writableDatabase.execSQL("DELETE FROM task_completions WHERE taskId = ?",arrayOf(id))
            KexuDatabase.get(app).openHelper.writableDatabase.execSQL("DELETE FROM focus_sessions WHERE taskId = ?",arrayOf(id))
        }
        app.repository.currentHabits().firstOrNull {it.id==prefs.getLong("habit",0)}?.let {app.repository.deleteHabit(it)}
        val countdowns=prefs.getString("countdowns","").orEmpty().split(',').mapNotNull {it.toLongOrNull()}
        app.repository.currentCountdowns().filter {it.id in countdowns}.forEach {app.repository.deleteCountdown(it)}
        KexuDatabase.get(app).openHelper.writableDatabase.execSQL("DELETE FROM day_reflections WHERE note = ?",arrayOf("QA_rhythm_note"))
        prefs.getString("original_timer",null)?.let {raw ->
            val j=org.json.JSONObject(raw)
            app.settingsRepository.setFocusTimer(com.kexu.app.settings.FocusTimerSettings(
                positiveMode=j.getBoolean("positive"),durationMinutes=j.getInt("duration"),remainingSeconds=j.getInt("remaining"),
                elapsedSeconds=j.getInt("elapsed"),running=j.getBoolean("running"),startedAtMillis=j.getLong("started"),
                anchorAtMillis=j.getLong("anchor"),selectedTaskId=j.getLong("task").takeIf {it>=0},
            ))
        }
    }
    override fun onDestroy() {host.stopListening();super.onDestroy()}
}
