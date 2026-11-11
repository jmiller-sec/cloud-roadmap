package com.josephmiller.cloudroadmap

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** Buzzes the phone when a 50-minute block ends or the daily cap is hit, even if the app is closed. */
object TimerAlarm {
    private const val WORK = "burnout-timer"

    fun schedule(ctx: Context, minutes: Int, title: String, text: String) {
        val req = OneTimeWorkRequestBuilder<TimerWorker>()
            .setInitialDelay(minutes.toLong().coerceAtLeast(1), TimeUnit.MINUTES)
            .setInputData(workDataOf("title" to title, "text" to text))
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK, androidx.work.ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(WORK)
}

class TimerWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Sync.notify(applicationContext, inputData.getString("title") ?: "Cloud Roadmap", listOf(inputData.getString("text") ?: ""), tab = 0)
        return Result.success()
    }
}
