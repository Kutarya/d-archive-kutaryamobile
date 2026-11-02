package com.kutarya.mobile

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** İçerik SQLite'a yazılmadan önce cihaz anahtarıyla şifrelenir. */
class Vault(context: Context) {
    companion object { private val keyLock=Any() }
    private val prefs = context.getSharedPreferences("kutarya-vault", Context.MODE_PRIVATE)
    private fun key(): SecretKey = synchronized(keyLock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias("kutarya-mobile-v1")) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("kutarya-mobile-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
                generateKey()
            }
        }
        store.getKey("kutarya-mobile-v1",null) as SecretKey
    }
    @Synchronized fun encrypt(text: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        return Base64.encodeToString(c.iv,Base64.NO_WRAP) + ":" + Base64.encodeToString(c.doFinal(text.toByteArray(StandardCharsets.UTF_8)),Base64.NO_WRAP)
    }
    @Synchronized fun decrypt(text: String): String {
        val p = text.split(':',limit=2); require(p.size==2) { "Şifreli kayıt okunamadı." }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP))) }
        return String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8)
    }
    @Synchronized fun set(name: String, value: String) {
        require(value.length<=32000) { "Güvenli kayıt çok büyük." }
        val editor = prefs.edit()
        if (value.isEmpty()) editor.remove(name) else editor.putString(name,encrypt(value))
        check(editor.commit()) { "Güvenli kayıt kaydedilemedi." }
    }
    @Synchronized fun get(name: String): String = prefs.getString(name,null)?.let(::decrypt) ?: ""
    fun has(name: String): Boolean = prefs.contains(name)
}

class LocalStore(context: Context) : SQLiteOpenHelper(context,"kutarya-mobile.db",null,1) {
    val vault = Vault(context)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,kind TEXT NOT NULL,created INTEGER NOT NULL,body TEXT NOT NULL)")
        db.execSQL("CREATE INDEX item_kind ON items(kind,created)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    override fun onConfigure(db: SQLiteDatabase) { db.enableWriteAheadLogging(); db.setForeignKeyConstraintsEnabled(true) }
    @Synchronized fun put(kind: String, body: JSONObject, id: Long? = null): Long {
        val values = android.content.ContentValues().apply {
            put("kind",kind); put("created",System.currentTimeMillis()); put("body",vault.encrypt(body.toString()))
        }
        return if (id == null) writableDatabase.insertOrThrow("items",null,values)
        else { check(writableDatabase.update("items",values,"id=? AND kind=?", arrayOf(id.toString(),kind))==1); id }
    }
    @Synchronized fun list(kind: String, limit: Int = 200): List<SavedItem> {
        require(limit in 1..1000)
        val out = mutableListOf<SavedItem>()
        readableDatabase.query("items",arrayOf("id","kind","created","body"),"kind=?", arrayOf(kind),null,null,"created DESC",limit.toString()).use { c ->
            while(c.moveToNext()) out += SavedItem(c.getLong(0),c.getString(1),c.getLong(2),JSONObject(vault.decrypt(c.getString(3))))
        }
        return out
    }
    @Synchronized fun delete(kind: String, id: Long): Boolean = writableDatabase.delete("items","kind=? AND id=?",arrayOf(kind,id.toString()))==1
    @Synchronized fun clear(kind: String) { writableDatabase.delete("items","kind=?",arrayOf(kind)) }
    @Synchronized fun prune(kind: String, count: Int, age: Long? = null) {
        if (age != null) writableDatabase.delete("items","kind=? AND created<?",arrayOf(kind,(System.currentTimeMillis()-age).toString()))
        writableDatabase.execSQL("DELETE FROM items WHERE kind=? AND id NOT IN (SELECT id FROM items WHERE kind=? ORDER BY created DESC LIMIT ?)",arrayOf<Any>(kind,kind,count))
    }
    fun settings(): AppSettings = vault.get("settings").takeIf(String::isNotEmpty)?.let { AppSettings.from(JSONObject(it)) } ?: AppSettings()
    fun settings(settings: AppSettings) = vault.set("settings",settings.json().toString())
}
