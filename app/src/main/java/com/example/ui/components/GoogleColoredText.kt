package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// The iconic Google brand colors
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleYellow = Color(0xFFFBBC05)
val GoogleGreen = Color(0xFF34A853)

val GOOGLE_PALETTE = listOf(GoogleBlue, GoogleRed, GoogleYellow, GoogleGreen)

/**
 * Renders text with letters colored sequentially in the iconic Google color palette.
 */
@Composable
fun GoogleColoredText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    letterSpacing: TextUnit = 0.5.sp,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val annotatedString = buildAnnotatedString {
        var colorIdx = 0
        for (char in text) {
            if (char.isWhitespace()) {
                append(char)
            } else {
                val color = GOOGLE_PALETTE[colorIdx % GOOGLE_PALETTE.size]
                colorIdx++
                withStyle(
                    style = SpanStyle(
                        color = color,
                        fontWeight = fontWeight,
                        fontSize = fontSize,
                        letterSpacing = letterSpacing
                    )
                ) {
                    append(char)
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}
