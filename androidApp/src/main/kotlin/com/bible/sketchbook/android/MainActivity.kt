package com.bible.sketchbook.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.bible.sketchbook.data.MemoryCardSummary
import com.bible.sketchbook.data.OfflineBibleSeeder
import com.bible.sketchbook.data.SketchbookRepository
import com.bible.sketchbook.data.SketchbookSummary
import com.bible.sketchbook.data.VerseSearchResult
import com.bible.sketchbook.db.BibleDatabase
import com.bible.sketchbook.memory.ReviewRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = AndroidSqliteDriver(
            schema = BibleDatabase.Schema,
            context = applicationContext,
            name = "bible-sketchbook.db",
        )
        val database = BibleDatabase(driver)
        setContent {
            BibleSketchbookApp(database)
        }
    }
}

@Composable
private fun BibleSketchbookApp(database: BibleDatabase) {
    val repository = remember(database) { SketchbookRepository(database) }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var retrySeed by remember { mutableIntStateOf(0) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var selectedSketchbook by remember { mutableStateOf<SketchbookSummary?>(null) }
    var selectedCard by remember { mutableStateOf<MemoryCardSummary?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddVerseDialog by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(database, retrySeed) {
        ready = false
        startupError = null
        try {
            withContext(Dispatchers.IO) {
                OfflineBibleSeeder(database).seedIfNeeded(System.currentTimeMillis())
            }
            ready = true
        } catch (exception: Exception) {
            startupError = exception.message ?: "Could not prepare the offline Bible database."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    fun performDatabaseAction(action: () -> Unit, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                onSuccess()
            } catch (exception: Exception) {
                actionMessage = exception.message ?: "The change could not be saved."
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            when {
                startupError != null -> StartupError(
                    message = startupError.orEmpty(),
                    onRetry = { retrySeed++ },
                )
                !ready -> LoadingScreen()
                selectedCard != null && selectedSketchbook != null -> ReviewCardScreen(
                    repository = repository,
                    card = selectedCard!!,
                    sketchbookTitle = selectedSketchbook!!.title,
                    onBack = { selectedCard = null },
                    onReview = { rating ->
                        val card = selectedCard!!
                        performDatabaseAction(action = {
                            repository.reviewCard(card, rating, System.currentTimeMillis())
                        }, onSuccess = {
                            actionMessage = "${rating.label} saved. The card schedule is updated."
                            selectedCard = null
                        })
                    },
                )
                selectedSketchbook != null -> SketchbookScreen(
                    repository = repository,
                    sketchbook = selectedSketchbook!!,
                    now = now,
                    message = actionMessage,
                    onDismissMessage = { actionMessage = null },
                    onBack = { selectedSketchbook = null },
                    onAddVerse = { showAddVerseDialog = true },
                    onSelectCard = { selectedCard = it },
                )
                else -> SketchbookShelf(
                    repository = repository,
                    now = now,
                    message = actionMessage,
                    onDismissMessage = { actionMessage = null },
                    onCreateSketchbook = { showCreateDialog = true },
                    onSelectSketchbook = { selectedSketchbook = it },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateSketchbookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                showCreateDialog = false
                performDatabaseAction(action = {
                    repository.createSketchbook(title, System.currentTimeMillis())
                })
            },
        )
    }

    if (showAddVerseDialog && selectedSketchbook != null) {
        AddVerseDialog(
            repository = repository,
            onDismiss = { showAddVerseDialog = false },
            onAdd = { verse ->
                val sketchbookId = selectedSketchbook!!.id
                showAddVerseDialog = false
                performDatabaseAction(action = {
                    repository.addVerseCard(sketchbookId, verse, System.currentTimeMillis())
                })
            },
        )
    }
}

private val Paper = Color(0xFFF5F0E6)
private val Ink = Color(0xFF3D4633)
private val MutedInk = Color(0xFF6D695E)
private val Sage = Color(0xFF6F7D52)
private const val MILLIS_PER_DAY = 86_400_000L

@Composable
private fun PaperBackdrop(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Paper)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 32.dp.toPx()
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = Color(0x176F7D52),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
                y += spacing
            }
            drawLine(
                color = Color(0x337F554D),
                start = androidx.compose.ui.geometry.Offset(32.dp.toPx(), 0f),
                end = androidx.compose.ui.geometry.Offset(32.dp.toPx(), size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        content()
    }
}

@Composable
private fun LoadingScreen() {
    PaperBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = Sage)
            Text(
                modifier = Modifier.padding(top = 18.dp),
                text = "Preparing your offline Bible for the first time…",
                color = Ink,
                fontSize = 16.sp,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "This only happens once on this device.",
                color = MutedInk,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun StartupError(message: String, onRetry: () -> Unit) {
    PaperBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Couldn’t open the sketchbook", color = Ink, fontSize = 22.sp)
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = message,
                color = MutedInk,
                fontSize = 14.sp,
            )
            Button(modifier = Modifier.padding(top = 20.dp), onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun SketchbookShelf(
    repository: SketchbookRepository,
    now: Long,
    message: String?,
    onDismissMessage: () -> Unit,
    onCreateSketchbook: () -> Unit,
    onSelectSketchbook: (SketchbookSummary) -> Unit,
) {
    val sketchbooks by remember(repository, now) {
        repository.observeSketchbooks(now)
    }.collectAsState(initial = emptyList())

    PaperBackdrop {
        Column(modifier = Modifier.fillMaxSize().padding(start = 52.dp, end = 20.dp, top = 28.dp)) {
            Text("Bible Sketchbook", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "A quiet place to gather, learn, and remember.",
                color = MutedInk,
                fontSize = 15.sp,
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "WORLD ENGLISH BIBLE · AVAILABLE OFFLINE",
                color = Sage,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            if (message != null) {
                MessageStrip(message, onDismissMessage)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("My sketchbooks", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCreateSketchbook) { Text("+ New") }
            }
            if (sketchbooks.isEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 20.dp),
                    text = "Create a sketchbook to begin collecting verses.",
                    color = MutedInk,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(sketchbooks, key = SketchbookSummary::id) { sketchbook ->
                        SketchbookCover(sketchbook, onClick = { onSelectSketchbook(sketchbook) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SketchbookCover(sketchbook: SketchbookSummary, onClick: () -> Unit) {
    val cover = if (sketchbook.coverStyle == "sage") Color(0xFFE2E7D7) else Color(0xFFFFFCF5)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cover),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sketchbook.title, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                sketchbook.description?.let {
                    Text(
                        modifier = Modifier.padding(top = 5.dp),
                        text = it,
                        color = MutedInk,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "Open sketchbook  →",
                    color = Sage,
                    fontSize = 13.sp,
                )
            }
            DueStamp(sketchbook.dueCardCount)
        }
    }
}

@Composable
private fun DueStamp(count: Long) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .border(1.5.dp, Color(0xFF8F665D), CircleShape)
            .padding(4.dp)
            .border(1.dp, Color(0x668F665D), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), color = Color(0xFF8F5147), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("DUE", color = Color(0xFF8F5147), fontSize = 8.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SketchbookScreen(
    repository: SketchbookRepository,
    sketchbook: SketchbookSummary,
    now: Long,
    message: String?,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    onAddVerse: () -> Unit,
    onSelectCard: (MemoryCardSummary) -> Unit,
) {
    val cards by remember(repository, sketchbook.id, now) {
        repository.observeCards(sketchbook.id, now)
    }.collectAsState(initial = emptyList())

    PaperBackdrop {
        Column(modifier = Modifier.fillMaxSize().padding(start = 52.dp, end = 20.dp, top = 18.dp)) {
            TextButton(onClick = onBack) { Text("←  All sketchbooks") }
            Text(sketchbook.title, color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "${cards.count(MemoryCardSummary::isDue)} cards due · ${cards.size} total",
                color = MutedInk,
                fontSize = 14.sp,
            )
            if (message != null) MessageStrip(message, onDismissMessage)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onAddVerse) { Text("＋ Add a verse") }
            }
            if (cards.isEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 18.dp),
                    text = "This sketchbook is ready for its first verse.",
                    color = MutedInk,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(cards, key = MemoryCardSummary::id) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectCard(card) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFDFEFCF6)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(card.reference, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        modifier = Modifier.padding(top = 4.dp),
                                        text = if (card.isDue) "Ready to review" else daysUntilReview(card.nextReviewAt, now),
                                        color = if (card.isDue) Color(0xFF9A574A) else MutedInk,
                                        fontSize = 13.sp,
                                    )
                                }
                                Text("Practice  →", color = Sage, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCardScreen(
    repository: SketchbookRepository,
    card: MemoryCardSummary,
    sketchbookTitle: String,
    onBack: () -> Unit,
    onReview: (ReviewRating) -> Unit,
) {
    val verses by remember(repository, card.id) {
        repository.observeVersesForCard(card.id)
    }.collectAsState(initial = emptyList())
    val fullText = verses.joinToString(" ") { it.text }
    var level by remember(card.id) { mutableIntStateOf(1) }
    var revealed by remember(card.id) { mutableStateOf(false) }

    PaperBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 52.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("←  Back to $sketchbookTitle") }
            Text(card.reference, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Choose a recall level", color = MutedInk, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf("Study", "Fade", "Initials", "Recall").forEachIndexed { index, label ->
                    val selected = level == index + 1
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Sage else Color(0xBFFFFCF5))
                            .clickable {
                                level = index + 1
                                revealed = false
                            }
                            .padding(vertical = 10.dp),
                        text = label,
                        color = if (selected) Color.White else Ink,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(enabled = level == 4) { revealed = !revealed },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = when (level) {
                            1 -> fullText
                            2 -> fadedText(fullText, initialsOnly = false)
                            3 -> fadedText(fullText, initialsOnly = true)
                            else -> if (revealed) fullText else "Tap here to reveal the verse"
                        },
                        color = Ink,
                        fontSize = 22.sp,
                        lineHeight = 34.sp,
                    )
                    Text(
                        modifier = Modifier.padding(top = 18.dp),
                        text = when (level) {
                            1 -> "Study the words and their meaning."
                            2 -> "Some words are left for you to remember."
                            3 -> "Use the first letters as a gentle hint."
                            else -> if (revealed) "Read it aloud, then rate your recall." else "Recall the verse before revealing."
                        },
                        color = MutedInk,
                        fontSize = 13.sp,
                    )
                }
            }
            Text("How well did you remember?", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReviewRating.entries.forEach { rating ->
                    val tint = when (rating) {
                        ReviewRating.AGAIN -> Color(0xFF9A574A)
                        ReviewRating.HARD -> Color(0xFF9B7740)
                        ReviewRating.GOOD -> Sage
                        ReviewRating.EASY -> Color(0xFF52776A)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onReview(rating) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(rating.label, color = tint, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSketchbookDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New sketchbook") },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("Sketchbook name") },
                placeholder = { Text("Peace & Rest") },
            )
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onCreate(title) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddVerseDialog(
    repository: SketchbookRepository,
    onDismiss: () -> Unit,
    onAdd: (VerseSearchResult) -> Unit,
) {
    var search by remember { mutableStateOf("John 3:16") }
    val results by remember(repository, search) {
        repository.observeVerseSearch(search)
    }.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find a verse") },
        text = {
            Column {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    label = { Text("Reference or words") },
                    placeholder = { Text("John 3:16 or peace") },
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(results, key = { "${it.bookId}-${it.chapter}-${it.verse}" }) { verse ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAdd(verse) }
                                .background(Color(0xFFFFFCF5))
                                .padding(12.dp),
                        ) {
                            Text(verse.reference, color = Sage, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                modifier = Modifier.padding(top = 4.dp),
                                text = verse.text,
                                color = Ink,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun MessageStrip(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE7EBDD))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, modifier = Modifier.weight(1f), color = Ink, fontSize = 12.sp)
        TextButton(onClick = onDismiss) { Text("×") }
    }
}

private fun fadedText(text: String, initialsOnly: Boolean): String {
    var wordIndex = 0
    return text.split(Regex("(\\s+)")).joinToString("") { token ->
        if (token.isBlank()) {
            token
        } else {
            val currentWord = wordIndex++
            if (!initialsOnly && currentWord % 2 == 0) {
                token.map { if (it.isLetterOrDigit()) '＿' else it }.joinToString("")
            } else if (initialsOnly) {
                var keptFirstLetter = false
                token.map { char ->
                    if (!char.isLetterOrDigit()) {
                        char
                    } else if (!keptFirstLetter) {
                        keptFirstLetter = true
                        char
                    } else {
                        '＿'
                    }
                }.joinToString("")
            } else {
                token
            }
        }
    }
}

private fun daysUntilReview(nextReviewAt: Long, now: Long): String {
    val days = ((nextReviewAt - now).coerceAtLeast(0) + MILLIS_PER_DAY - 1) / MILLIS_PER_DAY
    return "Next review in $days ${if (days == 1L) "day" else "days"}"
}

private val ReviewRating.label: String
    get() = when (this) {
        ReviewRating.AGAIN -> "Again"
        ReviewRating.HARD -> "Hard"
        ReviewRating.GOOD -> "Good"
        ReviewRating.EASY -> "Easy"
    }
