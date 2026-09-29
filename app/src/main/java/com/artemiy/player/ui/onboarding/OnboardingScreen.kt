package com.artemiy.player.ui.onboarding

import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.PlayerStyle
import com.artemiy.player.ui.components.LoadingMark
import com.artemiy.player.ui.components.pressScale
import com.artemiy.player.ui.mood.TalkingBurst
import com.artemiy.player.ui.settings.FolderChips
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.ThemeMode
import kotlinx.coroutines.delay

/** What the welcome screens need from the rest of the app. */
class OnboardingState(
    val musicAllowed: Boolean,
    val libraryLoaded: Boolean,
    val songCount: Int,
    val availableFolders: List<String>,
    val selectedFolders: Set<String>,
    val themeMode: ThemeMode,
    val playerStyle: PlayerStyle,
    val uiStyle: com.artemiy.player.ui.theme.UiStyle = com.artemiy.player.ui.theme.UiStyle.CLASSIC,
    /** Android 13+ asks for notifications separately; below that there's nothing to ask. */
    val notificationsNeedAsking: Boolean,
)

class OnboardingActions(
    val requestMusic: () -> Unit,
    val loadFolders: () -> Unit,
    val toggleFolder: (String) -> Unit,
    val setThemeMode: (ThemeMode) -> Unit,
    val setPlayerStyle: (PlayerStyle) -> Unit,
    val setUiStyle: (com.artemiy.player.ui.theme.UiStyle) -> Unit = {},
    val requestNotifications: () -> Unit,
    val finish: () -> Unit,
)

private enum class Step { Hello, Music, Folders, Look, Notifications, Done }

/**
 * The welcome screens, shown once on first launch (and again from "О приложении"): the app's
 * burst introduces itself line by line — a tap shows the next line — then a few short steps:
 * access to the music, which folders, theme and player style, notifications, and a last word.
 * Going back (the back gesture) returns to the step before.
 */
@Composable
fun OnboardingScreen(state: OnboardingState, actions: OnboardingActions) {
    val steps = remember(state.notificationsNeedAsking) {
        Step.entries.filter { it != Step.Notifications || state.notificationsNeedAsking }
    }
    var index by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    fun go(to: Int) {
        forward = to > index
        index = to.coerceIn(0, steps.lastIndex)
    }
    fun next() {
        var to = index + 1
        // Folders only make sense once the music can be read.
        if (steps.getOrNull(to) == Step.Folders && !state.musicAllowed) to++
        if (to > steps.lastIndex) actions.finish() else go(to)
    }
    BackHandler(enabled = index > 0) {
        var to = index - 1
        if (steps.getOrNull(to) == Step.Folders && !state.musicAllowed) to--
        go(to)
    }

    // Leaving at the end: the burst says goodbye its own way (flying at the viewer, or bursting
    // into sparks — picked at random each time) while the screen fades to the app underneath.
    val exit = remember { androidx.compose.animation.core.Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val farewell = remember { if (kotlin.random.Random.nextBoolean()) Farewell.FlyAtViewer else Farewell.Shatter }
    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        exit.animateTo(1f, tween(1150, easing = androidx.compose.animation.core.LinearEasing))
        actions.finish()
    }
    val background = PlayerColors.Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val fade = ((exit.value - 0.3f) / 0.55f).coerceIn(0f, 1f)
                drawRect(background.copy(alpha = 1f - fade))
            }
            // Nothing underneath is reachable while this is up.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        AnimatedContent(
            targetState = steps[index],
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(380)) { it / 4 * dir } + fadeIn(tween(380)))
                    .togetherWith(slideOutHorizontally(tween(320)) { -it / 4 * dir } + fadeOut(tween(260)))
            },
            label = "onboardingStep",
        ) { step ->
            when (step) {
                Step.Hello -> TalkingStep(lines = HELLO_LINES.map { stringResource(it) }, lastButton = stringResource(R.string.onb_start), onLastTap = ::next)
                Step.Music -> StepPage(
                    title = stringResource(R.string.onb_music_title),
                    text = stringResource(R.string.onb_music_text),
                    button = stringResource(if (state.musicAllowed) R.string.onb_next else R.string.onb_allow),
                    onButton = { if (state.musicAllowed) next() else actions.requestMusic() },
                    secondary = if (state.musicAllowed) null else stringResource(R.string.onb_later),
                    onSecondary = ::next,
                    progress = index to steps.size,
                ) {
                    if (state.musicAllowed) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
                            if (!state.libraryLoaded) {
                                LoadingMark(color = PlayerColors.AccentStandalone, modifier = Modifier.size(44.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "%,d".format(state.songCount).replace(',', ' '),
                                        color = PlayerColors.AccentStandalone,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Text(text = pluralStringResource(R.plurals.songs_in_library, state.songCount), color = PlayerColors.TextSecondary, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
                Step.Folders -> {
                    LaunchedEffect(Unit) { actions.loadFolders() }
                    StepPage(
                        title = stringResource(R.string.onb_folders_title),
                        text = stringResource(R.string.onb_folders_text),
                        button = stringResource(R.string.onb_next),
                        onButton = ::next,
                        progress = index to steps.size,
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        if (state.availableFolders.isEmpty()) {
                            LoadingMark(color = PlayerColors.AccentStandalone, modifier = Modifier.size(36.dp).align(Alignment.CenterHorizontally))
                        } else {
                            FolderChips(
                                availableFolders = state.availableFolders,
                                selectedFolders = state.selectedFolders,
                                onToggleFolder = actions.toggleFolder,
                            )
                        }
                    }
                }
                Step.Look -> StepPage(
                    title = stringResource(R.string.onb_look_title),
                    text = stringResource(R.string.onb_look_text),
                    button = stringResource(R.string.onb_next),
                    onButton = ::next,
                    progress = index to steps.size,
                ) {
                    ChoiceLabel(stringResource(R.string.onb_theme))
                    Choices(
                        options = listOf(ThemeMode.SYSTEM to stringResource(R.string.theme_system), ThemeMode.LIGHT to stringResource(R.string.theme_light), ThemeMode.DARK to stringResource(R.string.theme_dark)),
                        selected = state.themeMode,
                        onSelect = actions.setThemeMode,
                    )
                    ChoiceLabel(stringResource(R.string.set_ui_style))
                    Choices(
                        options = listOf(
                            com.artemiy.player.ui.theme.UiStyle.CLASSIC to stringResource(R.string.ui_style_classic),
                            com.artemiy.player.ui.theme.UiStyle.EXPRESSIVE to stringResource(R.string.ui_style_expressive),
                        ),
                        selected = state.uiStyle,
                        onSelect = actions.setUiStyle,
                    )
                    Text(
                        text = stringResource(
                            if (state.uiStyle == com.artemiy.player.ui.theme.UiStyle.CLASSIC) R.string.ui_style_classic_note else R.string.ui_style_expressive_note,
                        ),
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    ChoiceLabel(stringResource(R.string.onb_player))
                    Choices(
                        options = listOf(PlayerStyle.CLASSIC to stringResource(R.string.player_classic), PlayerStyle.EXPRESSIVE to stringResource(R.string.player_expressive)),
                        selected = state.playerStyle,
                        onSelect = actions.setPlayerStyle,
                    )
                    Text(
                        text = if (state.playerStyle == PlayerStyle.CLASSIC) {
                            stringResource(R.string.onb_player_classic_hint)
                        } else {
                            stringResource(R.string.onb_player_expressive_hint)
                        },
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Step.Notifications -> StepPage(
                    title = stringResource(R.string.onb_notif_title),
                    text = stringResource(R.string.onb_notif_text),
                    button = stringResource(R.string.onb_allow),
                    onButton = { actions.requestNotifications(); next() },
                    secondary = stringResource(R.string.onb_not_now),
                    onSecondary = ::next,
                    progress = index to steps.size,
                )
                Step.Done -> TalkingStep(
                    lines = FAREWELL_LINES.map { stringResource(it) },
                    lastHint = stringResource(R.string.onb_tap_to_start),
                    onLastTap = { leaving = true },
                    exit = { exit.value },
                    farewell = farewell,
                    below = { line, lineDone ->
                        // "…вот за этой кнопкой": the settings button itself, as on Home.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = line == SETTINGS_LINE && lineDone,
                            enter = fadeIn(tween(300)) + androidx.compose.animation.scaleIn(tween(420), initialScale = 0.6f),
                            exit = fadeOut(tween(200)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 18.dp)
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(PlayerColors.Surface),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.Icon(
                                    com.artemiy.player.ui.icons.AppIcons.Settings,
                                    contentDescription = null,
                                    tint = PlayerColors.TextPrimary,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

/** The burst's lines, one per tap. */
private val HELLO_LINES = listOf(R.string.hello_1, R.string.hello_2, R.string.hello_3, R.string.hello_4, R.string.hello_5)

/** The burst's goodbye at the end. */
private val FAREWELL_LINES = listOf(R.string.bye_1, R.string.bye_2, R.string.bye_3, R.string.bye_4)
private const val SETTINGS_LINE = 2

private enum class Farewell { FlyAtViewer, Shatter }

/**
 * The burst talking: each line types itself out while the burst "talks" (spins faster, rays
 * wagging); a tap anywhere finishes the line at once, or moves on to the next one. After the last
 * line: either a button ([lastButton]) or, with a tap, [onLastTap]. [below] adds something under
 * a line; [exit] (0..1) plays the burst's [farewell] and fades the words out.
 */
@Composable
private fun TalkingStep(
    lines: List<String>,
    onLastTap: () -> Unit,
    lastButton: String? = null,
    lastHint: String? = null,
    exit: () -> Float = { 0f },
    farewell: Farewell = Farewell.FlyAtViewer,
    below: @Composable (line: Int, lineDone: Boolean) -> Unit = { _, _ -> },
) {
    var line by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }
    val text = lines[line]
    val typing = shown < text.length
    LaunchedEffect(line) {
        shown = 0
        delay(350)
        while (shown < text.length) {
            val ch = text[shown]
            shown++
            // A beat after punctuation, like a breath.
            delay(if (ch in ".,!—") 160 else 34)
        }
    }
    val last = line == lines.lastIndex
    var hintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(line) { delay(2600); hintVisible = true }
    var tapped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                when {
                    typing -> shown = text.length
                    // The new line starts empty right away — otherwise, for a frame, it showed as
                    // many letters of itself as the previous line had.
                    !last -> { shown = 0; line++; hintVisible = false }
                    lastButton == null && !tapped -> { tapped = true; onLastTap() }
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        FarewellBurst(speaking = typing, exit = exit, farewell = farewell, modifier = Modifier.size(150.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
                .padding(top = 36.dp)
                .graphicsLayer { alpha = (1f - exit() * 4f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = text.take(shown),
                color = PlayerColors.TextPrimary,
                fontSize = if (line == 0) 34.sp else 22.sp,
                lineHeight = if (line == 0) 40.sp else 29.sp,
                fontWeight = if (line == 0) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            below(line, !typing)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp).graphicsLayer { alpha = (1f - exit() * 4f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            if (lastButton != null) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = last && !typing,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(420)) { it / 2 },
                ) {
                    PrimaryButton(lastButton, onClick = onLastTap)
                }
            }
            val hint = if (last) lastHint else stringResource(R.string.onb_tap_to_continue)
            androidx.compose.animation.AnimatedVisibility(
                visible = hintVisible && hint != null && !(last && lastButton != null) && !tapped,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(200)),
            ) {
                Text(text = hint ?: "", color = PlayerColors.TextTertiary, fontSize = 13.sp)
            }
        }
    }
}

/**
 * The talking burst, plus its way of leaving as [exit] runs 0 → 1: flying straight at the viewer
 * (growing huge, turning, fading), or bursting into sparks that fly apart and fade.
 */
@Composable
private fun FarewellBurst(speaking: Boolean, exit: () -> Float, farewell: Farewell, modifier: Modifier = Modifier) {
    val color = PlayerColors.AccentStandalone
    val sparks = remember { List(48) { Spark(kotlin.random.Random(it * 7919 + 17)) } }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        TalkingBurst(
            color = color,
            speaking = speaking,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = exit()
                    if (e <= 0f) return@graphicsLayer
                    when (farewell) {
                        Farewell.FlyAtViewer -> {
                            val grow = 1f + 13f * e * e
                            scaleX = grow
                            scaleY = grow
                            rotationZ = 110f * e
                            alpha = (1f - (e - 0.4f) / 0.4f).coerceIn(0f, 1f)
                        }
                        // Gone at once — the sparks take its place.
                        Farewell.Shatter -> alpha = (1f - e / 0.06f).coerceIn(0f, 1f)
                    }
                },
        )
        if (farewell == Farewell.Shatter) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val e = exit()
                if (e <= 0f) return@Canvas
                val r = size.minDimension / 2
                val fade = (1f - e / 0.9f).coerceIn(0f, 1f)
                sparks.forEach { spark ->
                    val travel = r * (spark.start + spark.speed * e * 3.2f)
                    val x = center.x + kotlin.math.cos(spark.angle) * travel
                    val y = center.y + kotlin.math.sin(spark.angle) * travel + r * 1.6f * e * e
                    val side = r * spark.size
                    rotate(spark.spin * e * 360f + spark.angle * 57f, androidx.compose.ui.geometry.Offset(x, y)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(x, y - side)
                            lineTo(x + side * 0.87f, y + side * 0.5f)
                            lineTo(x - side * 0.87f, y + side * 0.5f)
                            close()
                        }
                        drawPath(path, color.copy(alpha = color.alpha * fade))
                    }
                }
            }
        }
    }
}

/** One spark of the shattered burst: where it starts, which way and how fast it flies, how it
 * spins, how big it is. */
private class Spark(random: kotlin.random.Random) {
    val angle = random.nextFloat() * 2f * kotlin.math.PI.toFloat()
    val start = random.nextFloat() * 0.55f
    val speed = 0.5f + random.nextFloat() * 1.3f
    val spin = random.nextFloat() * 4f - 2f
    val size = 0.04f + random.nextFloat() * 0.09f
}

/** One of the steps after the hello: progress dots, a big title and a line or two, whatever the
 * step shows, and its button(s) at the bottom. */
@Composable
private fun StepPage(
    title: String,
    text: String,
    button: String,
    onButton: () -> Unit,
    progress: Pair<Int, Int>,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        ProgressDots(current = progress.first, total = progress.second, modifier = Modifier.padding(top = 20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 44.dp),
        ) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = text,
                color = PlayerColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            content()
            // Room between the last of it and the button.
            Spacer(modifier = Modifier.height(28.dp))
        }
        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = if (secondary == null) 20.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(button, onClick = onButton)
            if (secondary != null) Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                run {
                    Text(
                        text = secondary,
                        color = PlayerColors.TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSecondary)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(CircleShape)
            .background(PlayerColors.Accent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = PlayerColors.OnAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // The hello screen isn't counted — the dots are for the steps after it.
        for (i in 1 until total) {
            val done = i <= current
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (i == current) 22.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (done) PlayerColors.AccentStandalone else PlayerColors.TextTertiary.copy(alpha = 0.5f)),
            )
        }
    }
}

@Composable
private fun ChoiceLabel(text: String) {
    Text(
        text = text,
        color = PlayerColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val chosen = value == selected
            Text(
                text = label,
                color = if (chosen) PlayerColors.OnAccent else PlayerColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (chosen) PlayerColors.Accent else PlayerColors.Surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(value) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
