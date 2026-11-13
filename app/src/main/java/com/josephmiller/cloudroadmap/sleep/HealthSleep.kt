package com.josephmiller.cloudroadmap.sleep

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Reads the Galaxy Watch's sleep (Samsung Health writes it to Health Connect). Read only. */
object HealthSleep {
    val PERMISSIONS = setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    sealed class State {
        data object Loading : State()
        data object NotInstalled : State()
        data object NeedsPermission : State()
        data class Failed(val message: String) : State()
        /** nights newest first; week = the last 7 for trends */
        data class Ready(val nights: List<NightStats>, val week: SleepWeek) : State()
    }

    val state = MutableStateFlow<State>(State.Loading)

    /** Saves a short status line (no health values) so it syncs and can be checked from the PC side. */
    private fun report(ctx: Context, msg: String) {
        val line = "$msg @ ${java.time.LocalDateTime.now().withNano(0)}"
        runCatching {
            val r0 = com.josephmiller.cloudroadmap.Store.load(ctx)
            if (r0.sleepStatus.substringBefore(" @ ") != msg) com.josephmiller.cloudroadmap.Store.edit(ctx) { it.sleepStatus = line }
        }
    }

    private fun stage(t: Int): Stage = when (t) {
        SleepSessionRecord.STAGE_TYPE_DEEP -> Stage.DEEP
        SleepSessionRecord.STAGE_TYPE_REM -> Stage.REM
        SleepSessionRecord.STAGE_TYPE_LIGHT -> Stage.LIGHT
        SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> Stage.AWAKE
        else -> Stage.ASLEEP
    }

    /** one main sleep per morning; naps under 2 h are left out */
    fun pickNights(all: List<SleepNight>, zone: ZoneId): List<SleepNight> =
        all.filter { Duration.between(it.start, it.end).toMinutes() >= 120 }
            .groupBy { it.end.atZone(zone).toLocalDate() }
            .map { (_, v) -> v.maxBy { Duration.between(it.start, it.end) } }
            .sortedBy { it.end }

    suspend fun refresh(ctx: Context, days: Long = 14, zone: ZoneId = ZoneId.systemDefault()) {
        val sdk = HealthConnectClient.getSdkStatus(ctx)
        if (sdk != HealthConnectClient.SDK_AVAILABLE) { state.value = State.NotInstalled; report(ctx, "health connect unavailable (sdk $sdk)"); return }
        try {
            val c = HealthConnectClient.getOrCreate(ctx)
            if (!c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)) { state.value = State.NeedsPermission; report(ctx, "needs permission"); return }
            if (state.value !is State.Ready) state.value = State.Loading
            val end = Instant.now(); val start = end.minus(Duration.ofDays(days))
            val recs = mutableListOf<SleepSessionRecord>()
            var token: String? = null
            do {
                val r = c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end), pageToken = token))
                recs += r.records; token = r.pageToken
            } while (token != null)
            val nights = pickNights(recs.map { s ->
                SleepNight(s.startTime, s.endTime, s.stages.map { StageSpan(it.startTime, it.endTime, stage(it.stage)) },
                    s.metadata.dataOrigin.packageName)
            }, zone).map { SleepAnalysis.night(it, zone) }
            state.value = State.Ready(nights.reversed(), SleepAnalysis.week(nights, zone))
            val srcs = recs.map { it.metadata.dataOrigin.packageName }.distinct().joinToString(",").ifBlank { "none" }
            report(ctx, "ready: ${recs.size} sessions, ${nights.size} nights, newest ${nights.lastOrNull()?.date ?: "-"}, from $srcs")
            // Hours slept per morning feed the study plan (Burnout.bundlesFor / capMin) and sync to the PC app.
            val mins = nights.associate { it.date.toString() to it.asleepMin }
            val r0 = com.josephmiller.cloudroadmap.Store.load(ctx)
            if (mins.any { (k, v) -> r0.sleep[k] != v }) com.josephmiller.cloudroadmap.Store.edit(ctx) { r -> r.sleep.putAll(mins) }
        } catch (e: SecurityException) {
            state.value = State.NeedsPermission; report(ctx, "needs permission (security)")
        } catch (e: Exception) {
            state.value = State.Failed(e.message ?: "Health Connect didn't answer"); report(ctx, "failed: ${e.javaClass.simpleName}: ${e.message?.take(80)}")
        }
    }
}
