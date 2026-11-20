@file:OptIn(ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hair length. CROP is a short men's cut. */
enum class HairStyle(val label: String) { LONG("Long"), SHORT("Short"), CROP("Cropped") }

/** What the gardener wears on their head. */
enum class Headwear(val label: String) { NONE("Nothing"), BOBBLE("🧶 Bobble hat"), CAP("🧢 Baseball cap"), BAND("Hair band") }

/** How the little gardener looks. The default is the app creator, red hair and a woolly bobble hat. */
data class AvatarStyle(
    val hair: Long = 0xFFC2461F,
    val skin: Long = 0xFFF6D2B8,
    val hat: Long = 0xFF3F5E2F,
    val hatStripe: Long = 0xFFF7F1E3,
    val bobble: Long = 0xFFF7F1E3,
    val beard: Boolean = false,
    val glasses: Boolean = false,
    val hairStyle: HairStyle = HairStyle.CROP,
    val headwear: Headwear = Headwear.BOBBLE,
    val female: Boolean = false,
)

object AvatarOptions {
    const val BLONDE = 0xFFE9C46A
    const val BROWN = 0xFF5A3A22
    const val RED = 0xFFC2461F
    const val BLACK_HAIR = 0xFF1E1A17
    const val BLACK = 0xFF1C1C1C

    val hair = listOf(BLONDE, 0xFFC9A25A, BROWN, 0xFF8E3B1F, RED, 0xFFE07B39, BLACK_HAIR, 0xFFB8B2A7)
    val skin = listOf(0xFFF9DCC7, 0xFFF6D2B8, 0xFFE8B896, 0xFFC68B64, 0xFF8D5A3B, 0xFF5C3A24)
    /** Black first, as that's the usual choice. */
    val hat = listOf(BLACK, 0xFF3F5E2F, 0xFFB23A2E, 0xFF2D4A7A, 0xFFD9A441, 0xFF6B3F8F, 0xFF7A5537)
    val stripe = listOf(0xFFF7F1E3, 0xFFD9A441, 0xFFB23A2E, 0xFF6E9B4A)

    /** Ready-made gardeners to start from: 8 women, 3 men and the app's creator. */
    val presets: List<Pair<String, AvatarStyle>> = buildList {
        val women = listOf("Blonde" to BLONDE, "Brown" to BROWN, "Red" to RED, "Black" to BLACK_HAIR)
        val skins = listOf(0xFFF9DCC7, 0xFFE8B896, 0xFFF6D2B8, 0xFF8D5A3B, 0xFFF6D2B8, 0xFFC68B64, 0xFFF9DCC7, 0xFF5C3A24)
        var i = 0
        for ((name, colour) in women) for (style in listOf(HairStyle.LONG, HairStyle.SHORT)) {
            add("$name, ${style.label.lowercase()}" to AvatarStyle(hair = colour, skin = skins[i++], hairStyle = style, female = true, headwear = Headwear.NONE))
        }
        add("Brown, short" to AvatarStyle(hair = BROWN, skin = 0xFFE8B896, hairStyle = HairStyle.CROP, headwear = Headwear.NONE))
        add("Black, short" to AvatarStyle(hair = BLACK_HAIR, skin = 0xFF8D5A3B, hairStyle = HairStyle.CROP, headwear = Headwear.NONE))
        add("Fair, short, beard" to AvatarStyle(hair = BLONDE, skin = 0xFFF9DCC7, hairStyle = HairStyle.CROP, beard = true, headwear = Headwear.NONE))
        add("The creator" to AvatarStyle())
    }
}

/** Saved on the phone, shared across screens. */
object AvatarStore {
    private val _style = MutableStateFlow(AvatarStyle())
    val style: StateFlow<AvatarStyle> = _style.asStateFlow()
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        val p = ctx.getSharedPreferences("avatar", Context.MODE_PRIVATE)
        val d = AvatarStyle()
        _style.value = AvatarStyle(
            hair = p.getLong("hair", d.hair), skin = p.getLong("skin", d.skin), hat = p.getLong("hat", d.hat),
            hatStripe = p.getLong("stripe", d.hatStripe), bobble = p.getLong("bobble", d.bobble),
            beard = p.getBoolean("beard", d.beard), glasses = p.getBoolean("glasses", d.glasses),
            hairStyle = runCatching { HairStyle.valueOf(p.getString("hairStyle", null)!!) }.getOrDefault(d.hairStyle),
            headwear = runCatching { Headwear.valueOf(p.getString("headwear", null)!!) }.getOrDefault(d.headwear),
            female = p.getBoolean("female", d.female),
        )
        loaded = true
    }

    fun save(ctx: Context, s: AvatarStyle) {
        ctx.getSharedPreferences("avatar", Context.MODE_PRIVATE).edit()
            .putLong("hair", s.hair).putLong("skin", s.skin).putLong("hat", s.hat)
            .putLong("stripe", s.hatStripe).putLong("bobble", s.bobble)
            .putBoolean("beard", s.beard).putBoolean("glasses", s.glasses)
            .putString("hairStyle", s.hairStyle.name).putString("headwear", s.headwear.name)
            .putBoolean("female", s.female).apply()
        _style.value = s
    }
}

private fun Color.darker(f: Float = 0.8f) = copy(red = red * f, green = green * f, blue = blue * f)

/** The gardener character, drawn in code so it can be any colour and stays sharp at any size. */
@Composable
fun GardenerAvatar(style: AvatarStyle, size: Dp = 56.dp, background: Color = Color(0xFFDCEBC8), modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).clip(CircleShape).background(background)) {
        val w = this.size.width
        val h = this.size.height
        val hair = Color(style.hair)
        val skin = Color(style.skin)
        val hat = Color(style.hat)
        val cx = w / 2f

        // shoulders in a green jumper
        drawOval(Color(0xFF6E9B4A), topLeft = Offset(w * 0.12f, h * 0.80f), size = Size(w * 0.76f, h * 0.5f))

        // hair behind the head
        when (style.hairStyle) {
            HairStyle.LONG -> {
                drawRoundRect(hair, topLeft = Offset(w * 0.18f, h * 0.30f), size = Size(w * 0.64f, h * 0.62f), cornerRadius = CornerRadius(w * 0.22f))
            }
            HairStyle.SHORT -> {
                drawOval(hair, topLeft = Offset(w * 0.20f, h * 0.27f), size = Size(w * 0.60f, h * 0.50f))
            }
            HairStyle.CROP -> {
                drawOval(hair, topLeft = Offset(w * 0.25f, h * 0.27f), size = Size(w * 0.50f, h * 0.34f))
            }
        }

        // face
        drawOval(skin, topLeft = Offset(w * 0.27f, h * 0.32f), size = Size(w * 0.46f, h * 0.48f))

        // beard
        if (style.beard) {
            val beard = Path().apply {
                moveTo(w * 0.28f, h * 0.56f)
                quadraticBezierTo(cx, h * 0.98f, w * 0.72f, h * 0.56f)
                quadraticBezierTo(cx, h * 0.72f, w * 0.28f, h * 0.56f)
                close()
            }
            drawPath(beard, hair)
        }

        // eyes, cheeks and smile
        val eye = Color(0xFF2B2A22)
        drawCircle(eye, radius = w * 0.028f, center = Offset(w * 0.41f, h * 0.53f))
        drawCircle(eye, radius = w * 0.028f, center = Offset(w * 0.59f, h * 0.53f))
        if (style.female) {
            // a few lashes
            drawLine(eye, Offset(w * 0.385f, h * 0.505f), Offset(w * 0.37f, h * 0.49f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            drawLine(eye, Offset(w * 0.615f, h * 0.505f), Offset(w * 0.63f, h * 0.49f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0x55E86A5C), radius = w * 0.05f, center = Offset(w * 0.36f, h * 0.62f))
        drawCircle(Color(0x55E86A5C), radius = w * 0.05f, center = Offset(w * 0.64f, h * 0.62f))
        drawArc(
            if (style.female) Color(0xFFB8474A) else eye, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.56f), size = Size(w * 0.16f, h * 0.10f),
            style = Stroke(width = w * 0.025f, cap = StrokeCap.Round),
        )
        if (style.glasses) {
            val frame = Stroke(width = w * 0.02f)
            drawCircle(eye, radius = w * 0.07f, center = Offset(w * 0.41f, h * 0.53f), style = frame)
            drawCircle(eye, radius = w * 0.07f, center = Offset(w * 0.59f, h * 0.53f), style = frame)
            drawLine(eye, Offset(w * 0.48f, h * 0.53f), Offset(w * 0.52f, h * 0.53f), strokeWidth = w * 0.02f)
        }

        // hair on top of the head (hidden under a bobble hat or cap, apart from a few strands)
        fun topHair() {
            when (style.hairStyle) {
                HairStyle.CROP -> {
                    val top = Path().apply {
                        moveTo(w * 0.27f, h * 0.46f)
                        cubicTo(w * 0.24f, h * 0.20f, w * 0.76f, h * 0.20f, w * 0.73f, h * 0.46f)
                        quadraticBezierTo(w * 0.62f, h * 0.36f, w * 0.40f, h * 0.38f)
                        quadraticBezierTo(w * 0.32f, h * 0.40f, w * 0.27f, h * 0.46f)
                        close()
                    }
                    drawPath(top, hair)
                }
                else -> {
                    // a soft side-swept fringe
                    val top = Path().apply {
                        moveTo(w * 0.25f, h * 0.50f)
                        cubicTo(w * 0.20f, h * 0.18f, w * 0.80f, h * 0.18f, w * 0.75f, h * 0.50f)
                        quadraticBezierTo(w * 0.70f, h * 0.38f, w * 0.52f, h * 0.36f)
                        quadraticBezierTo(w * 0.40f, h * 0.44f, w * 0.25f, h * 0.50f)
                        close()
                    }
                    drawPath(top, hair)
                }
            }
        }

        when (style.headwear) {
            Headwear.NONE -> topHair()
            Headwear.BAND -> {
                topHair()
                drawArc(
                    hat, startAngle = 195f, sweepAngle = 150f, useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.22f), size = Size(w * 0.52f, h * 0.36f),
                    style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
                )
            }
            Headwear.BOBBLE -> {
                // a few strands of fringe poking out under the hat
                drawOval(hair, topLeft = Offset(w * 0.26f, h * 0.36f), size = Size(w * 0.16f, h * 0.12f))
                drawOval(hair, topLeft = Offset(w * 0.58f, h * 0.36f), size = Size(w * 0.16f, h * 0.12f))
                val dome = Path().apply {
                    moveTo(w * 0.22f, h * 0.42f)
                    cubicTo(w * 0.22f, h * 0.08f, w * 0.78f, h * 0.08f, w * 0.78f, h * 0.42f)
                    close()
                }
                drawPath(dome, hat)
                drawRect(Color(style.hatStripe), topLeft = Offset(w * 0.24f, h * 0.27f), size = Size(w * 0.52f, h * 0.05f))
                drawRoundRect(
                    hat.darker(0.85f), topLeft = Offset(w * 0.19f, h * 0.38f), size = Size(w * 0.62f, h * 0.10f),
                    cornerRadius = CornerRadius(w * 0.05f),
                )
                var x = w * 0.23f
                while (x < w * 0.78f) {
                    drawLine(Color.White.copy(alpha = 0.15f), Offset(x, h * 0.39f), Offset(x, h * 0.47f), strokeWidth = w * 0.012f)
                    x += w * 0.045f
                }
                val bobble = Color(style.bobble)
                drawCircle(bobble, radius = w * 0.09f, center = Offset(cx, h * 0.13f))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    drawCircle(
                        bobble, radius = w * 0.035f,
                        center = Offset(cx + (kotlin.math.cos(a) * w * 0.08f).toFloat(), h * 0.13f + (kotlin.math.sin(a) * w * 0.08f).toFloat()),
                    )
                }
            }
            Headwear.CAP -> {
                drawOval(hair, topLeft = Offset(w * 0.25f, h * 0.38f), size = Size(w * 0.12f, h * 0.10f))
                drawOval(hair, topLeft = Offset(w * 0.63f, h * 0.38f), size = Size(w * 0.12f, h * 0.10f))
                val crown = Path().apply {
                    moveTo(w * 0.24f, h * 0.43f)
                    cubicTo(w * 0.24f, h * 0.14f, w * 0.76f, h * 0.14f, w * 0.76f, h * 0.43f)
                    close()
                }
                drawPath(crown, hat)
                // seam and button
                drawLine(Color.White.copy(alpha = 0.18f), Offset(cx, h * 0.21f), Offset(cx, h * 0.42f), strokeWidth = w * 0.012f)
                drawCircle(hat.darker(0.7f), radius = w * 0.025f, center = Offset(cx, h * 0.21f))
                // peak curving down towards you
                drawArc(
                    hat.darker(0.7f), startAngle = 0f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(w * 0.27f, h * 0.35f), size = Size(w * 0.46f, h * 0.13f),
                )
            }
        }
    }
}

@Composable
private fun Swatches(title: String, colours: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colours.forEach { c ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onPick(c) }
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Pick a ready-made gardener, then change hair, skin and what they wear on their head. */
@Composable
fun AvatarEditorDialog(current: AvatarStyle, onDismiss: () -> Unit, onSave: (AvatarStyle) -> Unit) {
    var s by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your gardener") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { GardenerAvatar(s, size = 120.dp) }
                Spacer(Modifier.height(12.dp))

                Text("Pick a gardener", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarOptions.presets.forEach { (name, p) ->
                        // Keep whatever hat or band they've already chosen, except for the creator who has his own.
                        val look = if (name == "The creator") p else p.copy(headwear = s.headwear, hat = s.hat)
                        val chosen = s.hair == look.hair && s.hairStyle == look.hairStyle && s.female == look.female && s.beard == look.beard && s.skin == look.skin
                        Box(
                            Modifier.clip(CircleShape)
                                .border(if (chosen) 3.dp else 0.dp, if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { s = look }
                        ) { GardenerAvatar(look, size = 52.dp) }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Text("On their head", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Headwear.entries.forEach { hw ->
                        FilterChip(
                            selected = s.headwear == hw,
                            onClick = {
                                // Hats and bands start off black, which is the usual choice.
                                s = if (hw != Headwear.NONE && s.headwear == Headwear.NONE) s.copy(headwear = hw, hat = AvatarOptions.BLACK)
                                else s.copy(headwear = hw)
                            },
                            label = { Text(hw.label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (s.headwear != Headwear.NONE) {
                    Swatches(if (s.headwear == Headwear.BAND) "Band colour" else "Hat colour", AvatarOptions.hat, s.hat) { s = s.copy(hat = it) }
                }
                if (s.headwear == Headwear.BOBBLE) {
                    Swatches("Stripe and bobble", AvatarOptions.stripe, s.hatStripe) { s = s.copy(hatStripe = it, bobble = it) }
                }

                Text("Hair length", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HairStyle.entries.forEach { hs ->
                        FilterChip(selected = s.hairStyle == hs, onClick = { s = s.copy(hairStyle = hs) }, label = { Text(hs.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Swatches("Hair colour", AvatarOptions.hair, s.hair) { s = s.copy(hair = it) }
                Swatches("Skin", AvatarOptions.skin, s.skin) { s = s.copy(skin = it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Beard", Modifier.weight(1f)); Switch(s.beard, { s = s.copy(beard = it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Glasses", Modifier.weight(1f)); Switch(s.glasses, { s = s.copy(glasses = it) })
                }
                TextButton(onClick = { s = AvatarStyle() }) { Text("Back to the original") }
            }
        },
        confirmButton = { Button(onClick = { onSave(s); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
