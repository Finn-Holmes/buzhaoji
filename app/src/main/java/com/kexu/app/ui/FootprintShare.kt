package com.kexu.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A local image; the user chooses whether and where to send it in the Android share sheet. */
internal suspend fun shareFootprints(context: Context, days: List<RhythmDay>) {
    val file=withContext(Dispatchers.IO) {
        val directory=File(context.cacheDir,"footprints").apply {mkdirs()}
        directory.listFiles()?.filter {System.currentTimeMillis()-it.lastModified()>7*86_400_000L}?.forEach {it.delete()}
        val output=File(directory,"footprints-${System.currentTimeMillis()}.png")
        val bitmap=Bitmap.createBitmap(1080,1400,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        fun label(text: String,x: Float,y: Float,size: Float,color: Int,bold: Boolean=false) {
            paint.color=color;paint.textSize=size;paint.typeface=if(bold) Typeface.create("sans-serif",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)
            canvas.drawText(text,x,y,paint)
        }
        val cream=Color.rgb(255,241,222);val muted=Color.rgb(205,191,214);val peach=Color.rgb(240,195,174)
        canvas.drawColor(Color.rgb(48,44,64))
        label("不着急  /  一周足迹",90f,130f,28f,muted)
        label("慢慢来，也在向前",90f,242f,60f,cream,true)
        val format=DateTimeFormatter.ofPattern("yyyy.MM.dd")
        label("${days.first().date.format(format)} — ${days.last().date.format(format)}",90f,306f,25f,muted)
        paint.color=peach;canvas.drawCircle(878f,412f,63f,paint)
        paint.color=Color.rgb(87,66,79);canvas.drawCircle(860f,410f,4f,paint);canvas.drawCircle(896f,410f,4f,paint)
        label(days.sumOf {it.completed}.toString(),90f,510f,108f,cream,true)
        label("件事，认真完成",90f,558f,27f,muted)
        val maximum=days.maxOf {it.completed}.coerceAtLeast(1)
        days.forEachIndexed { index,day ->
            val x=122f+index*138f
            val height=12f+200f*day.completed/maximum
            paint.color=if(day.completed>0) peach else Color.rgb(82,74,94)
            canvas.drawRoundRect(x,906-height,x+40,906f,20f,20f,paint)
            label(if(day.date.isAfter(java.time.LocalDate.now())) "—" else day.completed.toString(),x+4,946f,26f,cream)
            label(listOf("一","二","三","四","五","六","日")[index],x+4,993f,25f,muted)
        }
        label("${days.sumOf{it.focusSeconds}/60} 分钟专注",90f,1120f,36f,cream,true)
        label("${days.sumOf{it.habits}} 次习惯打卡",550f,1120f,36f,cream,true)
        label("每一点，都算数。",90f,1290f,28f,muted)
        output.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
        bitmap.recycle()
        output
    }
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.footprints",file)
    val send=Intent(Intent.ACTION_SEND).apply {
        type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
        clipData=ClipData.newRawUri("一周足迹",uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send,"分享一周足迹"))
}
