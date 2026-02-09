package com.example.loginfirebaseapp.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

sealed class AppThemeColors(
    val id: Int,
    val gradient: Brush,
    val textColor: Color,
    val cardBackground: Color
) {
    // TEMA 1
    object Theme1 : AppThemeColors(
        id = 1,
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2B676).copy(alpha = 0.8f),
                Color(0xFF715639).copy(alpha = 0.8f),
                Color(0xFF969696).copy(alpha = 0.8f)
            )
        ),
        textColor = Color.White,
        cardBackground = Color.White.copy(alpha = 0.1f)
    )

    // TEMA 2
    object Theme2 : AppThemeColors(
        id = 2,
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEF9A9A).copy(alpha = 0.7f),
                Color(0xFFB71C1C).copy(alpha = 0.8f),
                Color(0xFF424242).copy(alpha = 0.9f)
            )
        ),
        textColor = Color.White,
        cardBackground = Color.Black.copy(alpha = 0.2f)
    )

    // TEMA 3
    object Theme3 : AppThemeColors(
        id = 3,
        gradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF9FA8DA).copy(alpha = 0.7f),
                Color(0xFF1A237E).copy(alpha = 0.8f),
                Color(0xFF263238).copy(alpha = 0.9f)
            )
        ),
        textColor = Color.White,
        cardBackground = Color.Black.copy(alpha = 0.3f)
    )

    companion object {
        fun fromId(id: Int): AppThemeColors {
            return when (id) {
                2 -> Theme2
                3 -> Theme3
                else -> Theme1
            }
        }
    }
}