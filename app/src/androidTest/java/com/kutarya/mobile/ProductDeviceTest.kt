package com.kutarya.mobile

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProductDeviceTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private lateinit var model:MobileModel
    private val instrument get()=InstrumentationRegistry.getInstrumentation()
    @Before fun setup() {
        rule.runOnUiThread { model=ViewModelProvider(rule.activity)[MobileModel::class.java] }
        rule.waitUntil(10000) { model.state.value.ready }
        rule.runOnUiThread { model.settings(AppSettings(speak=false));model.removeKey();model.newConversation() }
        rule.waitForIdle()
    }
    private fun submit(text:String,answer:String) {
        rule.onNodeWithTag("command-input").performTextReplacement(text)
        rule.onNodeWithTag("send").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.lastRequest==text && model.state.value.answer.contains(answer) }
    }
    private fun approve() {
        rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("approve-action").performClick()
        rule.waitUntil(10000) { !model.state.value.busy }
    }
    private fun screenshot(name:String) {
        val bitmap=instrument.uiAutomation.takeScreenshot() ?: error("Ekran alınamadı.")
        File(rule.activity.getExternalFilesDir(null),name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    @Test fun messagesRemainVisibleAndConversationReopensFromEncryptedStore() {
        submit("17+28","45");submit("12*12","144")
        val conversation=model.state.value.conversationId
        assertEquals(2,model.state.value.messages.size)
        assertEquals(listOf("45","144"),model.state.value.messages.map { it.answer })
        rule.onNodeWithTag("assistant-list").performScrollToNode(hasText("45"))
        rule.onNodeWithText("45").assertIsDisplayed()
        val reopened=LocalStore(rule.activity)
        try {
            assertEquals(conversation,reopened.vault.get("active-conversation"))
            assertEquals(2,ConversationMemory.messages(reopened.list("task",500).map { TaskRun.from(it.value) },conversation).size)
        } finally { reopened.close() }
        rule.onNodeWithTag("new-conversation").performClick()
        assertTrue(model.state.value.messages.isEmpty())
        rule.onNodeWithTag("conversations-button").performClick()
        rule.onNodeWithTag("conversation-search").performTextInput("17+28")
        rule.onNodeWithTag("conversation-search").performImeAction()
        rule.onNodeWithTag("open-conversation-"+conversation).assertIsDisplayed()
        rule.onNodeWithTag("open-conversation-"+conversation).performClick()
        rule.waitUntil(10000) { model.state.value.conversationId==conversation && model.state.value.messages.size==2 }
        assertEquals(2,model.state.value.messages.size)
        assertEquals("144",model.state.value.answer)
        rule.onNodeWithTag("assistant-list").performScrollToNode(hasText("144"))
        screenshot("v1.2-conversation.png")
    }
    @Test fun noteCanBeCreatedFromFormWithoutCommandSyntax() {
        val text="Formdan Türkçe not "+UUID.randomUUID().toString().take(6)
        rule.onNodeWithTag("tab-1").performClick();rule.onNodeWithTag("notes-filter").performClick()
        rule.onNodeWithTag("new-record").performClick()
        rule.onNodeWithTag("record-text").performTextInput(text)
        rule.onNodeWithTag("save-record").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.notes.any { it.value.optString("text")==text } }
        rule.waitForIdle()
        rule.onNodeWithText(text).assertIsDisplayed()
    }
    @Test fun listsSupportAddingRemovingAndDeletingWithApproval() {
        val title="Form liste "+UUID.randomUUID().toString().take(6)
        rule.onNodeWithTag("tab-1").performClick();rule.onNodeWithTag("lists-filter").performClick()
        rule.onNodeWithTag("new-record").performClick()
        rule.onNodeWithTag("record-title").performTextInput(title);rule.onNodeWithTag("record-text").performTextInput("süt\nekmek")
        rule.onNodeWithTag("save-record").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.checklists.any { it.value.optString("title")==title } }
        val id=model.state.value.checklists.first { it.value.optString("title")==title }.id
        rule.onNodeWithTag("records-list").performScrollToNode(hasTestTag("add-list-item-"+id))
        rule.onNodeWithTag("add-list-item-"+id).performClick()
        rule.onNodeWithTag("list-item-text").performTextInput("yumurta");rule.onNodeWithTag("save-list-item").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.checklists.first { it.id==id }.value.getJSONArray("items").length()==3 }
        rule.onNodeWithTag("remove-list-item-"+id+"-0").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Vazgeç").performClick();rule.waitUntil(10000) { !model.state.value.busy }
        assertEquals(3,model.store.list("checklist").first { it.id==id }.value.getJSONArray("items").length())
        rule.onNodeWithTag("remove-list-item-"+id+"-0").performClick();approve()
        assertEquals("ekmek",model.store.list("checklist").first { it.id==id }.value.getJSONArray("items").getJSONObject(0).getString("text"))
        rule.onNodeWithTag("delete-list-"+id).performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("approve-action").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Vazgeç").performClick();rule.waitUntil(10000) { !model.state.value.busy }
        assertTrue(model.store.list("checklist").any { it.id==id })
        rule.onNodeWithTag("delete-list-"+id).performClick();approve()
        assertFalse(model.store.list("checklist").any { it.id==id })
    }
    @Test fun reminderFormSavesExplicitFutureDateAndTime() {
        instrument.uiAutomation.grantRuntimePermission("com.kutarya.mobile",Manifest.permission.POST_NOTIFICATIONS)
        val text="Form hatırlatma "+UUID.randomUUID().toString().take(6)
        rule.onNodeWithTag("tab-1").performClick();rule.onNodeWithTag("reminders-filter").performClick()
        rule.onNodeWithTag("new-record").performClick()
        rule.onNodeWithTag("record-text").performTextInput(text)
        rule.onNodeWithTag("record-date").assertIsDisplayed();rule.onNodeWithTag("record-time").assertIsDisplayed()
        rule.onNodeWithTag("save-record").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.reminders.any { it.value.optString("text")==text } }
        val saved=model.store.list("reminder").first { it.value.optString("text")==text }
        assertTrue(saved.value.getLong("at")>System.currentTimeMillis())
    }
    @Test fun routinePlanRejectsUnknownStepBeforeAnyWrite() {
        val title="Geçersiz rutin "+UUID.randomUUID().toString().take(6)
        rule.onNodeWithTag("tab-2").performClick();rule.onNodeWithTag("new-routine").performClick()
        rule.onNodeWithTag("routine-title").performTextInput(title)
        rule.onNodeWithTag("routine-commands").performTextInput("Not al: ilk adım\nDünyayı yönet")
        rule.onNodeWithTag("save-routine").assertIsNotEnabled()
        rule.onNodeWithTag("routine-plan").assertTextContains("anlayamadım",substring=true)
        assertFalse(model.saveRoutine(title,"Not al: ilk adım\nDünyayı yönet"))
        assertFalse(model.store.list("routine").any { it.value.optString("title")==title })
        rule.runOnUiThread { model.clearError() }
    }
    @Test fun relatedSuggestionOnlyFillsComposerAndNeverRunsIt() {
        submit("Bana bir not fikri ver","Genel sorular")
        assertEquals(1,model.state.value.messages.size)
        rule.onNodeWithTag("assistant-list").performScrollToNode(hasTestTag("suggestion-Not kaydet"))
        rule.onNodeWithTag("suggestion-Not kaydet").performClick()
        assertEquals("Not al: ",model.state.value.input)
        assertFalse(model.state.value.busy);assertEquals(1,model.state.value.messages.size)
    }
    @Test fun dailySummaryUsesActualRecordsAndRunsWithoutApi() {
        instrument.uiAutomation.grantRuntimePermission("com.kutarya.mobile",Manifest.permission.POST_NOTIFICATIONS)
        val marker="Günlük kabul "+UUID.randomUUID().toString().take(6)
        submit("20 dakika sonra "+marker+" hatırlat","Hatırlatma kaydedildi")
        rule.onNodeWithTag("new-conversation").performClick()
        rule.onNodeWithTag("daily-summary").performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer.contains(marker) }
        assertEquals(ResultState.VERIFIED,model.state.value.messages.last().steps.single().state)
        assertTrue(model.state.value.messages.last().privateContext)
        assertFalse(model.state.value.hasKey)
        screenshot("v1.2-daily.png")
    }
    @Test fun unknownRoutineCannotBeSavedThroughModelApi() {
        val title="Model geçersiz "+UUID.randomUUID().toString().take(6)
        val count=model.store.list("note").size
        assertFalse(model.saveRoutine(title,"Not al: ilk adım\nBana bir hikaye anlat"))
        rule.runOnUiThread { model.clearError() }
        assertEquals(count,model.store.list("note").size)
        assertFalse(model.store.list("routine").any { it.value.optString("title")==title })
    }
    @Test fun voiceServiceStatusIsVisibleAfterExistingMessage() {
        instrument.uiAutomation.grantRuntimePermission("com.kutarya.mobile",Manifest.permission.RECORD_AUDIO)
        submit("17+28","45")
        rule.onNodeWithTag("microphone").performClick()
        rule.onNodeWithTag("voice-status").assertIsDisplayed()
        assertEquals("45",model.state.value.answer);assertFalse(model.state.value.busy)
        assertEquals(1,model.state.value.messages.size)
        rule.runOnUiThread { model.stop() }
    }
    @Test fun homeAndRecordFormRemainUsableInBothThemes() {
        rule.onNodeWithText("Ne yapalım?").assertIsDisplayed()
        rule.onNodeWithTag("daily-summary").assertIsDisplayed()
        listOf("Not al","Hatırlat","Liste oluştur","Hesapla").forEach { rule.onNodeWithText(it,substring=false).assertIsDisplayed() }
        rule.runOnUiThread { model.settings(model.state.value.settings.copy(theme="light")) }
        rule.waitForIdle();screenshot("v1.2-home-light.png")
        rule.runOnUiThread { model.settings(model.state.value.settings.copy(theme="dark")) }
        rule.waitForIdle();screenshot("v1.2-home-dark.png")
        rule.onNodeWithText("Not al",substring=false).performClick()
        rule.onNodeWithTag("record-text").performTextInput("Sade ve doğrudan not oluşturma.")
        rule.onNodeWithTag("save-record").assertIsEnabled()
        screenshot("v1.2-note-form.png")
        rule.onNodeWithText("Vazgeç").performClick()
    }
    @Test fun sameNamedRoutinesRunTheSelectedCardAndNotAnotherRoutine() {
        val title="Aynı ad "+UUID.randomUUID().toString().take(6)
        rule.runOnUiThread {
            assertTrue(model.saveRoutine(title,"Hesapla 20+2"))
            assertTrue(model.saveRoutine(title,"Hesapla 20+3"))
        }
        rule.waitUntil(10000) { model.state.value.routines.count { it.value.optString("title")==title }==2 }
        val selected=model.state.value.routines.first { it.value.optString("title")==title && it.value.optString("commands")=="Hesapla 20+2" }
        rule.onNodeWithTag("tab-2").performClick()
        rule.onNodeWithTag("routines-list").performScrollToNode(hasTestTag("run-routine-"+selected.id))
        rule.onNodeWithTag("run-routine-"+selected.id).performClick()
        rule.waitUntil(10000) { !model.state.value.busy && model.state.value.answer=="22" }
        assertEquals(title+" çalıştır",model.state.value.lastRequest)
        assertEquals(1,model.state.value.history.first().steps.size)
    }
}
