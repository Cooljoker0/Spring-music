package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkGlass
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight

/**
 * Applies RenderEffect blur cleanly on Android 12+ (API 31+) and graceful translucent layering on older APIs.
 */
fun Modifier.adaptiveBackdropBlur(radius: Dp = 28.dp): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius)
    } else {
        this
    }
}

/**
 * Primary Apple-inspired Midnight Liquid Glass container.
 * Combines translucent graphite fill, top-down specular light refraction, delicate depth shadow,
 * and crisp white edge highlight.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerBrush: Brush? = null,
    borderWidth: Dp = 0.85.dp,
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppCustomTheme.current
    val fillBrush = containerBrush ?: Brush.verticalGradient(
        colors = listOf(
            Color(0x9E262A34),
            theme.glassSurfaceColor,
            Color(0xB814171E)
        )
    )
    val rimBrush = FusionAmbience.themedRimBorderBrush(theme)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.72f),
                spotColor = Color.Black.copy(alpha = 0.82f)
            )
            .clip(shape)
            .background(fillBrush)
            .drawBehind {
                // Soft internal top glass refraction glint
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x2CFFFFFF),
                            Color(0x38FFFFFF),
                            Color(0x2CFFFFFF),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .border(width = borderWidth, brush = rimBrush, shape = shape),
        content = content
    )
}

/**
 * Compact translucent Liquid Glass pill for search controls, playback badges, and floating pills.
 */
@Composable
fun LiquidGlassPill(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val fillBrush = if (isActive) {
        Brush.verticalGradient(
            colors = listOf(
                SoftWhite,
                Color(0xFFE4E6EB)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x94252933),
                DarkGlass
            )
        )
    }
    val rimColor = if (isActive) SoftWhite else WhiteHighlight

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(fillBrush)
            .border(0.8.dp, rimColor, shape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Circular Liquid Glass icon control adhering to 48dp minimum touch target sizing.
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color = SoftWhite,
    isEmphasized: Boolean = false
) {
    val bgBrush = if (isEmphasized) {
        Brush.verticalGradient(listOf(SoftWhite, Color(0xFFE2E5EC)))
    } else {
        Brush.verticalGradient(listOf(Color(0x94262A34), Color(0x80181B22)))
    }
    val effectiveTint = if (isEmphasized) MidnightBlack else tint
    val borderBrush = if (isEmphasized) {
        Brush.verticalGradient(listOf(Color.White, Color(0x99FFFFFF)))
    } else {
        FusionAmbience.SpecularRimBrush
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgBrush)
                .border(0.8.dp, borderBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Spring Music editorial brand mark + wordmark for Home & About views.
 */
@Composable
fun BrandSpringLogo(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
    iconSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(10.dp))
                .border(0.8.dp, WhiteHighlight, RoundedCornerShape(10.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.spring_music_icon_1793437512160),
                contentDescription = "Spring Music Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (showWordmark) {
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Spring Music",
                    fontFamily = DisplayFontFamily,
                    color = SoftWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
                Text(
                    text = "LOSSLESS EDITION",
                    color = MutedGrey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Editorial section header separating title hierarchy cleanly across screens.
 */
@Composable
fun EditorialSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SoftWhite,
                fontFamily = DisplayFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MutedGrey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = actionLabel,
                    color = Silver,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
