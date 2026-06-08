package com.suyashbelekar.exoplayerdoviconvert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.suyashbelekar.exoplayerdoviconvert.ui.theme.ExoplayerDoviConvertTheme
import com.suyashbelekar.exoplayerdoviconvert.video.transformers.DoviStrategy
import com.suyashbelekar.exoplayerdoviconvert.video.transformers.Hdr10PlusStrategy
import com.suyashbelekar.exoplayerdoviconvert.video.transformers.TransformStrategy

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExoplayerDoviConvertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    SimpleVideoPlayer(
                        // Change to actual URL
                        "http://192.168.1.100:3000/dovi-video.mkv"
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SimpleVideoPlayer(videoUrl: String) {
    val context = LocalContext.current

    val exoPlayer = remember {
        val transformStrategy = TransformStrategy(
            doviP7Fel = DoviStrategy.CONVERT_TO_P8, // Or DoviStrategy.DISCARD to discard entire Dovi layer, or DoviStrategy.KEEP to keep it unchanged
            doviP7Mel = DoviStrategy.CONVERT_TO_P8,
            doviHdr10Plus = Hdr10PlusStrategy.DISCARD // Or Hdr10PlusStrategy.KEEP to keep it unchanged
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(
            context,
            BitstreamTransformingExtractorsFactory(transformStrategy)
        )

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}