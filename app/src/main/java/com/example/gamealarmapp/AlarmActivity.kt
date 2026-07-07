package com.example.gamealarmapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamealarmapp.data.Alarm
import com.example.gamealarmapp.data.FileAlarmRepository
import com.example.gamealarmapp.data.GameDifficulty
import com.example.gamealarmapp.data.GameType
import com.example.gamealarmapp.theme.GameAlarmAppTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

class AlarmActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var onShakeDetected: (() -> Unit)? = null
    private var shouldListenToSensor = false

    private var alarmId: String = ""
    private var alarmLabel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getStringExtra("ALARM_ID") ?: ""
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"

        // Display over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Disable Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing: User must solve the game to close!
            }
        })

        // Setup Accelerometer for Shake Game
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            GameAlarmAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AlarmRingingScreen(
                        alarmId = alarmId,
                        alarmLabel = alarmLabel,
                        onDismiss = {
                            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_DISMISS
                                putExtra("ALARM_ID", alarmId)
                            }
                            startService(dismissIntent)
                            finish()
                        },
                        registerShakeListener = { callback ->
                            onShakeDetected = callback
                            shouldListenToSensor = true
                            sensorManager?.registerListener(
                                this,
                                accelerometer,
                                SensorManager.SENSOR_DELAY_UI
                            )
                        },
                        unregisterShakeListener = {
                            shouldListenToSensor = false
                            sensorManager?.unregisterListener(this)
                            onShakeDetected = null
                        }
                    )
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
            if (gForce > 5.5f) { // Shake detection threshold
                onShakeDetected?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        if (shouldListenToSensor) {
            sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: String,
    alarmLabel: String,
    onDismiss: () -> Unit,
    registerShakeListener: (() -> Unit) -> Unit,
    unregisterShakeListener: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    var gameStarted by remember { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        val repo = FileAlarmRepository(context)
        alarm = repo.getAlarm(alarmId)
    }

    // Modern Deep Plum to Hot Pink Gradient background
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1F0E2A), // Midnight Plum
            Color(0xFF2C0F35), // Dark Purple
            Color(0xFF13061A)  // Very Dark Violet
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp)
    ) {
        if (!gameStarted) {
            // Introductory Ringing UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .background(Color(0xFFFF2D85).copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, Color(0xFFFF2D85), CircleShape)
                    ) {
                        Text(
                            text = "⏰",
                            fontSize = 64.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = alarmLabel,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pulse Time Display
                    var currentTime by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        while (true) {
                            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            delay(1000)
                        }
                    }
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF2D85),
                            letterSpacing = 2.sp
                        )
                    )
                }

                // Call to action buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val currentAlarm = alarm
                    val gameText = when (currentAlarm?.gameType) {
                        GameType.MATH -> "Çöz ve Sustur"
                        GameType.MEMORY -> "Eşleştir ve Sustur"
                        GameType.SHAKE -> "Salla ve Sustur"
                        GameType.SEQUENCE -> "Sırala ve Sustur"
                        GameType.COLOR_MATCH -> "Cevapla ve Sustur"
                        GameType.ZIP -> "Çiz ve Sustur"
                        else -> "Sustur"
                    }

                    Button(
                        onClick = {
                            if (currentAlarm == null || currentAlarm.gameType == GameType.NONE) {
                                onDismiss()
                            } else {
                                gameStarted = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF2D85),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = gameText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Game UI is visible
            val activeAlarm = alarm
            if (activeAlarm != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Game Title Header
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Görevi Tamamla",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF2D85),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(30.dp))

                    when (activeAlarm.gameType) {
                        GameType.MATH -> {
                            MathGameScreen(
                                difficulty = activeAlarm.difficulty,
                                onGameSolved = onDismiss
                            )
                        }
                        GameType.MEMORY -> {
                            MemoryGameScreen(
                                difficulty = activeAlarm.difficulty,
                                onGameSolved = onDismiss
                            )
                        }
                        GameType.SHAKE -> {
                            ShakeGameScreen(
                                difficulty = activeAlarm.difficulty,
                                registerShakeListener = registerShakeListener,
                                unregisterShakeListener = unregisterShakeListener,
                                onGameSolved = onDismiss
                            )
                        }
                        GameType.SEQUENCE -> {
                            SequenceGameScreen(
                                difficulty = activeAlarm.difficulty,
                                onGameSolved = onDismiss
                            )
                        }
                        GameType.COLOR_MATCH -> {
                            ColorMatchGameScreen(
                                difficulty = activeAlarm.difficulty,
                                onGameSolved = onDismiss
                            )
                        }
                        GameType.ZIP -> {
                            ZipGameScreen(
                                difficulty = activeAlarm.difficulty,
                                onGameSolved = onDismiss
                            )
                        }
                        else -> {
                            LaunchedEffect(Unit) { onDismiss() }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- 1. MATH GAME SCREEN ----------------------
@Composable
fun MathGameScreen(difficulty: GameDifficulty, onGameSolved: () -> Unit) {
    var questionIndex by remember { mutableStateOf(1) }
    val totalQuestions = when (difficulty) {
        GameDifficulty.EASY -> 2
        GameDifficulty.MEDIUM -> 3
        GameDifficulty.HARD -> 4
    }

    var operand1 by remember { mutableStateOf(0) }
    var operand2 by remember { mutableStateOf(0) }
    var operator by remember { mutableStateOf("+") }
    var correctAnswer by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun generateQuestion() {
        val rand = Random(System.nanoTime())
        when (difficulty) {
            GameDifficulty.EASY -> {
                operand1 = rand.nextInt(2, 10)
                operand2 = rand.nextInt(2, 10)
                operator = if (rand.nextBoolean()) "+" else "-"
                correctAnswer = if (operator == "+") operand1 + operand2 else operand1 - operand2
            }
            GameDifficulty.MEDIUM -> {
                operand1 = rand.nextInt(10, 80)
                operand2 = rand.nextInt(5, 30)
                val opType = rand.nextInt(3)
                if (opType == 0) {
                    operator = "+"
                    correctAnswer = operand1 + operand2
                } else if (opType == 1) {
                    operator = "-"
                    correctAnswer = operand1 - operand2
                } else {
                    operand1 = rand.nextInt(2, 10)
                    operand2 = rand.nextInt(11, 20)
                    operator = "*"
                    correctAnswer = operand1 * operand2
                }
            }
            GameDifficulty.HARD -> {
                val opType = rand.nextInt(3)
                if (opType == 0) { // Large multiplication
                    operand1 = rand.nextInt(11, 30)
                    operand2 = rand.nextInt(11, 25)
                    operator = "*"
                    correctAnswer = operand1 * operand2
                } else if (opType == 1) { // Division
                    operand2 = rand.nextInt(5, 15)
                    correctAnswer = rand.nextInt(5, 20)
                    operand1 = operand2 * correctAnswer
                    operator = "/"
                } else { // Large compound
                    operand1 = rand.nextInt(50, 150)
                    operand2 = rand.nextInt(30, 99)
                    operator = "-"
                    correctAnswer = operand1 - operand2
                }
            }
        }
        userInput = ""
        isError = false
    }

    LaunchedEffect(questionIndex) {
        generateQuestion()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Progress Text
        Text(
            text = "Soru: $questionIndex / $totalQuestions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Equation Card (Glassmorphic look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$operand1 $operator $operand2 = ?",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Input Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(
                    2.dp,
                    if (isError) Color.Red else Color(0xFFFF2D85).copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (userInput.isEmpty()) "Cevabınızı girin..." else userInput,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (userInput.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pink Neon Custom Keyboard
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("-", "0", "Sil")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.width(300.dp)
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (key == "Sil") Color(0xFF3D1B48) else Color.White.copy(
                                        alpha = 0.05f
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable {
                                    isError = false
                                    if (key == "Sil") {
                                        if (userInput.isNotEmpty()) {
                                            userInput = userInput.dropLast(1)
                                        }
                                    } else {
                                        if (userInput.length < 7) {
                                            userInput += key
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = key,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Submit Button
            Button(
                onClick = {
                    val ans = userInput.toIntOrNull()
                    if (ans == correctAnswer) {
                        if (questionIndex >= totalQuestions) {
                            onGameSolved()
                        } else {
                            questionIndex++
                        }
                    } else {
                        isError = true
                        userInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2D85),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("GÖNDER", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------- 2. MEMORY GAME SCREEN ----------------------
class MemoryCard(
    val id: Int,
    val icon: String,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

@Composable
fun MemoryGameScreen(difficulty: GameDifficulty, onGameSolved: () -> Unit) {
    // Emojis for pairs
    val emojiList = listOf("🌸", "🍩", "🎨", "🚀", "🐱", "💎", "⭐", "🍕", "🎈", "🦊")
    val gridCols = 3
    val numPairs = when (difficulty) {
        GameDifficulty.EASY -> 4   // 8 cards (2x4)
        GameDifficulty.MEDIUM -> 6 // 12 cards (3x4)
        GameDifficulty.HARD -> 9   // 18 cards (3x6)
    }

    var cards by remember {
        mutableStateOf(listOf<MemoryCard>())
    }

    var selectedFirstIndex by remember { mutableStateOf<Int?>(null) }
    var selectedSecondIndex by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Initialize Card Layout
    fun initializeBoard() {
        val selectedEmojis = emojiList.shuffled().take(numPairs)
        val doubleList = (selectedEmojis + selectedEmojis).shuffled()
        cards = doubleList.mapIndexed { idx, emoji ->
            MemoryCard(id = idx, icon = emoji)
        }
        selectedFirstIndex = null
        selectedSecondIndex = null
        busy = false
    }

    LaunchedEffect(Unit) {
        initializeBoard()
    }

    // Match checking logic
    LaunchedEffect(selectedFirstIndex, selectedSecondIndex) {
        if (selectedFirstIndex != null && selectedSecondIndex != null) {
            busy = true
            delay(800) // Keep cards flipped for a moment
            val first = cards[selectedFirstIndex!!]
            val second = cards[selectedSecondIndex!!]

            val newCards = cards.toMutableList()
            if (first.icon == second.icon) {
                // Match
                newCards[selectedFirstIndex!!] = MemoryCard(first.id, first.icon, isFlipped = true, isMatched = true)
                newCards[selectedSecondIndex!!] = MemoryCard(second.id, second.icon, isFlipped = true, isMatched = true)
            } else {
                // Flip back
                newCards[selectedFirstIndex!!] = MemoryCard(first.id, first.icon, isFlipped = false)
                newCards[selectedSecondIndex!!] = MemoryCard(second.id, second.icon, isFlipped = false)
            }
            cards = newCards
            selectedFirstIndex = null
            selectedSecondIndex = null
            busy = false

            // Check if game solved
            if (cards.all { it.isMatched }) {
                onGameSolved()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Eşleşen Çiftler: ${cards.count { it.isMatched } / 2} / $numPairs",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cards.size) { index ->
                    val card = cards[index]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (card.isFlipped || card.isMatched) Color(0xFFFF2D85).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                1.5.dp,
                                if (card.isMatched) Color(0xFFFF2D85)
                                else if (card.isFlipped) Color(0xFFFFA2C5)
                                else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !busy && !card.isFlipped && !card.isMatched) {
                                val newCards = cards.toMutableList()
                                newCards[index] = MemoryCard(card.id, card.icon, isFlipped = true)
                                cards = newCards

                                if (selectedFirstIndex == null) {
                                    selectedFirstIndex = index
                                } else {
                                    selectedSecondIndex = index
                                }
                            }
                    ) {
                        if (card.isFlipped || card.isMatched) {
                            Text(text = card.icon, fontSize = 28.sp)
                        } else {
                            Text(text = "?", fontSize = 20.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reset Button
        IconButton(
            onClick = { initializeBoard() },
            modifier = Modifier
                .background(Color(0xFF3D1B48), CircleShape)
                .size(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart Game",
                tint = Color.White
            )
        }
    }
}

// ---------------------- 3. SHAKE GAME SCREEN ----------------------
@Composable
fun ShakeGameScreen(
    difficulty: GameDifficulty,
    registerShakeListener: (() -> Unit) -> Unit,
    unregisterShakeListener: () -> Unit,
    onGameSolved: () -> Unit
) {
    val targetShakes = when (difficulty) {
        GameDifficulty.EASY -> 30
        GameDifficulty.MEDIUM -> 60
        GameDifficulty.HARD -> 100
    }

    var currentShakes by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        registerShakeListener {
            if (currentShakes < targetShakes) {
                currentShakes++
                if (currentShakes >= targetShakes) {
                    onGameSolved()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            unregisterShakeListener()
        }
    }

    val progress = currentShakes.toFloat() / targetShakes.toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Telefonu Hızlıca Salla!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sallama Sayısı: $currentShakes / $targetShakes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Glowing Energy Progress Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Pulse outer glow circle
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(glowScale)
                    .background(Color(0xFFFF2D85).copy(alpha = 0.05f), CircleShape)
                    .border(2.dp, Color(0xFFFF2D85).copy(alpha = 0.15f), CircleShape)
            )

            // Circular progress ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFF2D85),
                strokeWidth = 14.dp,
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            // Inner Percentage Text / Battery Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GÜÇ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2D85),
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        // Shaking Action Instruction Card
        Box(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ Telefonu salladıkça güç barı dolacaktır. Sallamaya devam et!",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ---------------------- 4. SEQUENCE GAME SCREEN ----------------------
@Composable
fun SequenceGameScreen(difficulty: GameDifficulty, onGameSolved: () -> Unit) {
    val totalNumbers = when (difficulty) {
        GameDifficulty.EASY -> 6    // 2x3 grid
        GameDifficulty.MEDIUM -> 9  // 3x3 grid
        GameDifficulty.HARD -> 12   // 3x4 grid
    }

    var numbers by remember { mutableStateOf(listOf<Int>()) }
    var currentTarget by remember { mutableStateOf(1) }
    var isError by remember { mutableStateOf(false) }

    fun resetGame() {
        val rand = Random(System.nanoTime())
        numbers = (1..totalNumbers).toList().shuffled(rand)
        currentTarget = 1
        isError = false
    }

    LaunchedEffect(Unit) {
        resetGame()
    }

    LaunchedEffect(isError) {
        if (isError) {
            delay(600)
            isError = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Sayıları Sırayla Seç!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sıradaki: $currentTarget",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    1.dp,
                    if (isError) Color.Red else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(numbers.size) { index ->
                    val num = numbers[index]
                    val isTapped = num < currentTarget
                    val isWrongTap = isError && num == currentTarget

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isTapped -> Color(0xFFFF2D85).copy(alpha = 0.25f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                1.5.dp,
                                when {
                                    isTapped -> Color(0xFFFF2D85)
                                    isWrongTap -> Color.Red
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isTapped && !isError) {
                                if (num == currentTarget) {
                                    if (currentTarget == totalNumbers) {
                                        onGameSolved()
                                    } else {
                                        currentTarget++
                                    }
                                } else {
                                    isError = true
                                    currentTarget = 1 // Reset progress to 1
                                }
                            }
                    ) {
                        Text(
                            text = num.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTapped) Color(0xFFFF2D85) else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Button
        IconButton(
            onClick = { resetGame() },
            modifier = Modifier
                .background(Color(0xFF3D1B48), CircleShape)
                .size(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Yeniden Başlat",
                tint = Color.White
            )
        }
    }
}

// ---------------------- 5. COLOR MATCH (STROOP) GAME SCREEN ----------------------
@Composable
fun ColorMatchGameScreen(difficulty: GameDifficulty, onGameSolved: () -> Unit) {
    val totalRounds = when (difficulty) {
        GameDifficulty.EASY -> 3
        GameDifficulty.MEDIUM -> 4
        GameDifficulty.HARD -> 6
    }
    
    val colors = listOf(
        "Kırmızı" to Color(0xFFFF3B30),
        "Mavi" to Color(0xFF007AFF),
        "Yeşil" to Color(0xFF34C759),
        "Sarı" to Color(0xFFFFCC00),
        "Turuncu" to Color(0xFFFF9500),
        "Mor" to Color(0xFFAF52DE)
    )

    var currentRound by remember { mutableStateOf(1) }
    var currentWord by remember { mutableStateOf("") }
    var currentColorPair by remember { mutableStateOf(colors[0]) }
    var askAboutText by remember { mutableStateOf(true) }
    var options by remember { mutableStateOf(listOf<String>()) }
    var isError by remember { mutableStateOf(false) }

    fun generateNewQuestion() {
        val rand = Random(System.nanoTime())
        
        val wordPair = colors.random(rand)
        val colorPair = colors.random(rand)
        
        currentWord = wordPair.first
        currentColorPair = colorPair
        
        askAboutText = if (difficulty == GameDifficulty.EASY) {
            true
        } else {
            rand.nextBoolean()
        }
        
        val correctAnswer = if (askAboutText) colorPair.first else wordPair.first
        
        val numOptions = when (difficulty) {
            GameDifficulty.EASY -> 3
            GameDifficulty.MEDIUM -> 4
            GameDifficulty.HARD -> 5
        }
        
        val optionsList = mutableSetOf(correctAnswer)
        while (optionsList.size < numOptions) {
            optionsList.add(colors.random(rand).first)
        }
        options = optionsList.toList().shuffled(rand)
        isError = false
    }

    LaunchedEffect(currentRound) {
        generateNewQuestion()
    }

    LaunchedEffect(isError) {
        if (isError) {
            delay(600)
            isError = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Renk Testi (Stroop)",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tur: $currentRound / $totalRounds",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    1.dp,
                    if (isError) Color.Red else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (askAboutText) "YAZININ RENGİ NEDİR?" else "YAZIDA NE YAZIYOR?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentWord,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = currentColorPair.second
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val correctAnswer = if (askAboutText) currentColorPair.first else currentWord
                val isCorrect = option == correctAnswer
                
                Button(
                    onClick = {
                        if (isCorrect) {
                            if (currentRound >= totalRounds) {
                                onGameSolved()
                            } else {
                                currentRound++
                            }
                        } else {
                            isError = true
                            currentRound = 1 // Reset progress to 1
                            generateNewQuestion()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------- 6. ZIP (FLOW) GAME SCREEN ----------------------
@Composable
fun ZipGameScreen(difficulty: GameDifficulty, onGameSolved: () -> Unit) {
    val gridSize = when (difficulty) {
        GameDifficulty.EASY -> 4
        GameDifficulty.MEDIUM -> 5
        GameDifficulty.HARD -> 5
    }

    val numColors = when (difficulty) {
        GameDifficulty.EASY -> 3
        GameDifficulty.MEDIUM -> 3
        GameDifficulty.HARD -> 4
    }

    val flowColors = remember {
        listOf(
            Color(0xFFFF2D55), // Neon Red/Pink
            Color(0xFF4CD964), // Neon Green
            Color(0xFF007AFF), // Neon Blue
            Color(0xFFFF9500), // Neon Yellow/Orange
            Color(0xFF5856D6)  // Neon Purple
        )
    }

    var endpoints by remember { mutableStateOf(mapOf<Pair<Int, Int>, Int>()) }
    var userPaths by remember { mutableStateOf(mapOf<Int, List<Pair<Int, Int>>>()) }
    var activeColorIndex by remember { mutableStateOf<Int?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun generatePuzzle() {
        val rand = Random(System.nanoTime())
        var generatedPaths: List<List<Pair<Int, Int>>>? = null
        var attempts = 0
        
        while (generatedPaths == null && attempts < 300) {
            attempts++
            val visited = Array(gridSize) { BooleanArray(gridSize) }
            val paths = List(numColors) { mutableListOf<Pair<Int, Int>>() }
            
            // Place initial starting dots
            var failedToPlace = false
            for (i in 0 until numColors) {
                var r = rand.nextInt(gridSize)
                var c = rand.nextInt(gridSize)
                var retries = 0
                while (visited[r][c] && retries < 100) {
                    r = rand.nextInt(gridSize)
                    c = rand.nextInt(gridSize)
                    retries++
                }
                if (retries >= 100) {
                    failedToPlace = true
                    break
                }
                visited[r][c] = true
                paths[i].add(r to c)
            }
            
            if (failedToPlace) continue
            
            // Growth loop: Grow paths randomly to fill the grid
            var progress = true
            while (progress) {
                progress = false
                val indices = (0 until numColors).shuffled(rand)
                for (idx in indices) {
                    val path = paths[idx]
                    if (path.isEmpty()) continue
                    
                    val growFromEnd = rand.nextBoolean()
                    val cell = if (growFromEnd) path.last() else path.first()
                    
                    val neighbors = mutableListOf<Pair<Int, Int>>()
                    for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                        val nr = cell.first + dr
                        val nc = cell.second + dc
                        if (nr in 0 until gridSize && nc in 0 until gridSize && !visited[nr][nc]) {
                            neighbors.add(nr to nc)
                        }
                    }
                    
                    if (neighbors.isNotEmpty()) {
                        val nextCell = neighbors.random(rand)
                        visited[nextCell.first][nextCell.second] = true
                        if (growFromEnd) {
                            path.add(nextCell)
                        } else {
                            path.add(0, nextCell)
                        }
                        progress = true
                    }
                }
            }
            
            if (paths.all { it.size >= 2 }) {
                generatedPaths = paths
            }
        }

        val finalPaths = generatedPaths ?: run {
            val fallback = mutableListOf<List<Pair<Int, Int>>>()
            for (i in 0 until numColors) {
                val row = i * (gridSize / numColors)
                fallback.add(listOf(row to 0, row to gridSize - 1))
            }
            fallback
        }

        val map = mutableMapOf<Pair<Int, Int>, Int>()
        finalPaths.forEachIndexed { i, path ->
            map[path.first()] = i
            map[path.last()] = i
        }

        endpoints = map
        userPaths = emptyMap()
        activeColorIndex = null
        isError = false
    }

    LaunchedEffect(Unit) {
        generatePuzzle()
    }

    fun checkWinCondition() {
        val allConnected = (0 until numColors).all { idx ->
            val path = userPaths[idx] ?: return@all false
            if (path.size < 2) return@all false
            
            val first = path.first()
            val last = path.last()
            
            val isEndpoint1 = endpoints[first] == idx
            val isEndpoint2 = endpoints[last] == idx
            isEndpoint1 && isEndpoint2 && (first != last)
        }

        if (allConnected) {
            val totalCoveredCells = userPaths.values.sumOf { it.size }
            if (totalCoveredCells == gridSize * gridSize) {
                onGameSolved()
            }
        }
    }

    fun startDrawing(r: Int, c: Int) {
        val coord = r to c
        val colorIdx = endpoints[coord]
        if (colorIdx != null) {
            activeColorIndex = colorIdx
            userPaths = userPaths.toMutableMap().apply {
                put(colorIdx, listOf(coord))
            }
        } else {
            val existingColor = userPaths.entries.firstOrNull { it.value.contains(coord) }
            if (existingColor != null) {
                val path = existingColor.value
                val idx = path.indexOf(coord)
                activeColorIndex = existingColor.key
                userPaths = userPaths.toMutableMap().apply {
                    put(existingColor.key, path.subList(0, idx + 1))
                }
            }
        }
    }

    fun extendDrawing(r: Int, c: Int) {
        val activeIdx = activeColorIndex ?: return
        val path = userPaths[activeIdx] ?: return
        if (path.isEmpty()) return
        val last = path.last()
        val target = r to c
        if (last == target) return

        val isAdjacent = (kotlin.math.abs(last.first - r) == 1 && last.second == c) || 
                          (last.first == r && kotlin.math.abs(last.second - c) == 1)
        if (!isAdjacent) return

        if (path.size > 1 && path[path.size - 2] == target) {
            userPaths = userPaths.toMutableMap().apply {
                put(activeIdx, path.subList(0, path.size - 1))
            }
            return
        }

        if (path.contains(target)) {
            val idx = path.indexOf(target)
            userPaths = userPaths.toMutableMap().apply {
                put(activeIdx, path.subList(0, idx + 1))
            }
            return
        }

        val endpointColor = endpoints[target]
        if (endpointColor != null && endpointColor != activeIdx) {
            return
        }

        if (endpointColor == activeIdx) {
            userPaths = userPaths.toMutableMap().apply {
                put(activeIdx, path + target)
            }
            activeColorIndex = null
            checkWinCondition()
            return
        }

        val otherColorEntry = userPaths.entries.firstOrNull { it.key != activeIdx && it.value.contains(target) }
        if (otherColorEntry != null) {
            val otherIdx = otherColorEntry.key
            val otherPath = otherColorEntry.value
            val targetIdx = otherPath.indexOf(target)
            userPaths = userPaths.toMutableMap().apply {
                if (targetIdx == 0) {
                    remove(otherIdx)
                } else {
                    put(otherIdx, otherPath.subList(0, targetIdx))
                }
            }
        }

        userPaths = userPaths.toMutableMap().apply {
            put(activeIdx, path + target)
        }
    }

    val totalCells = gridSize * gridSize
    val totalCovered = userPaths.values.sumOf { it.size }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Çizgi Bağla",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Doluluk: $totalCovered / $totalCells",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF2D85)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .width(280.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(
                    1.5.dp,
                    if (isError) Color.Red else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
                .pointerInput(gridSize, endpoints, userPaths) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val cellW = size.width / gridSize
                            val cellH = size.height / gridSize
                            
                            var r = (down.position.y / cellH).toInt().coerceIn(0, gridSize - 1)
                            var c = (down.position.x / cellW).toInt().coerceIn(0, gridSize - 1)
                            startDrawing(r, c)
                            
                            while (true) {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                if (!anyPressed) break
                                
                                val change = event.changes.first()
                                val pos = change.position
                                r = (pos.y / cellH).toInt().coerceIn(0, gridSize - 1)
                                c = (pos.x / cellW).toInt().coerceIn(0, gridSize - 1)
                                extendDrawing(r, c)
                                change.consume()
                            }
                            
                            activeColorIndex = null
                            checkWinCondition()
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until gridSize) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0 until gridSize) {
                            val coord = r to c
                            val endpointColorIdx = endpoints[coord]
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                if (endpointColorIdx != null) {
                                    val color = flowColors[endpointColorIdx % flowColors.size]
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(2.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = this.size.width / gridSize
                val cellH = this.size.height / gridSize
                
                userPaths.forEach { (colorIdx, path) ->
                    if (path.size > 1) {
                        val linePath = androidx.compose.ui.graphics.Path()
                        val first = path[0]
                        linePath.moveTo(
                            (first.second + 0.5f) * cellW,
                            (first.first + 0.5f) * cellH
                        )
                        for (i in 1 until path.size) {
                            val curr = path[i]
                            linePath.lineTo(
                                (curr.second + 0.5f) * cellW,
                                (curr.first + 0.5f) * cellH
                            )
                        }
                        
                        drawPath(
                            path = linePath,
                            color = flowColors[colorIdx % flowColors.size],
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 12.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        IconButton(
            onClick = { generatePuzzle() },
            modifier = Modifier
                .background(Color(0xFF3D1B48), CircleShape)
                .size(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart",
                tint = Color.White
            )
        }
    }
}
