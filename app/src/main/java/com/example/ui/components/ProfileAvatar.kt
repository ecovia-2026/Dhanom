package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * A circular profile avatar. Shows the user's photo when a valid path is set,
 * otherwise falls back to the person's initials on a themed circle.
 */
@Composable
fun ProfileAvatar(
    photoPath: String,
    name: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) {
        name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("").ifBlank { "OM" }
    }
    val bitmap = remember(photoPath) {
        try {
            if (photoPath.isNotBlank()) {
                val file = File(photoPath)
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    val cropped = remember(bitmap) { bitmap?.centerCropSquare() }

    if (cropped != null) {
        Image(
            bitmap = cropped.asImageBitmap(),
            contentDescription = "Profile photo",
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Crops a bitmap to its centered square for a round avatar. */
private fun Bitmap.centerCropSquare(): Bitmap {
    val side = minOf(width, height)
    val x = (width - side) / 2
    val y = (height - side) / 2
    return try {
        Bitmap.createBitmap(this, x, y, side, side)
    } catch (e: Exception) {
        this
    }
}
