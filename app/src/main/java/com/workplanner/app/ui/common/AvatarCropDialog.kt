package com.workplanner.app.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SOURCE_SIZE = 1440
private const val OUTPUT_SIZE = 512

/**
 * Full-screen-friendly dialog that lets the user pan and zoom a gallery image
 * and returns a circular PNG bitmap.
 */
@Composable
fun AvatarCropDialog(
    source: Uri,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(source) { loadScaledBitmap(context, source, MAX_SOURCE_SIZE) }

    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Fotoğraf açılamadı") },
            text = { Text("Seçilen görsel okunamadı. Lütfen başka bir fotoğraf deneyin.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Tamam") }
            }
        )
        return
    }

    val previewSize = 220.dp
    val previewPx = with(LocalDensity.current) { previewSize.toPx() }

    var zoom by remember(source) { mutableFloatStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }

    fun clamped(candidate: Offset, currentZoom: Float): Offset {
        val maxShift = ((currentZoom - 1f) * previewPx) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxShift, maxShift),
            y = candidate.y.coerceIn(-maxShift, maxShift)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profil fotoğrafı",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Yuvarlak alanda görünecek kısmı seç. Sürükleyerek kaydır, iki parmakla yakınlaştır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .size(previewSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                zoom = nextZoom
                                offset = clamped(offset + pan, nextZoom)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoom,
                                scaleY = zoom,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Vazgeç") }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            val cropped = bitmap.cropToCircle(previewPx, zoom, offset)
                                ?: return@Button
                            onCropped(cropped)
                        }
                    ) {
                        Text("Fotoğrafı kullan")
                    }
                }
            }
        }
    }
}

/** Photographs are decoded at a bounded size so large gallery shots stay safe. */
fun loadScaledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = max(info.size.width, info.size.height)
            if (largest > maxSize) {
                val ratio = maxSize.toFloat() / largest
                decoder.setTargetSize(
                    (info.size.width * ratio).roundToInt().coerceAtLeast(1),
                    (info.size.height * ratio).roundToInt().coerceAtLeast(1)
                )
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > maxSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}.getOrNull()

/**
 * Maps the on-screen circle back to the source bitmap: the preview shows the
 * image with ContentScale.Crop, so one preview pixel equals one source pixel
 * times [previewPx] / source size * zoom.
 */
private fun Bitmap.cropToCircle(
    previewPx: Float,
    zoom: Float,
    offset: Offset
): Bitmap? {
    if (width <= 0 || height <= 0) return null

    val baseScale = max(previewPx / width, previewPx / height)
    val scale = baseScale * zoom
    val side = (previewPx / scale).coerceAtMost(minOf(width, height).toFloat())
    val half = side / 2f

    val centerX = width / 2f - offset.x / scale
    val centerY = height / 2f - offset.y / scale
    val left = (centerX - half).coerceIn(0f, width - side)
    val top = (centerY - half).coerceIn(0f, height - side)

    return runCatching {
        val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // Clip to the circle so the source pixels keep their own colours. Using
        // a PorterDuff SRC_IN mask here would paint the whole avatar black.
        canvas.clipPath(
            Path().apply {
                addCircle(
                    OUTPUT_SIZE / 2f,
                    OUTPUT_SIZE / 2f,
                    OUTPUT_SIZE / 2f,
                    Path.Direction.CW
                )
            }
        )
        canvas.drawBitmap(
            this,
            android.graphics.Rect(
                left.toInt(),
                top.toInt(),
                (left + side).toInt(),
                (top + side).toInt()
            ),
            RectF(0f, 0f, OUTPUT_SIZE.toFloat(), OUTPUT_SIZE.toFloat()),
            paint
        )
        output
    }.getOrNull()
}
