package com.josephmiller.cloudroadmap

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Home-screen widget: today's WTD tasks with tap-to-check. */
class WtdWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetBody() }
    }

    companion object {
        suspend fun refresh(context: Context) = runCatching { WtdWidget().updateAll(context) }
    }
}

class WtdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WtdWidget()
}

val TaskKey = ActionParameters.Key<String>("task")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val key = parameters[TaskKey] ?: return
        val today = LocalDate.now()
        Store.edit(context) { r ->
            val t = StudyPlan.tasksFor(today, r).firstOrNull { it.key == key } ?: return@edit
            if (t.stream == "alerts") r.logAlert(today, 1) else r.toggleTask(t, today)
        }
        WtdWidget().updateAll(context)
    }
}

private fun cp(c: Color) = ColorProvider(c)

private fun widgetColor(stream: String) = when (stream) {
    "tcm", "soc" -> Neon.Cyan
    "ld", "thm" -> Neon.Green
    "video" -> Color(0xFF4DA3FF)
    "pt" -> Color(0xFF2BD9C9)
    "bundle" -> Color(0xFF7AA2FF)
    "job" -> Color(0xFFFF8A65)
    "lab" -> Neon.Violet
    "course" -> Neon.Amber
    "alerts" -> Neon.Red
    "todo" -> Color(0xFFFF7AC6)
    else -> Neon.Muted
}

@Composable
private fun WidgetBody() {
    val ctx = LocalContext.current
    val r = Store.load(ctx)
    val today = LocalDate.now()
    val tasks = StudyPlan.tasksFor(today, r)
    val real = tasks.filter { it.stream != "rest" }
    val done = real.count { r.isDone(it) }
    Column(
        GlanceModifier.fillMaxSize().background(cp(Neon.Bg)).cornerRadius(20.dp).padding(14.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("WTD", style = TextStyle(color = cp(Neon.Text), fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(8.dp))
            Text(today.format(DateTimeFormatter.ofPattern("EEE MMM d")), style = TextStyle(color = cp(Neon.Muted), fontSize = 12.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (real.isEmpty()) "Off day" else if (done == real.size) "All done ✓" else "$done/${real.size}",
                style = TextStyle(color = cp(if (real.isNotEmpty() && done == real.size) Neon.Green else Neon.Cyan), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        (tasks.filter { !r.isDone(it) }.ifEmpty { tasks }).take(4).forEach { t ->
            val isDone = r.isDone(t)
            val c = widgetColor(t.stream)
            Row(
                GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(GlanceModifier.size(6.dp).background(cp(c)).cornerRadius(3.dp)) {}
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    when (t.stream) {
                        "alerts" -> "Alerts ${r.alertsInWeek(today)}/${Roadmap.ALERTS_PER_WEEK} · ${r.alertsLeftThisMonth(today)} left"
                        "bundle" -> "B" + t.where.substringAfter("Bundle ").substringBefore("/") + " · " + t.where.substringAfterLast(" · ")
                        else -> t.task
                    },
                    maxLines = 2,
                    style = TextStyle(color = cp(if (isDone) Neon.Muted else Neon.Text), fontSize = 13.sp),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                )
                if (t.stream != "rest") {
                    Spacer(GlanceModifier.width(8.dp))
                    Box(
                        GlanceModifier.size(32.dp).cornerRadius(16.dp)
                            .background(cp(if (isDone) c else c.copy(alpha = 0.15f)))
                            .clickable(
                                // Warm-up quiz and portfolio note need the app; everything else checks off right here.
                                if (t.key.endsWith(":warm") || t.key.endsWith(":note")) actionStartActivity<MainActivity>()
                                else actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskKey to t.key))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (t.stream == "alerts") "+1" else "✓",
                            style = TextStyle(color = cp(if (isDone) Neon.Bg else c), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
