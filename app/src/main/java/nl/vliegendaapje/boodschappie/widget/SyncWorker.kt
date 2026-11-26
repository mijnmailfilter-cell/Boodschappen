package nl.vliegendaapje.boodschappie.widget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import nl.vliegendaapje.boodschappie.MainActivity
import nl.vliegendaapje.boodschappie.R
import nl.vliegendaapje.boodschappie.data.Repo
import java.util.concurrent.TimeUnit

/**
 * Kijkt ongeveer elk kwartier of de ander iets heeft toegevoegd (gratis, zonder pushserver)
 * en ververst meteen de widget.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("boodschappie", Context.MODE_PRIVATE)
        val code = prefs.getString("household", null) ?: return Result.success()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val items = Repo().fetchItems(code)
        val now = System.currentTimeMillis()
        val since = prefs.getLong(KEY_LAST_SEEN, 0L)

        if (since > 0 && prefs.getBoolean("notify", true)) {
            val fresh = items.filter { !it.checked && it.addedAt > since && it.byUid.isNotEmpty() && it.byUid != uid }
            if (fresh.isNotEmpty()) Notifier.showAdded(ctx, fresh.map { it.byName }, fresh.map { it.name })
        }
        prefs.edit().putLong(KEY_LAST_SEEN, now).apply()

        WidgetRefresher.refreshAll(ctx)
        return Result.success()
    }

    companion object {
        const val KEY_LAST_SEEN = "lastSeen"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("boodschappie-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Alles gezien (app staat open): vanaf nu tellen. */
        fun markSeen(context: Context) {
            context.getSharedPreferences("boodschappie", Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_SEEN, System.currentTimeMillis()).apply()
            NotificationManagerCompat.from(context).cancel(Notifier.ID_ADDED)
        }
    }
}

object Notifier {
    const val CHANNEL = "toegevoegd"
    const val ID_ADDED = 1

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Nieuwe producten", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Melding als iemand anders iets op de lijst zet"
                }
            )
        }
    }

    fun showAdded(context: Context, who: List<String>, products: List<String>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val names = who.filter { it.isNotBlank() }.distinct()
        val person = if (names.isEmpty()) "Iemand" else names.joinToString(" en ")
        val n = products.size
        val title = "$person zette $n ${if (n == 1) "product" else "producten"} op de lijst"
        val body = products.distinct().take(8).joinToString(", ") + if (products.size > 8) ", …" else ""

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(0xFF2E7D5B.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_ADDED, notification) }
    }
}
