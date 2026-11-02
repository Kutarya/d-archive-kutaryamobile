package com.kutarya.mobile

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.json.JSONObject

object PlatformActions {
    fun launch(activity:Activity, action:DeviceAction):UiReply {
        if(action.type in setOf("permission","approve")) return UiReply(true)
        val a=action.args
        val intent=when(action.type) {
            "app" -> activity.packageManager.getLaunchIntentForPackage(a.getString("package"))
            "alarm" -> Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,a.getInt("hour"))
                .putExtra(AlarmClock.EXTRA_MINUTES,a.getInt("minute")).putExtra(AlarmClock.EXTRA_MESSAGE,a.optString("label","Kutarya Mobile")).putExtra(AlarmClock.EXTRA_SKIP_UI,false)
            "timer" -> Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,a.getInt("seconds"))
                .putExtra(AlarmClock.EXTRA_MESSAGE,"Kutarya Mobile").putExtra(AlarmClock.EXTRA_SKIP_UI,false)
            "call" -> Intent(if(a.optBoolean("direct")) Intent.ACTION_CALL else Intent.ACTION_DIAL,Uri.fromParts("tel",a.getString("number"),null))
            "message" -> if(LocalParser.normalize(a.optString("app"))=="whatsapp") {
                val phone=a.getString("number").filter(Char::isDigit)
                Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/$phone").buildUpon().appendQueryParameter("text",a.getString("text")).build())
            } else Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",a.getString("number"),null)).putExtra("sms_body",a.getString("text"))
            "email" -> Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:").buildUpon().opaquePart(a.getString("to")).build())
                .putExtra(Intent.EXTRA_EMAIL,arrayOf(a.getString("to"))).putExtra(Intent.EXTRA_SUBJECT,a.getString("subject")).putExtra(Intent.EXTRA_TEXT,a.getString("body"))
            "calendar" -> Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE,a.getString("title")).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,a.getLong("start"))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,a.getLong("start")+a.getInt("duration_minutes")*60000L)
            "map" -> Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode(a.getString("query"))))
            "browser" -> Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search").buildUpon().appendQueryParameter("q",a.getString("query")).build())
            "settings" -> Intent(when(a.getString("panel")) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "sound" -> Settings.ACTION_SOUND_SETTINGS
                else -> Settings.ACTION_SETTINGS
            })
            else -> null
        } ?: return UiReply(false,"Bu işlem için uygun uygulama bulunamadı.")
        return try { activity.startActivity(intent); UiReply(true,"launched") }
        catch(_:ActivityNotFoundException) { UiReply(false,"Bu işlem için uygun uygulama bulunamadı.") }
        catch(_:SecurityException) { UiReply(false,"Android bu işlemi izin nedeniyle reddetti.") }
        catch(_:Exception) { UiReply(false,"İşlem başlatılamadı.") }
    }
}

object ReminderScheduler {
    fun cancel(context:Context,id:Long) {
        val pi=PendingIntent.getBroadcast(context,id.toInt(),Intent(context,ReminderReceiver::class.java).putExtra("id",id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if(pi!=null) { context.getSystemService(AlarmManager::class.java).cancel(pi); pi.cancel() }
        context.getSystemService(NotificationManager::class.java).cancel(id.toInt())
    }
    fun schedule(context:Context,id:Long,at:Long) {
        require(id in 1..Int.MAX_VALUE)
        val pi=PendingIntent.getBroadcast(context,id.toInt(),Intent(context,ReminderReceiver::class.java).putExtra("id",id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
    }
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val id=intent.getLongExtra("id",-1)
        val store=LocalStore(context)
        try {
            val row=store.list("reminder",1000).firstOrNull { it.id==id } ?: return
            if(row.value.optBoolean("delivered")) return
            val manager=context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("reminders","Hatırlatmalar",NotificationManager.IMPORTANCE_HIGH))
            val pending=PendingIntent.getActivity(context,id.toInt(),Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val public=NotificationCompat.Builder(context,"reminders").setSmallIcon(R.drawable.ic_kutarya).setContentTitle("Kutarya Mobile").setContentText("Bir hatırlatman var.").build()
            val notice=NotificationCompat.Builder(context,"reminders").setSmallIcon(R.drawable.ic_kutarya).setContentTitle("Kutarya hatırlatması")
                .setContentText(row.value.optString("text")).setStyle(NotificationCompat.BigTextStyle().bigText(row.value.optString("text")))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public).setContentIntent(pending).setAutoCancel(true).build()
            manager.notify(id.toInt(),notice)
            store.put("reminder",JSONObject(row.value.toString()).put("delivered",true),id)
        } catch(_:Exception) { /* İzin kaldırıldıysa arka planda veri veya hata ayrıntısı loglanmaz. */ }
        finally { store.close() }
    }
}
class BootReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action!=Intent.ACTION_BOOT_COMPLETED) return
        val store=LocalStore(context)
        try { store.list("reminder",1000).filter { !it.value.optBoolean("delivered") && it.value.optLong("at")>System.currentTimeMillis() }
            .forEach { ReminderScheduler.schedule(context,it.id,it.value.getLong("at")) } }
        catch(_:Exception) { } finally { store.close() }
    }
}
class KutaryaNotifications:NotificationListenerService() {
    override fun onNotificationPosted(sbn:StatusBarNotification) {
        if(sbn.packageName==packageName) return
        val store=LocalStore(this)
        try {
            if(!store.settings().notifications) return
            val extras=sbn.notification.extras
            val title=extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text=extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if(Privacy.notification("$title $text")==null || (title.isBlank() && text.isBlank())) return
            val app=try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName,0)).toString() } catch(_:Exception) { sbn.packageName }
            val id=sbn.key
            val existing=store.list("notification",200).firstOrNull { it.value.optString("key")==id }
            store.put("notification",JSONObject().put("key",id).put("app",app.take(100)).put("title",title.take(200)).put("text",text.take(1500)),existing?.id)
            store.prune("notification",200,7L*86400000L)
        } catch(_:Exception) { } finally { store.close() }
    }
}
