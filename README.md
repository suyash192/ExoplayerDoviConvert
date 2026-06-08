This repository is an example project demonstrating HDR bitstreams manipulation on the fly using libdovi.

# Highlights

- Dolby Vision Profile 7 to Profile 8 Conversion: Converts Profile 7 FEL and MEL videos to Profile 8 by discarding the Enhancement Layer, if any.

- Dolby Vision Stripping: Removes the Dolby Vision layer entirely from videos while keeping the underlying HDR10 or HDR10+ layers intact.

- HDR10+ Stripping: Removes the HDR10+ layer from Dolby Vision videos (mainly to resolve black screen playback bug on MediaTek MT8698D devices).

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
