This repository is an example project demonstrating Dolby Vision bitstream manipulation on the fly in ExoPlayer using libdovi.

The project uses jni-rs bindings for libdovi.

All logic is encapsulated within its own dedicated classes, integrating only requires setting a new `MediaSourceFactory` in the ExoPlayer instance.

# Highlights

- **Dolby Vision Profile 7 to Profile 8 Conversion:** Can convert Profile 7 FEL and MEL videos to Profile 8 (discards the Enhancement Layer in FEL).

- **Dolby Vision Stripping:** Can remove the Dolby Vision layer entirely from videos while keeping the underlying HDR10 or HDR10+ layers intact.

- **HDR10+ Stripping:** Can remove the HDR10+ layer from Dolby Vision videos (mainly to resolve black screen playback bug on MediaTek MT8696D devices like the firestick 4k).

# Example

```Kotlin
// Transformation rules
val transformStrategy = TransformStrategy(
    doviP7Fel = DoviStrategy.CONVERT_TO_P8, // Or DoviStrategy.DISCARD to discard entire Dovi layer, or DoviStrategy.KEEP to keep it unchanged
    doviP7Mel = DoviStrategy.CONVERT_TO_P8,
    doviHdr10Plus = Hdr10PlusStrategy.DISCARD // Or Hdr10PlusStrategy.KEEP to keep it unchanged
)

val mediaSourceFactory = DefaultMediaSourceFactory(
    context,
    BitstreamTransformingExtractorsFactory(transformStrategy)
)

val player = ExoPlayer.Builder(context)
    .setMediaSourceFactory(mediaSourceFactory)
    .build().apply {
        setMediaItem(MediaItem.fromUri(videoUrl))
        prepare()
        playWhenReady = true
    }
```

## AI Disclosure

AI has been used in the development of this project, but except for some Android framework classes most of it has been understood by me.
