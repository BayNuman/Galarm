package com.example.gamealarmapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamealarmapp.data.Alarm
import com.example.gamealarmapp.data.AndroidAlarmScheduler
import com.example.gamealarmapp.data.GameType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: AlarmViewModel,
    onAddAlarm: () -> Unit,
    onEditAlarm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    var nextAlarmText by remember { mutableStateOf("") }
    LaunchedEffect(alarms) {
        while (true) {
            nextAlarmText = getNextAlarmRemainingText(alarms)
            kotlinx.coroutines.delay(30000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF13061A)) // Dark Plum background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Title
            Text(
                text = "galarm",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF2D85) // Glowing fuchsia
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown Card (Glassmorphic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Kalan Süre",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nextAlarmText,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm List
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .border(
                                border = BorderStroke(1.dp, Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF2D85).copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(28.dp))
                            .padding(vertical = 36.dp, horizontal = 20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color(0xFFFF2D85).copy(alpha = 0.1f), CircleShape)
                                .border(1.5.dp, Color(0xFFFF2D85).copy(alpha = 0.3f), CircleShape)
                        ) {
                            Text(
                                text = "⏰",
                                fontSize = 32.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Alarm Bulunmuyor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Güne zinde ve odaklanmış başlamak için yeni bir alarm kurun.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            onClick = { onEditAlarm(alarm.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddAlarm,
            containerColor = Color(0xFFFF2D85),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Alarm Ekle",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val cardBg = if (alarm.isEnabled) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2E113C), // Glowing Plum
                Color(0xFF1B0722)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Alarmı Sil?", color = Color.White) },
            text = { Text("Bu alarmı kalıcı olarak silmek istiyor musunuz?", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Evet", color = Color(0xFFFF2D85))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal", color = Color.White)
                }
            },
            containerColor = Color(0xFF1B0722),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (alarm.isEnabled) Color(0xFFFF2D85).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        Box(
            modifier = Modifier
                .background(cardBg)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Alarm Information
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.getFormattedTime(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${alarm.label} • ${alarm.getRepeatDaysText()}",
                        fontSize = 14.sp,
                        color = if (alarm.isEnabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Game badge
                    if (alarm.gameType != GameType.NONE) {
                        val gameEmoji = when (alarm.gameType) {
                            GameType.MATH -> "🔢 Matematik"
                            GameType.MEMORY -> "🎴 Hafıza Kartı"
                            GameType.SHAKE -> "📱 Telefon Sallama"
                            GameType.SEQUENCE -> "🔢 Sayı Sıralama"
                            GameType.COLOR_MATCH -> "🎨 Renk Eşleme"
                            GameType.ZIP -> "⚡ Zip"
                            else -> ""
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (alarm.isEnabled) Color(0xFFFF2D85).copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$gameEmoji (${when(alarm.difficulty) {
                                    com.example.gamealarmapp.data.GameDifficulty.EASY -> "Kolay"
                                    com.example.gamealarmapp.data.GameDifficulty.MEDIUM -> "Orta"
                                    com.example.gamealarmapp.data.GameDifficulty.HARD -> "Zor"
                                }})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alarm.isEnabled) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Switch and Delete buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }

                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF2D85),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

fun getNextAlarmRemainingText(alarms: List<Alarm>): String {
    val enabledAlarms = alarms.filter { it.isEnabled }
    if (enabledAlarms.isEmpty()) return "Aktif alarm yok"

    val now = System.currentTimeMillis()
    var minTrigger = Long.MAX_VALUE

    for (alarm in enabledAlarms) {
        val trigger = AndroidAlarmScheduler.calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)
        if (trigger in now until minTrigger) {
            minTrigger = trigger
        }
    }

    if (minTrigger == Long.MAX_VALUE) return "Aktif alarm yok"

    val diff = minTrigger - now
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff / (1000 * 60)) % 60

    return when {
        hours > 0 -> "$hours sa $minutes dk kaldı"
        minutes > 0 -> "$minutes dk kaldı"
        else -> "1 dk'dan az kaldı"
    }
}
