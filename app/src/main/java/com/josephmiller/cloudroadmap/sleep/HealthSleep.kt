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
        if (HealthConnectClient.getSdkStatus(ctx) != HealthConnectClient.SDK_AVAILABLE) { state.value = State.NotInstalled; return }
        try {
            val c = HealthConnectClient.getOrCreate(ctx)
            if (!c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)) { state.value = State.NeedsPermission; return }
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
        } catch (e: SecurityException) {
            state.value = State.NeedsPermission
        } catch (e: Exception) {
            state.value = State.Failed(e.message ?: "Health Connect didn't answer")
        }
    }
}
