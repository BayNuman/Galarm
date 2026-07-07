package com.example.gamealarmapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamealarmapp.data.Alarm
import com.example.gamealarmapp.data.GameDifficulty
import com.example.gamealarmapp.data.GameType

@Composable
fun AddEditAlarmScreen(
    alarmId: String?,
    viewModel: AlarmViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    val existingAlarm = remember(alarmId, alarms) {
        alarms.find { it.id == alarmId }
    }

    // State holders
    var hour by remember { mutableStateOf(existingAlarm?.hour ?: 8) }
    var minute by remember { mutableStateOf(existingAlarm?.minute ?: 0) }
    var daysOfWeek by remember { mutableStateOf(existingAlarm?.daysOfWeek ?: emptySet()) }
    var label by remember { mutableStateOf(existingAlarm?.label ?: "Alarm") }
    var gameType by remember { mutableStateOf(existingAlarm?.gameType ?: GameType.NONE) }
    var difficulty by remember { mutableStateOf(existingAlarm?.difficulty ?: GameDifficulty.MEDIUM) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF13061A)) // Dark Plum background
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("İptal", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
            }
            Text(
                text = if (existingAlarm == null) "Yeni Alarm" else "Düzenle",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(
                onClick = {
                    val finalAlarm = existingAlarm?.copy(
                        hour = hour,
                        minute = minute,
                        daysOfWeek = daysOfWeek,
                        label = label,
                        gameType = gameType,
                        difficulty = difficulty
                    ) ?: Alarm(
                        hour = hour,
                        minute = minute,
                        daysOfWeek = daysOfWeek,
                        label = label,
                        gameType = gameType,
                        difficulty = difficulty
                    )
                    viewModel.saveAlarm(finalAlarm)
                    onBack()
                }
            ) {
                Text("Kaydet", color = Color(0xFFFF2D85), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. CHEVRON TIME PICKER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeDigitColumn(
                value = hour,
                onValueChange = { hour = (it + 24) % 24 },
                maxValue = 23
            )
            Text(
                text = ":",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            TimeDigitColumn(
                value = minute,
                onValueChange = { minute = (it + 60) % 60 },
                maxValue = 59
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. REPEAT DAYS SELECTOR
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "Günler",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                for (dayIdx in 1..7) {
                    val isSelected = daysOfWeek.contains(dayIdx)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .clickable {
                                daysOfWeek = if (isSelected) {
                                    daysOfWeek - dayIdx
                                } else {
                                    daysOfWeek + dayIdx
                                }
                            }
                    ) {
                        Text(
                            text = dayNames[dayIdx - 1].take(1),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 3. LABEL INPUT
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "Etiket",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF2D85),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. GAME TYPE SELECTOR
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "Görev",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameCard(
                        title = "Yok",
                        emoji = "⏰",
                        isSelected = gameType == GameType.NONE,
                        onClick = { gameType = GameType.NONE },
                        modifier = Modifier.weight(1f)
                    )
                    GameCard(
                        title = "Matematik",
                        emoji = "🔢",
                        isSelected = gameType == GameType.MATH,
                        onClick = { gameType = GameType.MATH },
                        modifier = Modifier.weight(1f)
                    )
                    GameCard(
                        title = "Hafıza",
                        emoji = "🎴",
                        isSelected = gameType == GameType.MEMORY,
                        onClick = { gameType = GameType.MEMORY },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameCard(
                        title = "Sallama",
                        emoji = "📱",
                        isSelected = gameType == GameType.SHAKE,
                        onClick = { gameType = GameType.SHAKE },
                        modifier = Modifier.weight(1f)
                    )
                    GameCard(
                        title = "Sayı Sırala",
                        emoji = "🔢",
                        isSelected = gameType == GameType.SEQUENCE,
                        onClick = { gameType = GameType.SEQUENCE },
                        modifier = Modifier.weight(1f)
                    )
                    GameCard(
                        title = "Renk Testi",
                        emoji = "🎨",
                        isSelected = gameType == GameType.COLOR_MATCH,
                        onClick = { gameType = GameType.COLOR_MATCH },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameCard(
                        title = "Zip Oyunu",
                        emoji = "⚡",
                        isSelected = gameType == GameType.ZIP,
                        onClick = { gameType = GameType.ZIP },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 5. DIFFICULTY SELECTOR (Conditional)
        if (gameType != GameType.NONE) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Zorluk",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val difficulties = listOf(
                        Triple(GameDifficulty.EASY, "Kolay", "1/3"),
                        Triple(GameDifficulty.MEDIUM, "Orta", "2/3"),
                        Triple(GameDifficulty.HARD, "Zor", "3/3")
                    )
                    for ((diff, name, scale) in difficulties) {
                        val isSelected = difficulty == diff
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFFFF2D85).copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.03f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { difficulty = diff }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun TimeDigitColumn(
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Artır",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.5.dp, Color(0xFFFF2D85).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = { onValueChange(value - 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Azalt",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun GameCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFFFF2D85).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.03f)
            )
            .border(
                1.5.dp,
                if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFFF2D85) else Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
