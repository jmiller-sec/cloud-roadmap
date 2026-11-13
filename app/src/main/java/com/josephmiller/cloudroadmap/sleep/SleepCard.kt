package com.josephmiller.cloudroadmap.sleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Last night's sleep from the Galaxy Watch (via Health Connect), shown on the WTD tab.
 * Display only for now; the study load does not change yet.
 */
@Composable
fun SleepCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by HealthSleep.state.collectAsState()
    val ask = rememberLauncherForActivityResult(HealthSleep.permissionContract()) { scope.launch { HealthSleep.refresh(ctx) } }
    LaunchedEffect(Unit) { HealthSleep.refresh(ctx) }
    val mono = FontFamily.Monospace
    val cyan = Color(0xFF00E5FF); val muted = Color(0xFF7A8DA3); val text = Color(0xFFE6F1FF); val violet = Color(0xFFB388FF)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0C1622))
        .border(1.dp, Color(0xFF1C3146), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text("LAST NIGHT · GALAXY WATCH", color = violet, fontFamily = mono, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        when (val s = st) {
            is HealthSleep.State.Loading -> Text("Reading your sleep…", color = muted, fontSize = 13.sp)
            is HealthSleep.State.NotInstalled -> Text("Health Connect isn't set up on this phone, so sleep can't be read yet.", color = muted, fontSize = 13.sp)
            is HealthSleep.State.Failed -> Text("Couldn't read sleep: ${s.message}", color = muted, fontSize = 13.sp)
            is HealthSleep.State.NeedsPermission -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Let Cloud Roadmap read your watch's sleep (read only).", color = text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(cyan).clickable { ask.launch(HealthSleep.PERMISSIONS) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Connect", color = Color(0xFF00121A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            is HealthSleep.State.Ready -> {
                val n = s.nights.firstOrNull()
                if (n == null) Text("No sleep recorded yet. Wear the watch to bed tonight.", color = muted, fontSize = 13.sp)
                else {
                    val tf = DateTimeFormatter.ofPattern("h:mm a"); val z = ZoneId.systemDefault()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(SleepAnalysis.hm(n.asleepMin), color = text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text("score ${n.score}/100", color = cyan, fontFamily = mono, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text("${tf.format(n.bedtime.atZone(z))} → ${tf.format(n.wake.atZone(z))}" +
                        (if (n.hasStages) " · deep ${SleepAnalysis.hm(n.deepMin)} · REM ${SleepAnalysis.hm(n.remMin)}" else "") +
                        " · woke ${n.wakeUps}×", color = muted, fontSize = 12.sp, fontFamily = mono)
                }
            }
        }
    }
}

/** Shown by Android when you ask why Cloud Roadmap reads sleep. */
class SleepPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF060B12)).systemBarsPadding().padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Why Cloud Roadmap reads your sleep", color = Color(0xFFE6F1FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("It reads the sleep your Galaxy Watch saves to Health Connect and shows last night on the WTD tab. " +
                    "Later it will use it to size each day's study load.", color = Color(0xFFE6F1FF), fontSize = 15.sp)
                Text("Read only. Sleep data stays on this phone and is never synced to GitHub.", color = Color(0xFFE6F1FF), fontSize = 15.sp)
            }
        }
    }
}
