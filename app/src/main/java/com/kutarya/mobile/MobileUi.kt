package com.kutarya.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun KutaryaUi(model:MobileModel,state:MobileState,listening:Boolean,voiceStatus:String,voice:()->Unit,stop:()->Unit,
    attach:()->Unit,export:()->Unit,assistant:()->Unit,notificationSettings:()->Unit,ticket:RequestTicket?,reply:(UiReply)->Unit,
    voiceLevel:Float=0f) {
    val dark=when(state.settings.theme) { "dark"->true; "light"->false; else->isSystemInDarkTheme() }
    val colors=if(dark) darkColorScheme(primary=Color(0xff79e2c3),onPrimary=Color(0xff09362d),background=Color(0xff10181d),surface=Color(0xff1a252b),surfaceVariant=Color(0xff24343b),onSurface=Color(0xffedf5f2),onSurfaceVariant=Color(0xffb2c1bd),secondaryContainer=Color(0xff28483e),onSecondaryContainer=Color(0xffb3f5db))
        else lightColorScheme(primary=Color(0xff126c56),onPrimary=Color.White,background=Color(0xfff5f5ef),surface=Color(0xfffffff9),surfaceVariant=Color(0xffe8eee5),onSurface=Color(0xff1a2b25),onSurfaceVariant=Color(0xff62736a),secondaryContainer=Color(0xffdbeee2),onSecondaryContainer=Color(0xff204d3c))
    MaterialTheme(colorScheme=colors) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var conversations by remember { mutableStateOf(false) }
        var commands by remember { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize(),color=colors.background) {
            Scaffold(containerColor=colors.background,bottomBar={
                NavigationBar(containerColor=colors.surface) {
                    val labels=listOf("Asistan","Kayıtlar","Rutinler","Ayarlar")
                    val icons=listOf(Icons.Default.GraphicEq,Icons.Default.Bookmarks,Icons.Default.PlayCircleOutline,Icons.Default.Tune)
                    labels.forEachIndexed { index,label -> NavigationBarItem(selected=tab==index,onClick={tab=index},
                        colors=NavigationBarItemDefaults.colors(selectedIconColor=colors.primary,selectedTextColor=colors.primary,indicatorColor=colors.secondaryContainer,
                            unselectedIconColor=colors.onSurfaceVariant,unselectedTextColor=colors.onSurfaceVariant),
                        icon={Icon(icons[index],label)},label={Text(label)},modifier=Modifier.testTag("tab-$index")) }
                }
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(colors.primary,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
                            Text("K",color=colors.onPrimary,fontWeight=FontWeight.Black,style=MaterialTheme.typography.headlineSmall)
                        }
                        Column(Modifier.padding(start=12.dp).weight(1f)) {
                            Text("Kutarya Mobile",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                            Text(listOf("Asistan","Kayıtların","Rutinlerin","Ayarların")[tab],style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                        }
                        if(tab==0) {
                            IconButton(onClick={commands=true},enabled=state.ready && !state.busy,
                                modifier=Modifier.testTag("commands-button")){Icon(Icons.Default.GridView,"Komutlar")}
                            IconButton(onClick={conversations=true},enabled=state.ready && !state.busy,
                                modifier=Modifier.testTag("conversations-button")){Icon(Icons.Default.Forum,"Konuşmalar")}
                            IconButton(onClick=model::newConversation,enabled=state.ready && !state.busy,
                                modifier=Modifier.testTag("new-conversation")){Icon(Icons.Default.Add,"Yeni konuşma")}
                        }
                    }
                    when(tab) {
                        0 -> ConversationScreen(model,state,listening,voiceStatus,voiceLevel,voice,stop,attach)
                        1 -> RecordsScreen(model,state) { value -> model.input(value);tab=0 }
                        2 -> RoutinesScreen(model,state) { routine -> tab=0; model.runRoutine(routine.id) }
                        3 -> SettingsScreen(model,state,export,assistant,notificationSettings)
                    }
                }
            }
        }
        if(conversations) ConversationSheet(model,state){conversations=false}
        if(commands) CommandSheet({commands=false},model::input)
        if(state.error.isNotBlank()) AlertDialog(onDismissRequest=model::clearError,title={Text("Kutarya Mobile")},
            text={Text(state.error)},confirmButton={TextButton(onClick=model::clearError){Text("Tamam")}})
        if(ticket!=null && ticket.reply.isActive) when(val request=ticket.request) {
            is UiRequest.Launch -> request.approval?.let { approval -> AlertDialog(onDismissRequest={reply(UiReply(false,"cancel"))},
                title={Text(approval.title)},text={Text(approval.detail,modifier=Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={reply(UiReply(true,"approve"))},modifier=Modifier.testTag("approve-action")){Text(approval.accept)}},
                dismissButton={TextButton(onClick={reply(UiReply(false,"cancel"))}){Text("Vazgeç")}}) }
            is UiRequest.Select -> AlertDialog(onDismissRequest={reply(UiReply(false,"cancel"))},title={Text(request.title)},
                text={LazyColumn { items(request.choices) { choice -> TextButton(onClick={reply(UiReply(true,choice.value))},modifier=Modifier.fillMaxWidth()){Text(choice.label)} } }},
                confirmButton={TextButton(onClick={reply(UiReply(false,"cancel"))}){Text("Vazgeç")}})
        }
    }
}

@Composable private fun EmptyPanel(icon:ImageVector,title:String,description:String) {
    Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(40.dp)); Spacer(Modifier.height(16.dp))
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp))
        Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun RoutinesScreen(model:MobileModel,state:MobileState,run:(SavedItem)->Unit) {
    var editor by remember { mutableStateOf<SavedItem?>(null) }; var adding by remember { mutableStateOf(false) }; var deleting by remember { mutableStateOf<SavedItem?>(null) }
    var template by remember { mutableStateOf<Pair<String,String>?>(null) }
    val listState=rememberLazyListState()
    LaunchedEffect(state.routines.firstOrNull()?.id) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Kendi rutinlerin",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
            FilledTonalButton(onClick={template=null;adding=true},modifier=Modifier.testTag("new-routine")){Icon(Icons.Default.Add,null);Text("Yeni")}
        }
        LazyColumn(Modifier.testTag("routines-list"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.routines.isEmpty()) item {
                EmptyPanel(Icons.Default.PlayCircleOutline,"İlk rutinini ekle","Komutları kaydet, adıyla çalıştır.")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={template="Güne başla" to "Saat kaç?\nHatırlatmalarımı göster\nNotlarımı göster";adding=true}){Text("Güne başla")}
                    OutlinedButton(onClick={template="Odaklan" to "Medya sesini yüzde 20 ayarla\nYarım saat sayaç başlat";adding=true}){Text("Odaklan")}
                }
            }
            items(state.routines,key={it.id}) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(item.value.optString("title"),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                val plan=RoutineValidation.plan(item.value.optString("commands"))
                Text(if(plan is LocalPlan.Actions) plan.calls.mapIndexed { i,call ->
                    (i+1).toString()+". "+(ToolRegistry.tools[call.name]?.title ?: call.name)
                }.joinToString("\n") else "Komutları düzenle: "+(plan as LocalPlan.Clarify).message,
                    modifier=Modifier.padding(vertical=10.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row { Button(onClick={run(item)},enabled=!state.busy && plan is LocalPlan.Actions,
                    modifier=Modifier.testTag("run-routine-"+item.id)){Icon(Icons.Default.PlayArrow,null);Text("Çalıştır")}
                    Spacer(Modifier.weight(1f)); IconButton(onClick={editor=item}){Icon(Icons.Default.Edit,"Düzenle")}; IconButton(onClick={deleting=item}){Icon(Icons.Default.DeleteOutline,"Rutini sil")}
                }
            } } }
        }
    }
    if(adding || editor!=null) {
        var title by remember(editor,adding) { mutableStateOf(editor?.value?.optString("title") ?: template?.first.orEmpty()) }
        var commands by remember(editor,adding) { mutableStateOf(editor?.value?.optString("commands") ?: template?.second.orEmpty()) }
        val plan=remember(commands) { RoutineValidation.plan(commands) }
        AlertDialog(onDismissRequest={adding=false;editor=null},title={Text(if(editor==null) "Rutin oluştur" else "Rutini düzenle")},
            text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title,{title=it.take(100)},label={Text("Rutin adı")},singleLine=true,modifier=Modifier.testTag("routine-title"))
            OutlinedTextField(commands,{commands=it.take(8000)},label={Text("Komutlar")},minLines=3,maxLines=6,modifier=Modifier.testTag("routine-commands"))
            Text("Her satıra bir komut yaz.",style=MaterialTheme.typography.bodySmall)
            if(commands.isNotBlank()) Text(if(plan is LocalPlan.Actions) "İşlem planı\n"+plan.calls.mapIndexed { i,call ->
                (i+1).toString()+". "+(ToolRegistry.tools[call.name]?.title ?: call.name)
            }.joinToString("\n") else (plan as LocalPlan.Clarify).message,
                color=if(plan is LocalPlan.Actions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("routine-plan"))
        }},confirmButton={TextButton(onClick={
            if(model.saveRoutine(title,commands,editor?.id)) {adding=false;editor=null}
        },enabled=title.isNotBlank() && commands.isNotBlank() && plan is LocalPlan.Actions,
            modifier=Modifier.testTag("save-routine")){Text("Kaydet")}},
            dismissButton={TextButton(onClick={adding=false;editor=null}){Text("Vazgeç")}})
    }
    if(deleting!=null) Confirm("Rutini sil","Bu rutin cihazdan silinecek.",{model.delete("routine",deleting!!.id);deleting=null},{deleting=null})
}

@Composable private fun SettingsScreen(model:MobileModel,state:MobileState,export:()->Unit,assistant:()->Unit,notifications:()->Unit) {
    var ai by remember { mutableStateOf(false) }; var alias by remember { mutableStateOf(false) }; var backup by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val localVoice=remember(context) { SpeechEngine.onDevice(context) }
    LazyColumn(modifier=Modifier.testTag("settings-list"),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { SettingCard("Ses ve asistan") {
            SettingSwitch("Yanıtları seslendir",state.settings.speak){model.settings(state.settings.copy(speak=it))}
            SettingSwitch("Çevrimiçi ses kullanımı",state.settings.onlineSpeech){model.settings(state.settings.copy(onlineSpeech=it))}
            SettingSwitch("Ses komutunu hemen gönder",state.settings.voiceSubmit){model.settings(state.settings.copy(voiceSubmit=it))}
            Text(if(localVoice) "Cihazda ses tanıma hizmeti var. Türkçe dil desteği hizmete bağlıdır."
                else "Cihazda yerel ses tanıma hizmeti yok. Yazıyla kullanabilir veya çevrimiçi sesi açabilirsin.",style=MaterialTheme.typography.bodySmall)
            Text("Hemen gönder kapalıysa duyulan metni önce düzenleyebilirsin.",style=MaterialTheme.typography.bodySmall)
            Text("Çevrimiçi ses hizmeti konuşmanı ve seslendirilen yanıtlarını alabilir.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick=assistant){Text("Varsayılan asistan olarak seç")}
        } }
        item { SettingCard("Telefon işlemleri") {
            SettingSwitch("Onaydan sonra doğrudan ara",state.settings.directCall){model.settings(state.settings.copy(directCall=it))}
            Text("Kapalıysa arama ekranı açılır.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={alias=true}){Text("Kişi kısayolu ekle")}
        } }
        item { SettingCard("Bildirimler") {
            SettingSwitch("Yeni bildirimleri yerelde tut",state.settings.notifications){model.settings(state.settings.copy(notifications=it))}
            Text("7 gün saklanır. Android erişimini ayrıca aç.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick=notifications){Text("Android bildirim erişimi")}
        } }
        item { SettingCard("Görünüm") {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("system" to "Sistem","light" to "Açık","dark" to "Koyu").forEach { (value,label) ->
                FilterChip(selected=state.settings.theme==value,onClick={model.settings(state.settings.copy(theme=value))},label={Text(label)},modifier=Modifier.testTag("theme-$value"))
            } }
            SettingSwitch("Hareketleri azalt",state.settings.reduceMotion){model.settings(state.settings.copy(reduceMotion=it))}
        } }
        item { SettingCard("Ek AI bağlantısı") {
            Text(if(state.hasKey) "Bağlantı kayıtlı" else "İsteğe bağlı",fontWeight=FontWeight.SemiBold)
            Text("Genel sohbet ve görsel analizi için.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={ai=true},modifier=Modifier.testTag("ai-settings")){Text("Bağlantıyı ayarla")}
        } }
        item { SettingCard("Verilerin") {
            Text("Kayıtların bu cihazda şifreli saklanır.",style=MaterialTheme.typography.bodyMedium)
            TextButton(onClick={backup=true}){Text("Yedek çıkar")}
            Text("Kutarya Mobile · "+BuildConfig.VERSION_NAME,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
    if(ai) {
        var key by remember { mutableStateOf("") }; var name by remember { mutableStateOf(state.settings.model) }
        var consent by remember { mutableStateOf(state.settings.cloudConsent) }; var private by remember { mutableStateOf(state.settings.sharePrivate) }
        AlertDialog(onDismissRequest={ai=false},title={Text("AI bağlantısı · Gemini")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(key,{key=it},label={Text(if(state.hasKey) "Yeni API anahtarı · boşsa korunur" else "Gemini API anahtarı")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.testTag("api-key"))
            OutlinedTextField(name,{name=it},label={Text("Model kimliği")},singleLine=true,modifier=Modifier.testTag("model-id"))
            SettingSwitch("Metin ve ekleri AI sağlayıcısına aktar",consent){consent=it}
            SettingSwitch("Not ve bildirim içeriğini AI ile paylaş",private){private=it}
            Text("Açık konuşmanın sınırlı geçmişi yanıta bağlam sağlar. Eski kayıtlar otomatik aktarılmaz.",style=MaterialTheme.typography.bodySmall)
            Text("Google AI Studio anahtarı gerekir. Kullanım ücretleri hesabına bağlıdır.",style=MaterialTheme.typography.bodySmall)
            if(state.hasKey) TextButton(onClick={model.removeKey();key=""}){Text("Kayıtlı anahtarı kaldır")}
            TextButton(onClick=model::loadModels,enabled=state.hasKey && state.settings.cloudConsent){Text("Hesabımdaki modelleri getir")}
            if(state.models.isNotEmpty()) LazyColumn(Modifier.heightIn(max=140.dp)){ items(state.models){item->TextButton(onClick={name=item}){Text(item)}} }
        }},confirmButton={TextButton(onClick={model.settings(state.settings.copy(model=name.trim(),cloudConsent=consent,sharePrivate=private),key);key="";ai=false},enabled=name.isNotBlank(),modifier=Modifier.testTag("save-ai")){Text("Kaydet")}},dismissButton={TextButton(onClick={ai=false;key=""}){Text("Vazgeç")}})
    }
    if(alias) {
        var name by remember { mutableStateOf("") }; var person by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest={alias=false},title={Text("Kişi kısayolu")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name,{name=it},label={Text("Söyleyeceğin ad · annem")},singleLine=true)
            OutlinedTextField(person,{person=it},label={Text("Kişinin adı")},singleLine=true)
            OutlinedTextField(phone,{phone=it},label={Text("Telefon · +90...")},singleLine=true)
            Text("Yalnız cihazda saklanır. Aramadan önce numara gösterilir.",style=MaterialTheme.typography.bodySmall)
        }},confirmButton={TextButton(onClick={model.alias(name,person,phone);alias=false},enabled=name.isNotBlank()&&phone.isNotBlank()){Text("Kaydet")}},dismissButton={TextButton(onClick={alias=false}){Text("Vazgeç")}})
    }
    if(backup) Confirm("Yedek çıkar","Her türün son 200 not, liste, hatırlatma ve rutin kaydı şifresiz dosyaya yazılır. API anahtarı ve konuşmalar eklenmez.",{backup=false;export()},{backup=false})
}
@Composable private fun SettingCard(title:String,content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary); content()
        }
    }
}
@Composable private fun SettingSwitch(label:String,value:Boolean,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium); Switch(checked=value,onCheckedChange=change)
    }
}
@Composable private fun Confirm(title:String,body:String,accept:()->Unit,dismiss:()->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text(title)},text={Text(body)},confirmButton={TextButton(onClick=accept){Text("Onayla")}},dismissButton={TextButton(onClick=dismiss){Text("Vazgeç")}})
}
