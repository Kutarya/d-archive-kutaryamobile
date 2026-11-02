package com.kutarya.mobile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

data class CommandExample(val title:String,val command:String,val group:String)
object CommandCatalog {
    val examples=listOf(
        CommandExample("Not kaydet","Not al: ","Notlar"),
        CommandExample("Notlarında ara","Notlarımda toplantı ara","Notlar"),
        CommandExample("Notlarını göster","Notlarımı göster","Notlar"),
        CommandExample("Son yanıtı kaydet","Bunu not al","Notlar"),
        CommandExample("Hatırlatma ekle","20 dakika sonra su içmeyi hatırlat","Planlama"),
        CommandExample("Planını gör","Hatırlatmalarımı göster","Planlama"),
        CommandExample("Günlük özet","Günümü özetle","Planlama"),
        CommandExample("Alarm","Sabah 07:30'a alarm kur","Planlama"),
        CommandExample("Sayaç","Yarım saat sayaç başlat","Planlama"),
        CommandExample("Liste oluştur","Alışveriş listesi oluştur: süt, ekmek","Listeler"),
        CommandExample("Listeye ekle","Alışveriş listesine yumurta ekle","Listeler"),
        CommandExample("Listeni göster","Alışveriş listesini göster","Listeler"),
        CommandExample("Maddeyi tamamla","Alışveriş listesinde süt tamamla","Listeler"),
        CommandExample("Maddeyi kaldır","Alışveriş listesinden süt kaldır","Listeler"),
        CommandExample("Hesapla","Yirmi beş çarpı dört","Hesap"),
        CommandExample("Birim çevir","5 km kaç metre?","Hesap"),
        CommandExample("İndirim","1200 lira yüzde 20 indirim","Hesap"),
        CommandExample("Tarih hesabı","20 gün sonra hangi gün?","Hesap"),
        CommandExample("Metin özeti","Metni özetle: ","Metin"),
        CommandExample("Kelime sayısı","Kelimeleri say: ","Metin"),
        CommandExample("Fener","Feneri aç","Telefon"),
        CommandExample("Medya sesi","Medya sesini yüzde 40 ayarla","Telefon"),
        CommandExample("Wi-Fi","WiFi ayarlarını aç","Telefon"),
        CommandExample("Uygulama aç","WhatsApp'ı aç","Telefon"),
        CommandExample("Birini ara","Annemi ara","Telefon"),
        CommandExample("Mesaj hazırla","Ahmet'e on dakika gecikeceğimi yaz","Telefon"),
        CommandExample("Haritada bul","En yakın eczaneyi bul","Telefon")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CommandSheet(dismiss:()->Unit,choose:(String)->Unit) {
    var search by remember { mutableStateOf("") }
    val rows=CommandCatalog.examples.filter { LocalParser.normalize(it.title+" "+it.command+" "+it.group).contains(LocalParser.normalize(search)) }
    ModalBottomSheet(onDismissRequest=dismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
            Text("Komutlar",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(search,{search=it},placeholder={Text("Bir iş ara")},leadingIcon={Icon(Icons.Default.Search,null)},
                singleLine=true,modifier=Modifier.fillMaxWidth().testTag("command-search"))
            LazyColumn(Modifier.heightIn(max=470.dp).padding(top=12.dp),contentPadding=PaddingValues(bottom=30.dp)) {
                rows.groupBy { it.group }.forEach { (group,commands) ->
                    item { Text(group,Modifier.padding(top=18.dp,bottom=8.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary) }
                    items(commands) { example ->
                        ListItem(headlineContent={Text(example.title,fontWeight=FontWeight.SemiBold)},
                            supportingContent={Text(example.command,style=MaterialTheme.typography.bodySmall)},
                            trailingContent={TextButton(onClick={choose(example.command);dismiss()}){Text("Seç")}})
                    }
                }
                if(rows.isEmpty()) item { Text("Eşleşen komut yok.",Modifier.padding(20.dp)) }
            }
        }
    }
}

@Composable fun RecordsScreen(model:MobileModel,state:MobileState,use:(String)->Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var noteDelete by remember { mutableStateOf<SavedItem?>(null) }
    var noteEdit by remember { mutableStateOf<SavedItem?>(null) }
    var clear by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<String?>(null) }
    var listAdding by remember { mutableStateOf<SavedItem?>(null) }
    fun matches(text:String)=LocalParser.normalize(text).contains(LocalParser.normalize(search))
    val date=remember { SimpleDateFormat("d MMM · HH:mm",LocalParser.tr) }
    val listState=rememberLazyListState()
    val first=when(section) { 0->state.history.firstOrNull()?.id;1->state.notes.firstOrNull()?.id
        2->state.reminders.firstOrNull()?.id;else->state.checklists.firstOrNull()?.id }
    LaunchedEffect(section,first) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Geçmiş","Notlar","Planım","Listeler").forEachIndexed { index,label ->
                FilterChip(selected=section==index,onClick={section=index;search=""},label={Text(label)},modifier=Modifier.testTag(
                    when(index) { 0->"history-filter";1->"notes-filter";2->"reminders-filter";else->"lists-filter" }))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(search,{search=it},placeholder={Text("Kayıtlarında ara")},singleLine=true,
                leadingIcon={Icon(Icons.Default.Search,null)},modifier=Modifier.weight(1f).testTag("records-search"))
            if(section==0 && state.history.isNotEmpty()) IconButton(onClick={clear=true},enabled=!state.busy){Icon(Icons.Default.DeleteOutline,"Geçmişi temizle")}
            if(section>0) IconButton(onClick={adding=when(section){1->"note";2->"reminder";else->"list"}},
                enabled=state.ready && !state.busy,modifier=Modifier.testTag("new-record")){Icon(Icons.Default.Add,"Kayıt ekle")}
        }
        LazyColumn(Modifier.fillMaxSize().testTag("records-list"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(section) {
                0 -> {
                    val history=state.history.filter { matches(it.request+" "+it.answer) }
                    if(history.isEmpty()) item { RecordEmpty("Henüz bir kayıt yok") }
                    items(history,key={it.id}) { task ->
                        var open by remember(task.id) { mutableStateOf(false) }
                        Card(onClick={open=!open},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                Text(date.format(Date(task.created)),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(task.request,fontWeight=FontWeight.SemiBold)
                                if(open) {
                                    SelectionContainer { Text(task.answer) }
                                    TextButton(onClick={use(task.request)}){Text("Tekrar kullan")}
                                } else Text(task.answer,maxLines=2,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                1 -> {
                    val notes=state.notes.filter { matches(it.value.optString("text")) }
                    if(notes.isEmpty()) item { RecordEmpty("Notların burada") }
                    items(notes,key={it.id}) { note ->
                        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp)) {
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                    Text("#"+note.id+" · "+date.format(Date(note.created)),style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(onClick={noteEdit=note},enabled=!state.busy){Icon(Icons.Default.Edit,"Notu düzenle")}
                                    IconButton(onClick={noteDelete=note},enabled=!state.busy){Icon(Icons.Default.DeleteOutline,"Notu sil")}
                                }
                                SelectionContainer { Text(note.value.optString("text")) }
                            }
                        }
                    }
                }
                2 -> {
                    val reminders=state.reminders.filter { matches(it.value.optString("text")) }
                    if(reminders.isEmpty()) item { RecordEmpty("Bekleyen hatırlatma yok") }
                    items(reminders,key={it.id}) { row ->
                        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsNone,null,tint=MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                    Text(date.format(Date(row.value.optLong("at"))),fontWeight=FontWeight.SemiBold)
                                    Text(row.value.optString("text"),color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick={model.submit("Hatırlatmayı iptal et",ToolCall("cancel_reminder",JSONObject().put("id",row.id)))},enabled=!state.busy){Icon(Icons.Default.Close,"Hatırlatmayı iptal et")}
                            }
                        }
                    }
                }
                3 -> {
                    val lists=state.checklists.filter { matches(it.value.optString("title")+" "+it.value.optJSONArray("items")) }
                    if(lists.isEmpty()) item { RecordEmpty("Listelerin burada") }
                    items(lists,key={it.id}) { row ->
                        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp)) {
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                    Text(row.value.optString("title"),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                    IconButton(onClick={listAdding=row},enabled=!state.busy,modifier=Modifier.testTag("add-list-item-"+row.id)){Icon(Icons.Default.Add,"Listeye madde ekle")}
                                    IconButton(onClick={model.submit("Liste #"+row.id+" sil",ToolCall("delete_list",JSONObject().put("id",row.id)))},
                                        enabled=!state.busy,modifier=Modifier.testTag("delete-list-"+row.id)){Icon(Icons.Default.DeleteOutline,"Listeyi sil")}
                                }
                                val items=row.value.getJSONArray("items")
                                val done=(0 until items.length()).count { items.getJSONObject(it).optBoolean("done") }
                                Text(done.toString()+" / "+items.length()+" tamamlandı",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
                                for(index in 0 until items.length()) {
                                    val item=items.getJSONObject(index); val text=item.optString("text")
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        Checkbox(checked=item.optBoolean("done"),enabled=!state.busy,onCheckedChange={checked ->
                                            model.submit(row.value.optString("title")+" · "+text,ToolCall("check_list_item",JSONObject().put("title",row.value.getString("title")).put("text",text).put("done",checked)))
                                        },modifier=Modifier.testTag("checklist-item-"+row.id+"-"+index))
                                        Text(text,Modifier.weight(1f),color=if(item.optBoolean("done")) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        IconButton(onClick={model.submit(row.value.optString("title")+" listesinden "+text+" kaldır",
                                            ToolCall("remove_list_item",JSONObject().put("title",row.value.getString("title")).put("text",text)))},
                                            enabled=!state.busy,modifier=Modifier.testTag("remove-list-item-"+row.id+"-"+index)) {
                                            Icon(Icons.Default.Close,"Maddeyi kaldır",Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    adding?.let { kind -> RecordEditor(model,kind){adding=null} }
    listAdding?.let { row -> ListItemEditor(model,row){listAdding=null} }
    noteDelete?.let { note -> AlertDialog(onDismissRequest={noteDelete=null},title={Text("Notu sil")},text={Text(note.value.optString("text").take(600))},
        confirmButton={TextButton(onClick={model.delete("note",note.id);noteDelete=null}){Text("Sil")}},dismissButton={TextButton(onClick={noteDelete=null}){Text("Vazgeç")}}) }
    noteEdit?.let { note ->
        var text by remember(note.id) { mutableStateOf(note.value.optString("text")) }
        AlertDialog(onDismissRequest={noteEdit=null},title={Text("Notu düzenle")},text={
            OutlinedTextField(text,{text=it.take(8000)},modifier=Modifier.testTag("edit-note-text"),minLines=3,maxLines=8)
        },confirmButton={TextButton(onClick={model.submit("Notu güncelle",ToolCall("edit_note",JSONObject().put("id",note.id).put("text",text)));noteEdit=null},enabled=text.isNotBlank()){Text("Kaydet")}},
            dismissButton={TextButton(onClick={noteEdit=null}){Text("Vazgeç")}})
    }
    if(clear) AlertDialog(onDismissRequest={clear=false},title={Text("Geçmişi temizle")},text={Text("Görev geçmişin silinecek.")},
        confirmButton={TextButton(onClick={model.clearHistory();clear=false}){Text("Temizle")}},dismissButton={TextButton(onClick={clear=false}){Text("Vazgeç")}})
}
@Composable private fun RecordEmpty(text:String) {
    Column(Modifier.fillMaxWidth().padding(vertical=60.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(Icons.Default.Inbox,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp)); Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
