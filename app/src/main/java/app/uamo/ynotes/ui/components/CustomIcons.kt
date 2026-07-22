package app.uamo.ynotes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val GooglePlay: ImageVector
        get() = ImageVector.Builder(
            name = "GooglePlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(22.018f, 13.298f)
                lineToRelative(-3.919f, 2.218f)
                lineToRelative(-3.515f, -3.493f)
                lineToRelative(3.543f, -3.521f)
                lineToRelative(3.891f, 2.202f)
                arcToRelative(1.49f, 1.49f, 0f, false, true, 0f, 2.594f)
                close()
                moveTo(1.337f, 0.924f)
                arcToRelative(1.486f, 1.486f, 0f, false, false, -0.112f, 0.568f)
                verticalLineToRelative(21.017f)
                curveToRelative(0f, 0.217f, 0.045f, 0.419f, 0.124f, 0.6f)
                lineToRelative(11.155f, -11.087f)
                lineTo(1.337f, 0.924f)
                close()
                moveTo(13.544f, 10.989f)
                lineToRelative(3.258f, -3.238f)
                lineTo(3.45f, 0.195f)
                arcToRelative(1.466f, 1.466f, 0f, false, false, -0.946f, -0.179f)
                lineToRelative(11.04f, 10.973f)
                close()
                moveTo(13.544f, 13.056f)
                lineToRelative(-11f, 10.933f)
                curveToRelative(0.298f, 0.036f, 0.612f, -0.016f, 0.906f, -0.183f)
                lineToRelative(13.324f, -7.54f)
                lineToRelative(-3.23f, -3.21f)
                close()
            }
        }.build()

    val Binance: ImageVector
        get() = ImageVector.Builder(
            name = "Binance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16.624f, 13.9202f)
                lineToRelative(2.7175f, 2.7154f)
                lineToRelative(-7.353f, 7.353f)
                lineToRelative(-7.353f, -7.352f)
                lineToRelative(2.7175f, -2.7164f)
                lineToRelative(4.6355f, 4.6595f)
                lineToRelative(4.6356f, -4.6595f)
                close()
                moveToRelative(4.6366f, -4.6366f)
                lineTo(24f, 12f)
                lineToRelative(-2.7154f, 2.7164f)
                lineTo(18.5682f, 12f)
                lineToRelative(2.6924f, -2.7164f)
                close()
                moveToRelative(-9.272f, 0.001f)
                lineToRelative(2.7163f, 2.6914f)
                lineToRelative(-2.7164f, 2.7174f)
                verticalLineToRelative(-0.001f)
                lineTo(9.2721f, 12f)
                lineToRelative(2.7164f, -2.7154f)
                close()
                moveToRelative(-9.2722f, -0.001f)
                lineTo(5.4088f, 12f)
                lineToRelative(-2.6914f, 2.6924f)
                lineTo(0f, 12f)
                lineToRelative(2.7164f, -2.7164f)
                close()
                moveTo(11.9885f, 0.0115f)
                lineToRelative(7.353f, 7.329f)
                lineToRelative(-2.7174f, 2.7154f)
                lineToRelative(-4.6356f, -4.6356f)
                lineToRelative(-4.6355f, 4.6595f)
                lineToRelative(-2.7174f, -2.7154f)
                lineToRelative(7.353f, -7.353f)
                close()
            }
        }.build()

    val PayPal: ImageVector
        get() = ImageVector.Builder(
            name = "PayPal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(15.607f, 4.653f)
                horizontalLineTo(8.941f)
                lineTo(6.645f, 19.251f)
                horizontalLineTo(1.82f)
                lineTo(4.862f, 0f)
                horizontalLineToRelative(7.995f)
                curveToRelative(3.754f, 0f, 6.375f, 2.294f, 6.473f, 5.513f)
                curveToRelative(-0.648f, -0.478f, -2.105f, -0.86f, -3.722f, -0.86f)
                moveToRelative(6.57f, 5.546f)
                curveToRelative(0f, 3.41f, -3.01f, 6.853f, -6.958f, 6.853f)
                horizontalLineToRelative(-2.493f)
                lineTo(11.595f, 24f)
                horizontalLineTo(6.74f)
                lineToRelative(1.845f, -11.538f)
                horizontalLineToRelative(3.592f)
                curveToRelative(4.208f, 0f, 7.346f, -3.634f, 7.153f, -6.949f)
                arcToRelative(5.24f, 5.24f, 0f, false, true, 2.848f, 4.686f)
                moveTo(9.653f, 5.546f)
                horizontalLineToRelative(6.408f)
                curveToRelative(0.907f, 0f, 1.942f, 0.222f, 2.363f, 0.541f)
                curveToRelative(-0.195f, 2.741f, -2.655f, 5.483f, -6.441f, 5.483f)
                horizontalLineTo(8.714f)
                close()
            }
        }.build()
}
