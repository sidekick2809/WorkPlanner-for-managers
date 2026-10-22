package com.workplanner.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette: olive grove + warm paper.
val Olive900 = Color(0xFF2C3719)
val Olive800 = Color(0xFF3B4A24)
val Olive700 = Color(0xFF4E5F2E)
val Olive600 = Color(0xFF647640)
val Olive500 = Color(0xFF7C8F51)
val Leaf500 = Color(0xFF7FA644)
val Leaf400 = Color(0xFF98BB5C)
val Sage300 = Color(0xFFC2CFA3)
val Sage200 = Color(0xFFDCE4C8)
val Sage100 = Color(0xFFEDF1E0)

val Cream = Color(0xFFF7F3E7)
val CreamDeep = Color(0xFFF0EBDC)
val Paper = Color(0xFFFFFCF4)
val Sand = Color(0xFFC9BFA5)
val Linen = Color(0xFFE7E2D1)
val OutlineSoft = Color(0xFFBDB7A1)

val Ink = Color(0xFF2A2A22)
val InkSoft = Color(0xFF5F604F)
val Clay = Color(0xFFB05B3B)

// Dark side of the grove.
val NightBg = Color(0xFF14170F)
val NightSurface = Color(0xFF1D2216)
val NightSurfaceHigh = Color(0xFF262D1D)
val NightOutline = Color(0xFF454D38)
val NightPrimary = Color(0xFFBED394)
val NightOnPrimary = Color(0xFF26330D)
val NightText = Color(0xFFECEFDF)
val NightTextSoft = Color(0xFFB9BFA9)

/** Avatar colours used for team members, in the olive / leaf family. */
data class AvatarColors(
    val container: Color,
    val content: Color
)

val AvatarPalette = listOf(
    AvatarColors(Color(0xFFDDE7C4), Color(0xFF41521F)),
    AvatarColors(Color(0xFFCFE0B7), Color(0xFF2F4A1C)),
    AvatarColors(Color(0xFFE7E2C2), Color(0xFF5C5220)),
    AvatarColors(Color(0xFFD6DEC8), Color(0xFF3B4A2A)),
    AvatarColors(Color(0xFFE3DCC2), Color(0xFF5A4A2A)),
    AvatarColors(Color(0xFFCBDBBF), Color(0xFF2E4526))
)

fun avatarColors(index: Int): AvatarColors = AvatarPalette[index.mod(AvatarPalette.size)]
