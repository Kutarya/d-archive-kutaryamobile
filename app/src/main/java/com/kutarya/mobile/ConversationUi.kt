package com.kutarya.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

@Composable fun ConversationScreen(model:MobileModel,state:MobileState,listening:Boolean,voiceStatus:String,
    voiceLevel:Float,voice:()->Unit,stop:()->Unit,attach:()->Unit) {
    var form by remember { mutableStateOf<String?>(null) }
    val color=MaterialTheme.colorScheme
    val list=rememberLazyListState()
    val day=remember(state.reminders,state.checklists) { DailyOverview.build(state.reminders,state.checklists) }
    val last=state.messages.lastOrNull()
    LaunchedEffect(state.messages.size,state.revision,state.conversationId) {
        if(state.messages.isEmpty()) list.scrollToItem(0)
        else {
            // Kullanıcı eski mesajları okuyorsa gelen ara adımlar ekranı çekmez.
            val target=list.layoutInfo.totalItemsCount-1
            if(target>=0) {
                if(state.settings.reduceMotion) list.scrollToItem(target) else list.animateScrollToItem(target)
            }
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("assistant-list"),state=list,
            contentPadding=PaddingValues(start=18.dp,end=18.dp,top=6.dp,bottom=18.dp),
            verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(state.messages.isEmpty()) {
                item {
                    Column(Modifier.padding(top=4.dp,bottom=2.dp)) {
                        Text("Ne yapalım?",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                        Text("Notlar, planlar ve telefon işleri.",Modifier.padding(top=6.dp),
                            style=MaterialTheme.typography.bodyMedium,color=color.onSurfaceVariant)
                    }
                }
                item {
                    Card(onClick={model.submit("Günümü özetle")},enabled=state.ready && !state.busy,
                        modifier=Modifier.fillMaxWidth().testTag("daily-summary"),shape=RoundedCornerShape(24.dp),
                        colors=CardDefaults.cardColors(containerColor=color.secondaryContainer)) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Today,null,tint=color.primary,modifier=Modifier.size(20.dp))
                                Text("Günlük plan",Modifier.weight(1f).padding(start=10.dp),fontWeight=FontWeight.Bold)
                                Icon(Icons.Default.ChevronRight,null,tint=color.primary)
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                                DailyCount(day.today.size,"hatırlatma",Modifier.weight(1f))
                                DailyCount(day.remainingItems,"açık madde",Modifier.weight(1f))
                                DailyCount(state.notes.size,"not",Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Text("Hızlı işler",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        QuickWork("Not al",Icons.Default.EditNote,{form="note"},Modifier.weight(1f))
                        QuickWork("Hatırlat",Icons.Default.NotificationsNone,{form="reminder"},Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        QuickWork("Liste oluştur",Icons.Default.Checklist,{form="list"},Modifier.weight(1f))
                        QuickWork("Hesapla",Icons.Default.Calculate,{model.input("Hesapla ")},Modifier.weight(1f))
                    }
                }
                if(state.routines.isNotEmpty()) item {
                    Text("Rutinlerin",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        state.routines.take(4).forEach { row ->
                            AssistChip(onClick={model.runRoutine(row.id)},enabled=!state.busy,
                                label={Text(row.value.optString("title"))},leadingIcon={Icon(Icons.Default.PlayArrow,null,Modifier.size(18.dp))})
                        }
                    }
                }
                day.next?.let { next -> item {
                    Card(onClick={model.submit("Hatırlatmalarımı göster")},modifier=Modifier.fillMaxWidth(),
                        colors=CardDefaults.cardColors(containerColor=color.surface)) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("Sıradaki hatırlatma",style=MaterialTheme.typography.labelLarge,color=color.primary)
                            Text(SimpleDateFormat("d MMM · HH:mm",LocalParser.tr).format(Date(next.value.optLong("at")))+" · "+
                                next.value.optString("text"),maxLines=2,overflow=TextOverflow.Ellipsis)
                        }
                    }
                } }
            }
            items(state.messages,key={it.id}) { run ->
                Column(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.testTag("message-"+run.id)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                        Surface(color=color.secondaryContainer,shape=RoundedCornerShape(22.dp,22.dp,6.dp,22.dp),
                            modifier=Modifier.widthIn(max=340.dp).padding(start=26.dp)) {
                            SelectionContainer { Text(run.request,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyLarge) }
                        }
                    }
                    if(run.answer.isNotBlank()) ConversationAnswer(model,run,run.id==last?.id,state.busy)
                    if(run.id==last?.id && state.busy) ActiveTask(state)
                }
            }
            if(state.suggestions.isNotEmpty()) item {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("Bunlarla başlayabilirsin",style=MaterialTheme.typography.labelLarge,color=color.onSurfaceVariant)
                    state.suggestions.forEach { example ->
                        SuggestionChip(onClick={model.input(example.command)},label={Text(example.title)},
                            modifier=Modifier.testTag("suggestion-"+example.title),
                            icon={Icon(Icons.Default.NorthWest,null,Modifier.size(16.dp))})
                    }
                }
            }
        }
        if(!state.ready) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("loading-records"))
        if(voiceStatus.isNotBlank()) {
            Surface(color=color.surfaceVariant,modifier=Modifier.fillMaxWidth().testTag("voice-status")) {
                Row(Modifier.padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(listening) VoiceMeter(voiceLevel,Modifier.size(36.dp,22.dp))
                    else Icon(Icons.Default.MicNone,null,Modifier.size(18.dp),tint=color.primary)
                    Text(voiceStatus,Modifier.weight(1f).padding(start=10.dp),style=MaterialTheme.typography.bodySmall)
                    if(listening) IconButton(onClick=stop){Icon(Icons.Default.Close,"Dinlemeyi iptal et")}
                }
            }
        }
        state.attachment?.let { file ->
            Surface(color=color.secondaryContainer,modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachFile,null,Modifier.size(18.dp))
                    Text(file.name,Modifier.weight(1f).padding(8.dp),maxLines=1,overflow=TextOverflow.Ellipsis,
                        style=MaterialTheme.typography.bodySmall)
                    IconButton(onClick={model.attachment(null)}){Icon(Icons.Default.Close,"Dosyayı çıkar")}
                }
            }
        }
        Surface(color=color.surface,tonalElevation=1.dp) {
            Column(Modifier.padding(horizontal=12.dp,vertical=10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick=attach,enabled=state.ready && !state.busy && !listening,
                        modifier=Modifier.testTag("attach-file")){Icon(Icons.Default.Add,"Dosya ekle")}
                    OutlinedTextField(state.input,model::input,placeholder={Text(if(listening) "Konuşman burada…" else "Kutarya Us'a yaz")},
                        modifier=Modifier.weight(1f).testTag("command-input"),shape=RoundedCornerShape(22.dp),
                        enabled=state.ready && !state.busy && !listening,minLines=1,maxLines=4,
                        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),
                        keyboardActions=KeyboardActions(onSend={if(!state.busy && !listening) model.submit()}))
                    Spacer(Modifier.width(8.dp))
                    val send=state.input.isNotBlank() && !listening
                    FilledIconButton(onClick={when { state.busy->stop();listening->voice();send->model.submit();else->voice() }},
                        enabled=state.ready,modifier=Modifier.size(48.dp).testTag(
                            when { state.busy->"stop";listening->"finish-voice";send->"send";else->"microphone" })) {
                        Icon(when { state.busy->Icons.Default.Stop;listening->Icons.Default.Done;send->Icons.AutoMirrored.Filled.Send;else->Icons.Default.Mic },
                            when { state.busy->"Durdur";listening->"Konuşmayı bitir";send->"Gönder";else->"Konuş" })
                    }
                }
            }
        }
    }
    form?.let { RecordEditor(model,it){form=null} }
}

@Composable private fun DailyCount(count:Int,label:String,modifier:Modifier) {
    Column(modifier) {
        Text(count.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun QuickWork(title:String,icon:ImageVector,click:()->Unit,modifier:Modifier) {
    OutlinedCard(onClick=click,modifier=modifier,shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp).heightIn(min=34.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer,RoundedCornerShape(10.dp)),
                contentAlignment=Alignment.Center) {
                Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
        }
    }
}
@Composable private fun ConversationAnswer(model:MobileModel,run:TaskRun,latest:Boolean,busy:Boolean) {
    var details by remember(run.id) { mutableStateOf(false) }
    val context=LocalContext.current
    val color=MaterialTheme.colorScheme
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Kutarya Us",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge,modifier=Modifier.weight(1f))
            Text(run.status,style=MaterialTheme.typography.labelSmall,color=when(run.status) {
                "Tamamlanamadı","Desteklenmeyen istek" -> color.error
                else -> color.onSurfaceVariant
            })
        }
        Surface(color=color.surface,shape=RoundedCornerShape(6.dp,22.dp,22.dp,22.dp),
            modifier=Modifier.fillMaxWidth().then(if(latest) Modifier.testTag("assistant-answer") else Modifier)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                SelectionContainer { Text(run.answer,style=MaterialTheme.typography.bodyLarge) }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    if(run.steps.isNotEmpty()) TextButton(onClick={details=!details},contentPadding=PaddingValues(horizontal=6.dp)) {
                        Icon(if(details) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,Modifier.size(18.dp))
                        Text(run.steps.size.toString()+" adım",style=MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick={
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Kutarya Us",run.answer))
                    }){Icon(Icons.Default.ContentCopy,"Yanıtı kopyala",Modifier.size(18.dp))}
                    IconButton(onClick={model.submit("Yanıtı not al",ToolCall("create_note",JSONObject().put("text",run.answer.take(7500))))},
                        enabled=!busy){Icon(Icons.Default.BookmarkAdd,"Yanıtı not al",Modifier.size(20.dp))}
                    IconButton(onClick={model.input(run.request)},enabled=!busy) {
                        Icon(Icons.Default.Replay,"İsteği yeniden kullan",Modifier.size(20.dp))
                    }
                }
                if(details) run.steps.forEach { step -> TaskStep(step.name,step.state,step.message) }
            }
        }
    }
}
@Composable private fun ActiveTask(state:MobileState) {
    val color=MaterialTheme.colorScheme
    Surface(color=color.surface,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth().testTag("active-task")) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)
                Text(state.progress,Modifier.padding(start=12.dp),style=MaterialTheme.typography.labelLarge)
            }
            state.planned.forEachIndexed { i,title ->
                val result=state.currentSteps.getOrNull(i)
                TaskStep((i+1).toString()+". "+title,result?.state,
                    if(result==null) { if(i==state.currentSteps.size) "Çalışıyor" else "Sırada" } else result.message)
            }
            if(state.planned.isEmpty()) state.currentSteps.forEach { TaskStep(it.name,it.state,it.message) }
        }
    }
}
@Composable private fun TaskStep(title:String,state:ResultState?,message:String) {
    Row(verticalAlignment=Alignment.Top) {
        Icon(when(state) { ResultState.VERIFIED->Icons.Default.CheckCircle;ResultState.HANDOFF->Icons.AutoMirrored.Filled.OpenInNew
            ResultState.FAILED,ResultState.BLOCKED->Icons.Default.ErrorOutline;ResultState.CANCELLED->Icons.Default.Cancel
            else->Icons.Default.RadioButtonUnchecked },null,Modifier.size(18.dp),
            tint=if(state in setOf(ResultState.FAILED,ResultState.BLOCKED)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start=10.dp)) {
            Text(title,style=MaterialTheme.typography.labelLarge)
            Text(message.take(240),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun VoiceMeter(level:Float,modifier:Modifier) {
    val color=MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        for(i in 0..4) {
            val height=size.height*(.15f+level*(if(i%2==0) .7f else .95f))
            drawRoundRect(color,Offset(i*size.width/5,(size.height-height)/2),
                Size(size.width/9,height),CornerRadius(4f,4f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ConversationSheet(model:MobileModel,state:MobileState,dismiss:()->Unit) {
    var search by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<ConversationSummary?>(null) }
    val date=remember { SimpleDateFormat("d MMM · HH:mm",LocalParser.tr) }
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    fun choose(id:String) { focus.clearFocus();keyboard?.hide();model.openConversation(id);dismiss() }
    ModalBottomSheet(onDismissRequest=dismiss) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal=20.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Konuşmalar",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                TextButton(onClick={model.newConversation();dismiss()},enabled=!state.busy){Text("Yeni")}
            }
            OutlinedTextField(search,{search=it},placeholder={Text("Konuşma ara")},singleLine=true,
                keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
                keyboardActions=KeyboardActions(onSearch={focus.clearFocus();keyboard?.hide()}),
                modifier=Modifier.fillMaxWidth().testTag("conversation-search"),leadingIcon={Icon(Icons.Default.Search,null)})
            LazyColumn(Modifier.weight(1f,fill=false).heightIn(max=460.dp).padding(top=10.dp),contentPadding=PaddingValues(bottom=30.dp)) {
                val rows=state.conversations.filter { LocalParser.normalize(it.title).contains(LocalParser.normalize(search)) }
                if(rows.isEmpty()) item { Text("Henüz konuşma yok.",Modifier.padding(vertical=24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant) }
                items(rows,key={it.id}) { row ->
                    ListItem(modifier=Modifier.clickable(enabled=!state.busy){choose(row.id)},
                        headlineContent={Text(row.title,maxLines=2,overflow=TextOverflow.Ellipsis)},
                        supportingContent={Text(date.format(Date(row.updated))+" · "+row.count+" istek")},
                        leadingContent={Icon(if(row.id==state.conversationId) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                            null,tint=MaterialTheme.colorScheme.primary)},
                        trailingContent={Row {
                            IconButton(onClick={choose(row.id)},enabled=!state.busy,
                                modifier=Modifier.testTag("open-conversation-"+row.id)){Icon(Icons.Default.ChevronRight,"Konuşmayı aç")}
                            IconButton(onClick={deleting=row},enabled=!state.busy){Icon(Icons.Default.DeleteOutline,"Konuşmayı sil")}
                        }})
                }
            }
        }
    }
    deleting?.let { row ->
        AlertDialog(onDismissRequest={deleting=null},title={Text("Konuşmayı sil")},text={Text(row.title+"\n\n"+row.count+" istek ve yanıt silinecek. Notların, listelerin ve hatırlatmaların korunur.")},
            confirmButton={TextButton(onClick={model.deleteConversation(row.id);deleting=null}){Text("Sil")}},
            dismissButton={TextButton(onClick={deleting=null}){Text("Vazgeç")}})
    }
}
