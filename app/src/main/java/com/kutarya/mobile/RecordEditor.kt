package com.kutarya.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ReminderDraft {
    fun call(text:String,date:String,time:String,clock:Clock=Clock.systemDefaultZone()):ToolCall {
        val at=LocalDateTime.of(LocalDate.parse(date),LocalTime.parse(time)).atZone(clock.zone).toInstant().toEpochMilli()
        return ToolCall("create_reminder",JSONObject().put("text",text.trim()).put("at",at)).also {
            require(ToolRegistry.validate(it,clock.millis())==null) { ToolRegistry.validate(it,clock.millis()).orEmpty() }
        }
    }
}

@Composable fun RecordEditor(model:MobileModel,kind:String,dismiss:()->Unit) {
    val state by model.state.collectAsState()
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf("") }
    val start=remember { LocalDateTime.now().plusMinutes(15).withSecond(0).withNano(0) }
    var date by remember { mutableStateOf(start.toLocalDate()) }
    var time by remember { mutableStateOf(start.toLocalTime()) }
    var error by remember { mutableStateOf("") }
    val context=LocalContext.current
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    fun close() { focus.clearFocus();keyboard?.hide();dismiss() }
    AlertDialog(onDismissRequest=::close,title={Text(when(kind) {"note"->"Yeni not";"list"->"Yeni liste";else->"Yeni hatırlatma"})},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(kind=="list") {
                OutlinedTextField(title,{title=it.take(100);error=""},label={Text("Liste adı")},singleLine=true,
                    modifier=Modifier.fillMaxWidth().testTag("record-title"))
                OutlinedTextField(entries,{entries=it.take(4000);error=""},label={Text("Maddeler")},minLines=3,maxLines=6,
                    supportingText={Text("Her satıra bir madde yaz.")},modifier=Modifier.fillMaxWidth().testTag("record-text"))
            } else {
                OutlinedTextField(text,{text=it.take(if(kind=="note") 8000 else 500);error=""},
                    label={Text(if(kind=="note") "Notun" else "Ne hatırlatalım?")},minLines=3,maxLines=6,
                    modifier=Modifier.fillMaxWidth().testTag("record-text"))
            }
            if(kind=="reminder") {
                OutlinedButton(onClick={
                    DatePickerDialog(context,{_,year,month,day->date=LocalDate.of(year,month+1,day);error=""},
                        date.year,date.monthValue-1,date.dayOfMonth).apply {
                        datePicker.minDate=System.currentTimeMillis()-60000
                    }.show()
                },modifier=Modifier.fillMaxWidth().testTag("record-date")) {
                    Icon(Icons.Default.CalendarToday,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy",LocalParser.tr)))
                }
                OutlinedButton(onClick={
                    TimePickerDialog(context,{_,hour,minute->time=LocalTime.of(hour,minute);error=""},time.hour,time.minute,true).show()
                },modifier=Modifier.fillMaxWidth().testTag("record-time")) {
                    Icon(Icons.Default.Schedule,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,
                modifier=Modifier.testTag("record-error"))
        }},
        confirmButton={TextButton(onClick={
            try {
                val call=when(kind) {
                    "note"->ToolCall("create_note",JSONObject().put("text",text.trim()))
                    "list"->ToolCall("create_list",JSONObject().put("title",title.trim()).also { if(entries.isNotBlank()) it.put("entries",entries) })
                    else->ReminderDraft.call(text,date.toString(),time.toString())
                }
                val invalid=ToolRegistry.validate(call)
                if(invalid!=null) error=invalid
                else {
                    model.submit(when(kind) {
                        "note"->"Not al: "+text.trim()
                        "list"->title.trim()+" listesi oluştur"
                        else->"Hatırlat: "+text.trim()
                    },call)
                    close()
                }
            } catch(e:IllegalArgumentException) { error=e.message ?: "Kaydı kontrol et." }
        },enabled=state.ready && !state.busy &&
            (if(kind=="list") title.isNotBlank() else text.isNotBlank()),modifier=Modifier.testTag("save-record")){Text("Kaydet")}},
        dismissButton={TextButton(onClick=::close){Text("Vazgeç")}})
}

@Composable fun ListItemEditor(model:MobileModel,row:SavedItem,dismiss:()->Unit) {
    val state by model.state.collectAsState()
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    fun close() { focus.clearFocus();keyboard?.hide();dismiss() }
    var text by remember(row.id) { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=::close,title={Text("Listeye madde ekle")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(row.value.optString("title"),style=MaterialTheme.typography.labelLarge)
        OutlinedTextField(text,{text=it.take(200);error=""},label={Text("Madde")},singleLine=true,modifier=Modifier.testTag("list-item-text"))
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton(onClick={
        val call=ToolCall("add_list_item",JSONObject().put("title",row.value.getString("title")).put("text",text.trim()))
        val invalid=ToolRegistry.validate(call)
        if(invalid!=null) error=invalid else {
            model.submit(row.value.optString("title")+" listesine "+text.trim()+" ekle",call); close()
        }
    },enabled=text.isNotBlank() && !state.busy,modifier=Modifier.testTag("save-list-item")){Text("Ekle")}},
        dismissButton={TextButton(onClick=::close){Text("Vazgeç")}})
}
