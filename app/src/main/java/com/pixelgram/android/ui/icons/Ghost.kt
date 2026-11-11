package com.pixelgram.android.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val GhostIcon: ImageVector by lazy {
    ImageVector.Builder("Rounded.Ghost", 24.dp, 24.dp, 24f, 24f).addPath(
        addPathNodes("M12 2C7.6 2 4.5 5.3 4.5 9.6V20.3C4.5 21.4 5.7 22 6.6 21.4L8.1 20.4C8.5 20.1 9 20.1 9.4 20.4L11.2 21.7C11.7 22 12.3 22 12.8 21.7L14.6 20.4C15 20.1 15.5 20.1 15.9 20.4L17.4 21.4C18.3 22 19.5 21.4 19.5 20.3V9.6C19.5 5.3 16.4 2 12 2ZM9.3 12.2C8.4 12.2 7.8 11.4 7.8 10.4C7.8 9.4 8.4 8.6 9.3 8.6C10.1 8.6 10.8 9.4 10.8 10.4C10.8 11.4 10.1 12.2 9.3 12.2ZM14.7 12.2C13.9 12.2 13.2 11.4 13.2 10.4C13.2 9.4 13.9 8.6 14.7 8.6C15.6 8.6 16.2 9.4 16.2 10.4C16.2 11.4 15.6 12.2 14.7 12.2Z"),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.Black),
    ).build()
}
