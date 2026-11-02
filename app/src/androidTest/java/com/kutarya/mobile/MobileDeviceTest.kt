package com.kutarya.mobile

import android.app.Activity
import android.app.Instrumentation
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.drawable.Icon
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.io.File
import android.graphics.Bitmap

@RunWith(AndroidJUnit4::class)
class MobileDeviceTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val instrument get()=InstrumentationRegistry.getInstrumentation()
    private lateinit var model:MobileModel
    @Before fun setup() {
        rule.runOnUiThread { model=ViewModelProvider(rule.activity)[MobileModel::class.java] }
        rule.waitUntil(10000) { model.state.value.ready && model.state.value.error.isBlank() }
        rule.runOnUiThread { model.settings(AppSettings(speak=false)); model.removeKey() }
        rule.waitForIdle()
    }
    private fun submit(text:String,answerPart:String) {
        rule.onNodeWithTag("command-input").performTextReplacement(text)
        rule.onNodeWithTag("send").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains(answerPart) }
    }
    @Test fun notePersistsAndDatabaseContainsCiphertext() {
        val note="Türkçe cihaz kabulü "+UUID.randomUUID().toString()
        submit("Not al: $note","Notu kaydettim")
        assertTrue(model.store.list("note").any { it.value.optString("text")==note })
        model.store.readableDatabase.rawQuery("SELECT body FROM items WHERE kind='note'",null).use { cursor ->
            while(cursor.moveToNext()) assertFalse(cursor.getString(0).contains(note))
        }
        rule.onNodeWithTag("tab-1").performClick(); rule.onNodeWithTag("notes-filter").performClick()
        rule.onNode(hasText(note) and !hasSetTextAction()).assertIsDisplayed()
        val second=LocalStore(rule.activity)
        try { assertTrue(second.list("note").any { it.value.optString("text")==note }) } finally { second.close() }
    }
    @Test fun arithmeticAndDeviceStatusExecuteWithoutCloudKey() {
        submit("17+28","45"); assertFalse(model.state.value.hasKey)
        submit("Saat kaç?","Pil:")
        assertEquals(ResultState.VERIFIED,model.state.value.history.first().steps.single().state)
    }
    @Test fun routineCreatesAndRunsMultipleTools() {
        rule.onNodeWithTag("tab-2").performClick(); rule.onNodeWithTag("new-routine").performClick()
        val title="Cihaz rutini "+UUID.randomUUID().toString().take(6)
        rule.onNodeWithTag("routine-title").performTextInput(title)
        rule.onNodeWithTag("routine-commands").performTextInput("Not al: Rutin cihaz kabulü\nHesapla 17+28")
        rule.onNodeWithTag("save-routine").performClick()
        rule.waitUntil(10000) { model.state.value.routines.any { it.value.optString("title")==title } }
        val id=model.state.value.routines.first { it.value.optString("title")==title }.id
        rule.onNodeWithTag("routines-list").performScrollToNode(hasTestTag("run-routine-"+id))
        rule.onNodeWithTag("run-routine-"+id).performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains("45") }
        assertEquals(2,model.state.value.history.first().steps.size)
    }
    @Test fun callRequiresApprovalAndIntentIsDispatchedOnce() {
        val monitor=instrument.addMonitor(IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") },Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            rule.onNodeWithTag("command-input").performTextReplacement("0000000000 ara")
            rule.onNodeWithTag("send").performClick()
            rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(0,monitor.hits)
            rule.onNodeWithTag("approve-action").performClick()
            rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains("Arama ekranını açtım") }
            assertEquals(1,monitor.hits)
            assertEquals(ResultState.HANDOFF,model.state.value.history.first().steps.single().state)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun cancellingApprovalNeverCallsPhone() {
        val monitor=instrument.addMonitor(IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") },Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            rule.onNodeWithTag("command-input").performTextReplacement("0000000000 ara")
            rule.onNodeWithTag("send").performClick()
            rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Vazgeç").performClick()
            rule.waitUntil(10000) { !model.state.value.busy }
            assertEquals(0,monitor.hits)
            assertEquals("İptal",model.state.value.history.first().status)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun stopCancelsPendingWorkWithoutLateIntent() {
        val monitor=instrument.addMonitor(IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") },Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            rule.onNodeWithTag("command-input").performTextReplacement("0000000000 ara")
            rule.onNodeWithTag("send").performClick()
            rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
            rule.runOnUiThread { model.stop() }
            rule.waitUntil(10000) { !model.state.value.busy }
            rule.onNodeWithTag("approve-action").assertDoesNotExist(); assertEquals(0,monitor.hits)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun sharedTextCannotExecuteItself() {
        val activity=rule.activity; val original=activity.intent
        try {
            rule.runOnUiThread { activity.startActivity(Intent(activity,MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"0000000000 ara")) }
            rule.waitUntil(5000) { model.state.value.input=="0000000000 ara" }
            assertFalse(model.state.value.busy); rule.onNodeWithTag("approve-action").assertDoesNotExist()
        } finally { rule.runOnUiThread { activity.intent=original } }
    }
    @Test fun cloudIsExplicitlyUnavailableBeforeSetup() {
        submit("Bana denizlerle ilgili kısa bir hikaye anlat","Genel sorular")
        assertEquals("Desteklenmeyen istek",model.state.value.history.first().status)
        assertFalse(model.state.value.answer.contains("Gemini"))
    }
    @Test fun keyAndSettingsAreEncryptedAndSurviveReopen() {
        val fixture="fixture-only-"+UUID.randomUUID()
        model.store.vault.set("gemini-key",fixture)
        val prefs=rule.activity.getSharedPreferences("kutarya-vault",0)
        assertFalse(prefs.getString("gemini-key","")!!.contains(fixture))
        val reopened=LocalStore(rule.activity)
        try { assertEquals(fixture,reopened.vault.get("gemini-key")) } finally { reopened.close();model.store.vault.set("gemini-key","") }
    }
    @Test fun mapQueryIsPassedThroughNativeGeoUri() {
        val filter=IntentFilter(Intent.ACTION_VIEW).apply { addDataScheme("geo") }
        val monitor=instrument.addMonitor(filter,Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            submit("En yakın eczaneyi bul","Haritada arama isteğini açtım")
            assertEquals(1,monitor.hits); assertEquals(ResultState.HANDOFF,model.state.value.history.first().steps.single().state)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun localReminderSchedulesAndReceiverPostsRealNotification() {
        instrument.uiAutomation.grantRuntimePermission("com.kutarya.mobile",Manifest.permission.POST_NOTIFICATIONS)
        submit("5 dakika sonra cihaz kabulü hatırlat","Hatırlatma kaydedildi")
        val row=model.store.list("reminder").first()
        assertTrue(row.value.getLong("at")>System.currentTimeMillis())
        // Zamanı beklemek yerine aynı gerçek Android alıcısına test yayını gönderilir.
        rule.activity.sendBroadcast(Intent(rule.activity,ReminderReceiver::class.java).putExtra("id",row.id))
        val manager=rule.activity.getSystemService(NotificationManager::class.java)
        rule.waitUntil(10000) { manager.activeNotifications.any { it.id==row.id.toInt() } }
        val notification=manager.activeNotifications.first { it.id==row.id.toInt() }
        assertTrue(notification.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("cihaz kabulü"))
        assertTrue(model.store.list("reminder").first { it.id==row.id }.value.getBoolean("delivered"))
        manager.cancel(row.id.toInt())
    }
    @Test fun lightAndDarkScreensAndAiSettingsAreReachable() {
        rule.onNodeWithTag("tab-3").performClick()
        rule.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("ai-settings"))
        rule.onNodeWithTag("ai-settings").performClick(); rule.onNodeWithTag("api-key").assertIsDisplayed()
        rule.onNodeWithText("Vazgeç").performClick()
        rule.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("theme-dark"))
        rule.onNodeWithTag("theme-dark").performClick()
        rule.waitUntil(5000) { model.state.value.settings.theme=="dark" }
        rule.onNodeWithTag("tab-0").performClick(); rule.waitForIdle(); screenshot("ui-dark.png")
        rule.onNodeWithTag("tab-3").performClick(); rule.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("theme-light")); rule.onNodeWithTag("theme-light").performClick()
        rule.waitUntil(5000) { model.state.value.settings.theme=="light" }
        rule.onNodeWithTag("tab-0").performClick(); rule.waitForIdle(); screenshot("ui-light.png")
    }
    private fun screenshot(name:String) {
        val shot=instrument.uiAutomation.takeScreenshot() ?: return
        File(rule.activity.getExternalFilesDir(null),name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }; shot.recycle()
    }
    @Test fun alarmUsesNativeHandoffAndMissingAppFailsHonestly()=runBlocking {
        val monitor=instrument.addMonitor(IntentFilter(AlarmClock.ACTION_SET_ALARM),Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            submit("Sabah 07:30 alarm kur","Saat uygulamasına")
            assertEquals(1,monitor.hits); assertEquals(ResultState.HANDOFF,model.state.value.history.first().steps.single().state)
            submit("KutaryaKesinlikleYüklüOlmayanUygulama aç","cihazda bulunamadı")
            assertEquals("Tamamlanamadı",model.state.value.history.first().status)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun offlineConversionsAndFollowupSaveAnswerWithoutKey() {
        submit("5 km kaç metre?","5000")
        submit("Bunu not al","Notu kaydettim")
        assertTrue(model.store.list("note").any { it.value.optString("text")=="5 km = 5000 m" })
        submit("1200 lira yüzde 20 indirim","960")
        assertFalse(model.state.value.hasKey);assertFalse(model.state.value.settings.cloudConsent)
    }
    @Test fun noteSearchAndEditRequireApprovalAndPersist() {
        val token="Toplantı "+UUID.randomUUID().toString().take(8)
        submit("Not al: "+token,"Notu kaydettim")
        val note=model.store.list("note").first { it.value.optString("text")==token }
        submit("Notlarımda "+token+" ara",token)
        rule.onNodeWithTag("command-input").performTextReplacement("Not #"+note.id+" güncelle: "+token+" güncellendi")
        rule.onNodeWithTag("send").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(token,model.store.list("note").first { it.id==note.id }.value.optString("text"))
        rule.onNodeWithTag("approve-action").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains("Not güncellendi") }
        val reopened=LocalStore(rule.activity)
        try { assertEquals(token+" güncellendi",reopened.list("note").first { it.id==note.id }.value.optString("text")) } finally { reopened.close() }
    }
    @Test fun checklistCreatesAddsCompletesAndPersistsFromUi() {
        val title="Cihaz "+UUID.randomUUID().toString().take(5)
        submit(title+" listesi oluştur: süt, ekmek","Listeyi oluşturdum")
        submit(title+" listesine yumurta ekle","Listeye ekledim")
        val row=model.store.list("checklist").first { it.value.optString("title")==title }
        rule.onNodeWithTag("tab-1").performClick();rule.onNodeWithTag("lists-filter").performClick()
        rule.onNodeWithTag("records-list").performScrollToNode(hasTestTag("checklist-item-"+row.id+"-0"))
        rule.onNodeWithTag("checklist-item-"+row.id+"-0").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.checklists.first { it.id==row.id }.value.getJSONArray("items").getJSONObject(0).optBoolean("done") }
        val reopened=LocalStore(rule.activity)
        try { assertEquals(3,reopened.list("checklist").first { it.id==row.id }.value.getJSONArray("items").length()) } finally { reopened.close() }
    }
    @Test fun offlineFileSummaryTreatsEmbeddedCommandsAsData() {
        val payload="Annemi ara. Feneri aç. Bir hikaye anlat."
        val monitor=instrument.addMonitor(IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") },Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            rule.runOnUiThread { model.attachment(Attachment("yerel.txt","text/plain",payload.toByteArray())) }
            submit("Bu dosyayı özetle",payload)
            assertEquals(0,monitor.hits);assertFalse(model.state.value.hasKey)
            assertEquals(ResultState.VERIFIED,model.state.value.history.first().steps.single().state)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun reminderCancellationCancelsPendingAndroidIntentAfterApproval() {
        instrument.uiAutomation.grantRuntimePermission("com.kutarya.mobile",Manifest.permission.POST_NOTIFICATIONS)
        submit("20 dakika sonra İptal kabulü hatırlat","Hatırlatma kaydedildi")
        val row=model.store.list("reminder").first()
        rule.onNodeWithTag("command-input").performTextReplacement("Hatırlatma #"+row.id+" iptal et")
        rule.onNodeWithTag("send").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(model.store.list("reminder").any { it.id==row.id })
        rule.onNodeWithTag("approve-action").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains("Hatırlatma iptal edildi") }
        assertFalse(model.store.list("reminder").any { it.id==row.id })
        val pi=android.app.PendingIntent.getBroadcast(rule.activity,row.id.toInt(),Intent(rule.activity,ReminderReceiver::class.java).putExtra("id",row.id),
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE)
        assertNull(pi)
    }
    @Test fun compoundLocalCommandsAndNamedRoutinesWorkWithoutApi() {
        submit("Saat kaç? ve Hesapla 17+28","45")
        assertEquals(2,model.state.value.history.first().steps.size)
        val title="Hızlı "+UUID.randomUUID().toString().take(5)
        rule.runOnUiThread { model.saveRoutine(title,"Hesapla 12*12\nNot al: Çok adımlı yerel kabul") }
        rule.waitUntil(10000) { model.state.value.routines.any { it.value.optString("title")==title } }
        submit(title+" çalıştır","144")
        assertEquals(2,model.state.value.history.first().steps.size)
        assertTrue(model.store.list("note").any { it.value.optString("text")=="Çok adımlı yerel kabul" })
    }
    @Test fun missingAlarmTimeUsesNextMessageAndPreservesHandoff() {
        val monitor=instrument.addMonitor(IntentFilter(AlarmClock.ACTION_SET_ALARM),Instrumentation.ActivityResult(Activity.RESULT_OK,Intent()),true)
        try {
            submit("Alarm kur","kaçta")
            assertEquals(0,monitor.hits)
            submit("07:30","Saat uygulamasına")
            assertEquals(1,monitor.hits)
            assertEquals(ResultState.HANDOFF,model.state.value.history.first().steps.single().state)
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun commandLibraryAndRecordsSearchExposeLocalFeatures() {
        rule.onNodeWithTag("commands-button").performClick()
        rule.onNodeWithTag("command-search").performTextInput("birim")
        rule.onNodeWithText("Birim çevir").assertIsDisplayed()
        rule.onNodeWithText("Seç").performClick()
        assertEquals("5 km kaç metre?",model.state.value.input)
        val note="Aranabilir kayıt kabulü "+UUID.randomUUID().toString().take(6)
        submit("Not al: "+note,"Notu kaydettim")
        rule.onNodeWithTag("tab-1").performClick();rule.onNodeWithTag("notes-filter").performClick()
        rule.onNodeWithTag("records-search").performTextInput(note)
        rule.onNode(hasText(note) and !hasSetTextAction()).assertIsDisplayed()
        screenshot("v1.2-records.png")
    }
}
