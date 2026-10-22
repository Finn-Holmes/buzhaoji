package com.kexu.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kexu.app.data.model.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun Long.rhythmDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

data class RhythmDay(val date: LocalDate, val completed: Int, val focusSeconds: Int, val habits: Int)

internal fun rhythmDay(state: KexuUiState, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) = RhythmDay(
    date,
    state.completions.count { it.completedAt.rhythmDate(zone) == date },
    state.focusSessions.filter { it.completedAt.rhythmDate(zone) == date }.sumOf { it.durationSeconds },
    state.habitChecks.count { it.dateEpochDay == date.toEpochDay() },
)

@Composable
internal fun rememberRhythmDate(): LocalDate {
    var date by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { date = LocalDate.now(); delay(15_000) }
    }
    return date
}

/** A drawn companion: no old mascot assets; system animation scale is respected by Compose. */
@Composable
internal fun QuietCompanion(
    resting: Boolean,
    moving: Boolean,
    modifier: Modifier = Modifier,
    celebrating: Boolean = false,
) {
    val breath = if (moving && !resting) {
        val transition = rememberInfiniteTransition(label = "quiet-breath")
        transition.animateFloat(.96f, 1.04f,
            infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath").value
    } else 1f
    val glow by animateFloatAsState(if (celebrating && moving) 1.18f else 1f, tween(500), label = "completion-glow")
    Canvas(modifier.semantics { contentDescription = if (resting) "小光团正在休息" else "小光团陪你慢慢来" }) {
        val c = Offset(size.width * .5f, size.height * .48f)
        val r = size.minDimension * .27f * breath * glow
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF5C69B).copy(alpha=.24f), Color.Transparent), c, r * 1.85f), r * 1.85f, c)
        drawOval(Color.Black.copy(alpha=.10f), Offset(c.x-r*.8f, c.y+r*1.35f), Size(r*1.6f, r*.18f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6D9), Color(0xFFF0BEA2)), c-Offset(r*.3f,r*.3f), r*1.8f),r,c)
        val ink = Color(0xFF65515F)
        if (resting) {
            listOf(-.28f,.28f).forEach { x ->
                drawArc(ink, 10f, 160f, false, Offset(c.x+r*x-r*.12f,c.y-r*.06f),Size(r*.24f,r*.12f), style=Stroke(r*.045f))
            }
        } else {
            drawCircle(ink,r*.045f,c+Offset(-r*.28f,0f)); drawCircle(ink,r*.045f,c+Offset(r*.28f,0f))
        }
        drawArc(ink, 15f, 150f, false, c+Offset(-r*.10f,r*.12f),Size(r*.20f,r*.12f),style=Stroke(r*.025f))
        listOf(Offset(.14f,.22f),Offset(.83f,.28f),Offset(.76f,.73f)).forEachIndexed { i, p ->
            val s=Offset(size.width*p.x,size.height*p.y)
            drawCircle(Color(0xFFFFE1B9).copy(alpha=if(resting) .45f else .75f),size.minDimension*(if(i==1) .018f else .009f),s)
        }
    }
}

@Composable
internal fun DayRhythmCard(
    state: KexuUiState,
    date: LocalDate,
    onClose: () -> Unit,
    onFootprints: () -> Unit,
) {
    val day = rhythmDay(state,date)
    val closed = state.reflections.any { it.dateEpochDay == date.toEpochDay() && it.closedAt != null }
    var previous by remember(date) { mutableStateOf<Int?>(null) }
    var celebrate by remember { mutableStateOf(false) }
    var greeting by remember { mutableStateOf(false) }
    LaunchedEffect(day.completed, state.settings != null) {
        if (state.settings == null) return@LaunchedEffect
        if(previous != null && day.completed > previous!!) { celebrate=true; delay(1600); celebrate=false }
        previous=day.completed
    }
    Surface(shape=RoundedCornerShape(28.dp),color=Color(0xFF302C40),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("M月d日"))+"  ·  今天的节奏",fontSize=11.sp,color=Color(0xFFCDC2D1),letterSpacing=1.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(when { celebrate -> "又向前了一点"; closed -> "今天，就到这里"; greeting -> "我在，慢慢来"; else -> "每一点，都算数" },
                        fontSize=24.sp,color=Color(0xFFFFF6ED),fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(if(closed) "好好休息，明天再继续。" else "完成 ${day.completed} 件  ·  专注 ${day.focusSeconds/60} 分钟",
                        color=Color(0xFFD1C7D7),fontSize=12.sp)
                }
                QuietCompanion(closed,state.settings?.companionMotion != false,
                    Modifier.size(84.dp).clip(CircleShape).clickable { greeting=!greeting },celebrate)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Button(onClick=onClose,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFF1CAB9),contentColor=Color(0xFF40303A)),
                    contentPadding=PaddingValues(horizontal=18.dp,vertical=10.dp)) {
                    Icon(Icons.Outlined.NightsStay,null,Modifier.size(17.dp)); Spacer(Modifier.width(7.dp))
                    Text(if(closed) "查看今日回顾" else "今天收工",fontSize=13.sp)
                }
                TextButton(onClick=onFootprints,colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFF4E6F2))) {
                    Text("一周足迹",fontSize=13.sp); Icon(Icons.Outlined.ChevronRight,null,Modifier.size(17.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayClosingSheet(
    state: KexuUiState,
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Long,String,Boolean) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
) {
    val stored=state.reflections.firstOrNull { it.dateEpochDay==date.toEpochDay() }
    var note by rememberSaveable(date.toEpochDay()) { mutableStateOf(stored?.note.orEmpty()) }
    val closed=stored?.closedAt!=null
    val day=rhythmDay(state,date)
    val remaining=state.tasks.filter { !it.isCompleted && it.dueAt?.rhythmDate()?.let { d -> !d.isAfter(date) }==true }
    var expand by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今天，就到这里",fontSize=27.sp,fontWeight=FontWeight.SemiBold)
                    Text("把进展留下，把今天放下。",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                QuietCompanion(true,false,Modifier.size(70.dp))
            }
            RhythmNumbers(day.completed,day.focusSeconds/60,day.habits)
            OutlinedTextField(note,{note=it.take(280)},Modifier.fillMaxWidth(),
                label={Text("留一句给今天")},placeholder={Text("一点进步、一件小事，或一句晚安…")},
                supportingText={Text("${note.length}/280 · 只保存在本机")},minLines=2,maxLines=4,
                shape=RoundedCornerShape(18.dp))
            if(remaining.isNotEmpty()) {
                Surface(color=MaterialTheme.colorScheme.surfaceContainer,shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { expand=!expand },verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("还有 ${remaining.size} 件待安排",fontWeight=FontWeight.Medium)
                                Text("截止时间与提醒仍然有效",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if(expand) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,"查看未完成任务")
                        }
                        if(expand) remaining.forEach { task ->
                            TextButton(onClick={ onDismiss(); onEditTask(task) },modifier=Modifier.fillMaxWidth()) {
                                Text(task.title,Modifier.weight(1f),maxLines=2)
                                Text("查看",fontSize=12.sp)
                            }
                        }
                    }
                }
            }
            Text("收工后仍可添加和完成任务。",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick={onSave(date.toEpochDay(),note,true);onDismiss()},modifier=Modifier.fillMaxWidth().height(52.dp)) {
                Text(if(closed) "保存回顾" else "收工，好好休息")
            }
            if(closed) TextButton(onClick={onSave(date.toEpochDay(),note,false);onDismiss()},modifier=Modifier.fillMaxWidth()) {Text("我还想再做一点")}
        }
    }
}

@Composable
private fun RhythmNumbers(completed: Int, minutes: Int, habits: Int) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        listOf(completed to "件完成",minutes to "分钟专注",habits to "次习惯").forEach { (count,label) ->
            Surface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(14.dp)) {
                    Text(count.toString(),fontSize=26.sp,fontWeight=FontWeight.SemiBold)
                    Text(label,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FootprintsSheet(state: KexuUiState, today: LocalDate, onDismiss: () -> Unit, onMotion: (Boolean) -> Unit) {
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    val monday=today.minusDays(today.dayOfWeek.value-1L).plusWeeks(weekOffset.toLong())
    val days=(0L..6L).map { rhythmDay(state,monday.plusDays(it)) }
    var selectedEpoch by rememberSaveable(monday.toEpochDay()) { mutableLongStateOf(minOf(today,monday.plusDays(6)).toEpochDay()) }
    val selected=LocalDate.ofEpochDay(selectedEpoch)
    val selectedDay=days.first { it.date==selected }
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("一周足迹",fontSize=28.sp,fontWeight=FontWeight.SemiBold)
                    Text("走过的每一步，都留在这里。",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick={weekOffset--}) {Icon(Icons.Outlined.ChevronLeft,"上一周")}
                IconButton(onClick={weekOffset++},enabled=weekOffset<0) {Icon(Icons.Outlined.ChevronRight,"下一周")}
            }
            Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFF302C40)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("慢慢来，也在向前",color=Color(0xFFFFF1DE),fontSize=21.sp,fontWeight=FontWeight.Medium)
                            Text("${monday.format(DateTimeFormatter.ofPattern("M.d"))} — ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("M.d"))}",color=Color(0xFFCEC1D3),fontSize=12.sp)
                        }
                        QuietCompanion(false,false,Modifier.size(70.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        val max=days.maxOf { it.completed }.coerceAtLeast(1)
                        days.forEachIndexed { index,day ->
                            val chosen=day.date==selected
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if(chosen) Color.White.copy(alpha=.10f) else Color.Transparent)
                                .clickable(enabled=!day.date.isAfter(today)) {selectedEpoch=day.date.toEpochDay()}
                                .padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text(if(day.date.isAfter(today)) "—" else day.completed.toString(),fontSize=11.sp,color=Color(0xFFE9D2CE))
                                Box(Modifier.height(66.dp).padding(vertical=7.dp),contentAlignment=Alignment.BottomCenter) {
                                    Box(Modifier.width(12.dp).height((4+48*day.completed.toFloat()/max).dp).clip(RoundedCornerShape(8.dp))
                                        .background(if(day.completed>0) Color(0xFFF0C3AE) else Color(0xFF5B5165)))
                                }
                                Text(listOf("一","二","三","四","五","六","日")[index],fontSize=11.sp,color=Color(0xFFE5DAE9))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${days.sumOf{it.completed}} 件完成   ·   ${days.sumOf{it.focusSeconds}/60} 分钟专注",fontSize=12.sp,color=Color(0xFFDDD0E1))
                }
            }
            Text("${selected.monthValue}月${selected.dayOfMonth}日 · 这一天",fontWeight=FontWeight.SemiBold)
            RhythmNumbers(selectedDay.completed,selectedDay.focusSeconds/60,selectedDay.habits)
            val reflection=state.reflections.firstOrNull {it.dateEpochDay==selectedEpoch}
            if(!reflection?.note.isNullOrBlank()) Text("“${reflection.note}”",color=MaterialTheme.colorScheme.onSurfaceVariant)
            val titles=state.completions.filter {it.completedAt.rhythmDate()==selected}
            if(titles.isEmpty()) Text("这天还没有完成记录，留白也很好。",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else titles.forEach { Text("✓  ${it.title}",fontSize=14.sp) }
            OutlinedButton(onClick={
                sharing=true
                scope.launch {
                    runCatching { shareFootprints(context,days) }.onFailure {shareError="分享卡生成失败，请重试"}
                    sharing=false
                }
            },enabled=!sharing,modifier=Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Outlined.IosShare,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(if(sharing) "正在生成…" else "分享足迹卡")
            }
            Text(shareError ?: "分享卡只包含统计，不包含任务名和回顾文字。",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text("陪伴动效",fontSize=14.sp);Text("呼吸与轻微亮起，可随时关闭",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Switch(checked=state.settings?.companionMotion!=false,onCheckedChange=onMotion,
                    modifier=Modifier.semantics { contentDescription="陪伴动效" })
            }
        }
    }
}
