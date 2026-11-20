package com.accidentalgardener.app.work

import android.Manifest
import android.annotation.SuppressLint
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
import com.accidentalgardener.app.MainActivity
import com.accidentalgardener.app.R
import com.accidentalgardener.app.data.AppDatabase
import com.accidentalgardener.app.data.CropCatalog
import com.accidentalgardener.app.data.Community
import com.accidentalgardener.app.domain.GardenAlerts
import com.accidentalgardener.app.domain.Tasks
import com.accidentalgardener.app.domain.WaterAdvisor
import com.accidentalgardener.app.weather.WeatherRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Notifications {
    const val CH_ALERTS = "garden_alerts"
    const val CH_WATER = "garden_water"
    const val CH_JOBS = "garden_jobs"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, "Weather alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Frost, wind, heavy rain, heat and Met Éireann warnings"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_WATER, "Watering reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A morning nudge when beds need water"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_JOBS, "Garden jobs", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "What needs sowing, planting or harvesting today"
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, id: Int, channel: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_leaf)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFF3F5E2F.toInt())
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

/** Runs every few hours in the background, checks the weather and alerts the user. */
class WeatherWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val dao = AppDatabase.get(ctx).dao()
        CropCatalog.init(ctx)
        val garden = dao.garden() ?: return Result.success()

        // Check for newer growing guides once a day.
        if (CropCatalog.sourceUrl(ctx).isNotBlank() &&
            System.currentTimeMillis() - CropCatalog.lastChecked(ctx) > TimeUnit.HOURS.toMillis(20)
        ) {
            try { CropCatalog.refresh(ctx) } catch (e: Exception) { /* try again next time */ }
        }

        if (!garden.notificationsOn) return Result.success()

        // Let people know when the admins reply to, add or decline something they asked for.
        try {
            val mine = Community.visibleRequests(ctx)
            val updated = mine.map { r ->
                if (r.lastStatus in setOf("ADDED", "DECLINED")) return@map r
                val st = Community.status(r.number) ?: return@map r
                if (st.code != r.lastStatus) {
                    val name = r.plant.ifBlank { "your request" }
                    val (title, text) = when (st.code) {
                        "QUESTION" -> "❓ A question about $name" to "The admins need a bit more info. Open About, then My requests, to answer."
                        "APPROVED" -> "✅ $name was approved" to "It'll be added to the plant database very soon."
                        "ADDED" -> "🎉 $name is in the database" to "It'll appear in your Seed tin after the next update."
                        "DECLINED" -> "$name wasn't added" to (st.adminNote ?: "Open About, then My requests, for details.")
                        else -> null to null
                    }
                    if (title != null && text != null) Notifications.show(ctx, ("req-" + r.number).hashCode(), Notifications.CH_JOBS, title, text)
                }
                Community.withStatus(r, st.code)
            }
            if (updated != mine) Community.saveMyRequests(ctx, updated)
        } catch (e: Exception) {
            // Not important enough to fail the weather check over.
        }

        val snap = try {
            WeatherRepository(ctx).fetch(garden.latitude, garden.longitude)
        } catch (e: Exception) {
            return Result.retry()
        }

        val beds = dao.beds()
        val today = LocalDate.now()
        val prefs = ctx.getSharedPreferences("alerts", Context.MODE_PRIVATE)
        val sent = (prefs.getStringSet("sent", emptySet()) ?: emptySet()).toMutableSet()
        if (sent.size > 300) sent.clear()

        GardenAlerts.evaluate(snap, beds.any { it.isTunnel }, today)
            .filter { it.key !in sent }
            .forEach { a ->
                Notifications.show(ctx, a.key.hashCode(), Notifications.CH_ALERTS, "${a.emoji} ${a.title}", a.message)
                sent += a.key
            }

        // One watering nudge per morning.
        val waterKey = "water-$today"
        if (LocalTime.now().hour in 6..11 && waterKey !in sent) {
            val customs = dao.customList()
            val advice = WaterAdvisor.advise(
                snap, beds, dao.plantingsFor(garden.planYear), dao.journalList(),
                { CropCatalog.resolve(it, customs) }, today,
            ).filter { it.needed }
            if (advice.isNotEmpty()) {
                Notifications.show(
                    ctx, waterKey.hashCode(), Notifications.CH_WATER,
                    "💧 Watering today",
                    advice.joinToString("\n") { "${it.title}. ${it.message}" },
                )
                sent += waterKey
            }
        }

        // One morning list of today's garden jobs, each with its how-to waiting in the app.
        val jobsKey = "jobs-$today"
        if (LocalTime.now().hour in 6..11 && jobsKey !in sent) {
            val customs = dao.customList()
            val due = Tasks.all(dao.plantingsFor(garden.planYear)) { CropCatalog.resolve(it, customs) }
                .filter { !it.date.isAfter(today) && !it.date.isBefore(today.minusDays(2)) }
            if (due.isNotEmpty()) {
                Notifications.show(
                    ctx, jobsKey.hashCode(), Notifications.CH_JOBS,
                    "🧑‍🌾 ${due.size} garden job${if (due.size == 1) "" else "s"} today",
                    due.joinToString("\n") { "${it.crop.emoji} ${it.title.replaceFirstChar { c -> c.uppercase() }}" } +
                        "\nOpen the app for step-by-step help.",
                )
                sent += jobsKey
            }
        }

        prefs.edit().putStringSet("sent", sent).apply()
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<WeatherWorker>(4, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("weather-check", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
