package com.josephmiller.cloudroadmap.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import com.josephmiller.cloudroadmap.Roadmap
import com.josephmiller.cloudroadmap.Savings

/** Money goals: saved vs needed for the car and WGU, with the plan line and what's actually been saved. */
@Composable
fun MoneyGoalsCard(r: Roadmap, today: LocalDate, modifier: Modifier = Modifier, onLog: ((Int, Int) -> Unit)? = null) {
    var logging by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Neon.Panel)
            .border(1.dp, Neon.Line, RoundedCornerShape(18.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("MONEY GOALS", color = Neon.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
        GoalBlock("car", "Car down payment", "by Mar 1", Savings.carSaved(r), Savings.CAR_GOAL, Savings.carSeries(r, today), today, Neon.Amber, Savings.gap(r, "car", Savings.carSeries(r, today), today))
        GoalBlock("wgu", "WGU first term", "by Aug 1 (after Pell)", Savings.wguSaved(r), Savings.WGU_GOAL, Savings.wguSeries(r, today), today, Neon.Cyan, Savings.gap(r, "wgu", Savings.wguSeries(r, today), today))
        Text("Solid line: what you've saved. Dashed: the plan. Tick each payday on What To Do and enter what you moved.", color = Neon.Muted, fontSize = 12.sp)
        if (onLog != null) Text("+ Log money in or out", color = Neon.Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { logging = true })
    }
    if (logging && onLog != null) MoneyDialog("Log money (not a payday)", "Took money out? Use a minus sign, like -150.", true, 0, 0,
        onSave = { c, w -> onLog(c, w); logging = false }, onCancel = { logging = false })
}

/** Ask how much actually went where. Blank = 0. */
@Composable
fun MoneyDialog(title: String, note: String, showCar: Boolean, carDefault: Int, wguDefault: Int, onSave: (Int, Int) -> Unit, onCancel: () -> Unit) {
    var car by remember { mutableStateOf(if (carDefault == 0) "" else carDefault.toString()) }
    var wgu by remember { mutableStateOf(if (wguDefault == 0) "" else wguDefault.toString()) }
    fun num(s: String) = s.trim().replace("$", "").replace(",", "").toIntOrNull()
    val ok = (car.isBlank() || num(car) != null) && (wgu.isBlank() || num(wgu) != null)
    AlertDialog(
        onDismissRequest = onCancel, containerColor = Neon.Panel,
        title = { Text(title, color = Neon.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(note, color = Neon.Muted, fontSize = 13.sp)
                if (showCar) OutlinedTextField(car, { car = it }, label = { Text("Car fund (Texans CU)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(wgu, { wgu = it }, label = { Text("WGU savings") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(num(car) ?: 0, num(wgu) ?: 0) }, enabled = ok) { Text("Save", color = Neon.Green) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Neon.Muted) } },
    )
}

@Composable
private fun GoalBlock(which: String, name: String, by: String, saved: Int, goal: Int, series: List<Savings.Point>, today: LocalDate, color: Color, gap: Int) {
    val started = series.any { !it.day.isAfter(today) }
    val status = when {
        saved >= goal -> "Goal reached" to Neon.Green
        !started -> "Starts ${series.first().day.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}" to Neon.Muted
        gap >= 0 -> "On track" to Neon.Green
        else -> "Behind by \$${-gap}" to Neon.Amber
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Neon.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(by, color = Neon.Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("\$$saved of \$$goal", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(status.first, color = status.second, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Neon.Line)) {
            Box(Modifier.fillMaxWidth((saved.toFloat() / goal).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
        }
        Canvas(Modifier.fillMaxWidth().height(86.dp).padding(top = 6.dp)) {
            val n = series.size
            if (n < 2) return@Canvas
            val top = maxOf(goal, series.last().planned).toFloat()
            fun x(i: Int) = size.width * i / (n - 1)
            fun y(v: Int) = size.height - size.height * (v / top)
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            // goal line
            drawLine(Neon.Line, Offset(0f, y(goal)), Offset(size.width, y(goal)), strokeWidth = 2f, pathEffect = dash)
            // plan
            val plan = Path().apply { series.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.planned)) else lineTo(x(i), y(p.planned)) } }
            drawPath(plan, Neon.Muted, style = Stroke(width = 3f, pathEffect = dash))
            // actual
            val pts = series.mapIndexedNotNull { i, p -> p.actual?.let { Offset(x(i), y(it)) } }
            if (pts.isNotEmpty()) {
                val act = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
                drawPath(act, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawCircle(color, 7f, pts.last())
            }
        }
        Row {
            val f = java.time.format.DateTimeFormatter.ofPattern("MMM d")
            Text(series.first().day.format(f), color = Neon.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(series.last().day.format(f), color = Neon.Muted, fontSize = 11.sp)
        }
    }
}
