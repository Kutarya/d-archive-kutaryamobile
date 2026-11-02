package com.kutarya.mobile

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DailySnapshot(val today:List<SavedItem>,val overdue:Int,val remainingItems:Int,
    val next:SavedItem?,val answer:String)

object DailyOverview {
    fun build(reminders:List<SavedItem>,lists:List<SavedItem>,clock:Clock=Clock.systemDefaultZone()):DailySnapshot {
        val now=clock.millis()
        val day=LocalDate.now(clock)
        val pending=reminders.filter { !it.value.optBoolean("delivered") }.sortedBy { it.value.optLong("at") }
        val today=pending.filter { Instant.ofEpochMilli(it.value.optLong("at")).atZone(clock.zone).toLocalDate()==day }
        val overdue=pending.count { it.value.optLong("at")<now }
        val remaining=lists.sumOf { row ->
            val items=row.value.optJSONArray("items")
            if(items==null) 0 else (0 until items.length()).count { !items.getJSONObject(it).optBoolean("done") }
        }
        val time=DateTimeFormatter.ofPattern("HH:mm",LocalParser.tr)
        val answer=buildString {
            append(day.format(DateTimeFormatter.ofPattern("d MMMM EEEE",LocalParser.tr)))
            append("\n\n")
            if(today.isEmpty()) append("Bugün için kayıtlı hatırlatman yok.")
            else {
                append("Bugünkü hatırlatmalar\n")
                append(today.take(20).joinToString("\n") {
                    Instant.ofEpochMilli(it.value.optLong("at")).atZone(clock.zone).format(time)+" · "+
                        it.value.optString("text")+(if(it.value.optLong("at")<now) " (zamanı geçti)" else "")
                })
                if(today.size>20) append("\nDiğerlerini Planım'dan görebilirsin.")
            }
            if(overdue>0) append("\n\n"+overdue+" hatırlatmanın zamanı geçmiş; teslim edildi olarak işaretlenmemiş.")
            append("\n\nListelerinde "+remaining+" tamamlanmamış madde var.")
            val active=lists.filter { row ->
                val items=row.value.optJSONArray("items")
                items!=null && (0 until items.length()).any { !items.getJSONObject(it).optBoolean("done") }
            }
            if(active.isNotEmpty()) append("\n"+active.take(10).joinToString(", ") { it.value.optString("title") })
        }
        return DailySnapshot(today,overdue,remaining,pending.firstOrNull { it.value.optLong("at")>=now },answer)
    }
}
