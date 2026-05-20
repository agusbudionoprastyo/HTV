package com.dafamsemarang.dhtv

import androidx.compose.ui.graphics.Color

class UiColor {
}

val HomeText = Color(0xFF000000).copy(alpha = .6f)
val HomeBox = Color(0xFFFFFFFF).copy(alpha = .2f)
val HomeRipple = Color(0xFF000000)
val HomeIcon = Color(0xFF000000).copy(alpha = .6f)

val HeaderText = Color(0xFFFFFFFF)
val HeaderIcon = Color(0xFFFFFFFF)

val FooterText = Color(0xFFFFFFFF).copy(alpha = 0.5f)
val FooterBox = Color(0xFFFFFFFF).copy(alpha = 1f)
val FooterRipple = Color(0xFFFFFFFF)
val FooterIcon = Color(0xFFFFFFFF)

val HotelInfoText = Color( 0xFF000000).copy(alpha = .6f)
val HotelInfoBox = Color(0xFFFFFFFF).copy(alpha = .2f)
val HotelInfoRipple = Color(0xFF000000)
val HotelInfoIcon = Color( 0xFF000000).copy(alpha = .6f)

val GlassTextDrawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
    width = 1f,
    miter = 10f,
    cap = androidx.compose.ui.graphics.StrokeCap.Round,
    join = androidx.compose.ui.graphics.StrokeJoin.Round
)