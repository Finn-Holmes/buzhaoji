package com.kexu.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.kexu.app.KexuApplication
import com.kexu.app.MainActivity
import com.kexu.app.R
import com.kexu.app.data.model.CountdownEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlin.math.abs

internal fun countdownDays(item: CountdownEntity, today: LocalDate = LocalDate.now()): Long {
    val original=LocalDate.ofEpochDay(item.targetDateEpochDay)
    var target=original
    if(item.repeatYearly) {
        target=original.withYear(today.year)
        if(target.isBefore(today)) target=original.withYear(today.year+1)
    }
    return ChronoUnit.DAYS.between(today,target)
}

internal fun countdownLabel(days: Long): String = when {
    days==0L -> "就是今天"; days<0L -> "已经走过"; else -> "还有这么多天"
}

internal fun selectCountdown(items: List<CountdownEntity>, selected: Long, today: LocalDate): CountdownEntity? =
    items.firstOrNull {it.id==selected} ?: items.filter {countdownDays(it,today)>=0}.minByOrNull {countdownDays(it,today)}
    ?: items.minByOrNull {abs(countdownDays(it,today))}

internal fun widgetOpen(context: Context, action: String) = actionStartActivity(
    Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MainActivity.EXTRA_WIDGET_ACTION,action),
)

internal fun widgetSettingsAction(context: Context,id: GlanceId) = actionStartActivity(
    Intent(context,WidgetConfigActivity::class.java)
        .setData(android.net.Uri.parse("kexu://widget/${WidgetPreferences.appWidgetId(context,id)}/settings"))
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,WidgetPreferences.appWidgetId(context,id)),
)

internal data class RhythmPalette(val text: ColorProvider,val muted: ColorProvider,val accent: ColorProvider,val track: ColorProvider)
internal fun rhythmPalette(dark: Boolean) = RhythmPalette(
    ColorProvider(if(dark) Color(0xFFF7EDF6) else Color(0xFF3E3649)),
    ColorProvider(if(dark) Color(0xFFC4B8CD) else Color(0xFF756A7B)),
    ColorProvider(if(dark) Color(0xFFF0C3AE) else Color(0xFF92596C)),
    ColorProvider(if(dark) Color(0xFF514859) else Color(0xFFE6DCE7)),
)

@Composable
internal fun widgetToday(): LocalDate {
    val date by produceState(LocalDate.now()) {
        while(true) {value=LocalDate.now();delay(30_000)}
    }
    return date
}

@Composable
internal fun RhythmWidgetMenu(context: Context,id: GlanceId,colors: RhythmPalette) {
    Image(ImageProvider(R.drawable.widget_ic_more_pressable),"小组件设置",
        modifier=GlanceModifier.width(40.dp).height(40.dp).clickable(widgetSettingsAction(context,id)).padding(9.dp),
        colorFilter=ColorFilter.tint(colors.muted))
}

class HabitWidget : KexuGlanceWidget() {
    override val sizeMode=SizeMode.Exact
    override suspend fun provideGlance(context: Context,id: GlanceId) {
        val app=context.applicationContext as KexuApplication
        val initial=app.repository.currentHabits()
        provideContent {
            val habits by app.repository.habits.collectAsState(initial)
            val checks by app.repository.habitChecks.collectAsState(emptyList())
            val settings by WidgetPreferences.observe(context,id).collectAsState(WidgetPreferences.settings(context,id))
            val today=widgetToday()
            val habit=habits.firstOrNull {it.id==settings.selectedItemId} ?: habits.firstOrNull()
            val checked=checks.any {it.habitId==habit?.id && it.dateEpochDay==today.toEpochDay()}
            val dark=widgetIsDark(context,id)
            val colors=rhythmPalette(dark)
            val roomy=LocalSize.current.height>=175.dp
            val tiny=LocalSize.current.height<140.dp
            Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                .background(widgetSurface(context,id,darkColor=Color(0xFF2E3540),lightColor=Color(0xFFF0F3E9))).padding(horizontal=12.dp,vertical=8.dp)) {
                Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(tiny) habit?.title ?: "添加习惯" else "日常一小步",GlanceModifier.defaultWeight().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_HABITS)),style=TextStyle(color=colors.muted,fontSize=11.sp),maxLines=1)
                    RhythmWidgetMenu(context,id,colors)
                }
                if(!tiny) Text(habit?.title ?: "种下一个小习惯",GlanceModifier.fillMaxWidth().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_HABITS)),
                    style=TextStyle(color=colors.text,fontSize=17.sp,fontWeight=FontWeight.Bold),maxLines=if(roomy) 2 else 1)
                Spacer(GlanceModifier.defaultWeight())
                if(roomy && habit!=null) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        (6L downTo 0L).forEach { ago ->
                            val done=checks.any {it.habitId==habit.id && it.dateEpochDay==today.minusDays(ago).toEpochDay()}
                            Box(GlanceModifier.defaultWeight().height(24.dp),contentAlignment=Alignment.Center) {
                                Text(if(done) "●" else "·",style=TextStyle(color=if(done) colors.accent else colors.muted,fontSize=16.sp))
                            }
                        }
                    }
                }
                Row(GlanceModifier.fillMaxWidth().height(44.dp).cornerRadius(16.dp).background(colors.track)
                    .clickable(if(habit==null) widgetOpen(context,MainActivity.ACTION_OPEN_HABITS) else
                        actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to habit.id,
                            HabitDateKey to today.toEpochDay(),HabitCheckedTargetKey to !checked))),
                    verticalAlignment=Alignment.CenterVertically,horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(habit==null) "＋  添加习惯" else if(checked) {if(tiny) "✓  已完成" else "✓  今天已做到"} else {if(tiny) "○  今日打卡" else "○  轻点，完成今天"},
                        style=TextStyle(color=colors.text,fontSize=12.sp,fontWeight=FontWeight.Medium),maxLines=1)
                }
            }
        }
    }
}
class HabitWidgetReceiver : GlanceAppWidgetReceiver() {override val glanceAppWidget=HabitWidget()}

class CountdownWidget : KexuGlanceWidget() {
    override val sizeMode=SizeMode.Exact
    override suspend fun provideGlance(context: Context,id: GlanceId) {
        val app=context.applicationContext as KexuApplication
        val initial=app.repository.currentCountdowns()
        provideContent {
            val items by app.repository.countdowns.collectAsState(initial)
            val settings by WidgetPreferences.observe(context,id).collectAsState(WidgetPreferences.settings(context,id))
            val today=widgetToday()
            val item=selectCountdown(items,settings.selectedItemId,today)
            val colors=rhythmPalette(widgetIsDark(context,id))
            val roomy=LocalSize.current.height>=180.dp
            val tiny=LocalSize.current.height<145.dp
            Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                .background(widgetSurface(context,id,darkColor=Color(0xFF383040),lightColor=Color(0xFFF9EEE7)))
                .padding(horizontal=14.dp,vertical=8.dp)) {
                Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(tiny) item?.name ?: "添加好日子" else "值得期待",GlanceModifier.defaultWeight().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_COUNTDOWN)),style=TextStyle(color=colors.muted,fontSize=11.sp),maxLines=if(tiny) 2 else 1)
                    RhythmWidgetMenu(context,id,colors)
                }
                Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_COUNTDOWN))) {
                    if(!tiny) Text(item?.name ?: "添加一个好日子",maxLines=if(roomy) 2 else 1,style=TextStyle(color=colors.text,fontSize=15.sp,fontWeight=FontWeight.Bold))
                    Spacer(GlanceModifier.defaultWeight())
                    val days=item?.let {countdownDays(it,today)}
                    Text(days?.let {abs(it).toString()} ?: "—",maxLines=1,style=TextStyle(color=colors.accent,
                        fontSize=when {roomy -> 44.sp; LocalSize.current.height<125.dp -> 24.sp; else -> 32.sp},fontWeight=FontWeight.Bold))
                    Text(days?.let(::countdownLabel) ?: "把期待放在这里",maxLines=1,style=TextStyle(color=colors.muted,fontSize=11.sp))
                }
            }
        }
    }
}
class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {override val glanceAppWidget=CountdownWidget()}

class CountdownListWidget : KexuGlanceWidget() {
    override val sizeMode=SizeMode.Exact
    override suspend fun provideGlance(context: Context,id: GlanceId) {
        val app=context.applicationContext as KexuApplication
        val initial=app.repository.currentCountdowns()
        provideContent {
            val entries by app.repository.countdowns.collectAsState(initial)
            val today=widgetToday()
            val items=entries.sortedWith(compareBy<CountdownEntity>{countdownDays(it,today)<0}.thenBy {abs(countdownDays(it,today))})
            val colors=rhythmPalette(widgetIsDark(context,id))
            Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                .background(widgetSurface(context,id,darkColor=Color(0xFF302C40),lightColor=Color(0xFFFAF4EE)))
                .padding(horizontal=14.dp,vertical=8.dp)) {
                Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("日子里的期待",GlanceModifier.defaultWeight().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_COUNTDOWN)),
                        style=TextStyle(color=colors.text,fontSize=16.sp,fontWeight=FontWeight.Bold))
                    RhythmWidgetMenu(context,id,colors)
                }
                if(items.isEmpty()) Text("＋ 添加一个值得记住的日子",GlanceModifier.fillMaxWidth().height(44.dp)
                    .clickable(widgetOpen(context,MainActivity.ACTION_OPEN_COUNTDOWN)).padding(vertical=12.dp),style=TextStyle(color=colors.muted,fontSize=13.sp))
                else LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    items(items,itemId={it.id}) { item ->
                        Row(GlanceModifier.fillMaxWidth().height(46.dp).clickable(widgetOpen(context,MainActivity.ACTION_OPEN_COUNTDOWN)),verticalAlignment=Alignment.CenterVertically) {
                            Text("${item.symbol}  ${item.name}",GlanceModifier.defaultWeight(),maxLines=1,style=TextStyle(color=colors.text,fontSize=14.sp))
                            Spacer(GlanceModifier.width(10.dp))
                            val days=countdownDays(item,today)
                            Text(when {days==0L->"今天";days<0->"已过 ${abs(days)} 天";else->"还有 $days 天"},
                                style=TextStyle(color=colors.accent,fontSize=12.sp,fontWeight=FontWeight.Medium),maxLines=1)
                        }
                    }
                }
            }
        }
    }
}
class CountdownListWidgetReceiver : GlanceAppWidgetReceiver() {override val glanceAppWidget=CountdownListWidget()}

class TaskStatsWidget : KexuGlanceWidget() {
    override val sizeMode=SizeMode.Exact
    override suspend fun provideGlance(context: Context,id: GlanceId) {
        val app=context.applicationContext as KexuApplication
        val initial=app.repository.currentCompletions()
        provideContent {
            val completions by app.repository.completions.collectAsState(initial)
            val today=widgetToday()
            val zone=java.time.ZoneId.systemDefault()
            val counts=(6L downTo 0L).map { ago -> completions.count {
                java.time.Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate()==today.minusDays(ago)
            } }
            val colors=rhythmPalette(widgetIsDark(context,id))
            val roomy=LocalSize.current.height>=175.dp
            Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp)
                .background(widgetSurface(context,id,darkColor=Color(0xFF302C40),lightColor=Color(0xFFF4EEF8)))
                .padding(horizontal=12.dp,vertical=8.dp)) {
                Row(GlanceModifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(roomy) "每一点，都算数" else "一周足迹",GlanceModifier.defaultWeight(),maxLines=1,style=TextStyle(color=colors.muted,fontSize=11.sp))
                    RhythmWidgetMenu(context,id,colors)
                }
                Column(GlanceModifier.fillMaxWidth().defaultWeight().clickable(widgetOpen(context,MainActivity.ACTION_OPEN_FOOTPRINTS))) {
                    Text(if(roomy) "${counts.last()} 件" else "今日 ${counts.last()} 件",maxLines=1,style=TextStyle(color=colors.text,
                        fontSize=when {roomy -> 30.sp; LocalSize.current.height<125.dp -> 16.sp; else -> 19.sp},fontWeight=FontWeight.Bold))
                    if(roomy) Text("今天完成",style=TextStyle(color=colors.muted,fontSize=10.sp))
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth().height(if(roomy) 42.dp else 14.dp),verticalAlignment=Alignment.Bottom) {
                        val max=counts.maxOrNull()?.coerceAtLeast(1) ?: 1
                        counts.forEach { count ->
                            Box(GlanceModifier.defaultWeight().fillMaxHeight(),contentAlignment=Alignment.BottomCenter) {
                                Box(GlanceModifier.width(9.dp).height((3+(if(roomy) 35 else 11)*count.toFloat()/max).dp)
                                    .cornerRadius(5.dp).background(if(count>0) colors.accent else colors.track)) {}
                            }
                        }
                    }
                    Spacer(GlanceModifier.height(5.dp))
                    Text("近 7 天 · ${counts.sum()} 件",style=TextStyle(color=colors.muted,fontSize=10.sp))
                }
            }
        }
    }
}
class TaskStatsWidgetReceiver : GlanceAppWidgetReceiver() {override val glanceAppWidget=TaskStatsWidget()}
