# ExoPlayer HDR Utils

![Maven Central Version](https://img.shields.io/maven-central/v/com.suyashbelekar/exoplayerhdrutils)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This library enables on-the-fly Dolby Vision and HDR10+ bitstream manipulation in ExoPlayer using
libdovi.

The project uses jni-rs bindings for libdovi.

Integrating only requires setting a new `HdrCompatMediaSourceFactory` in your ExoPlayer instance.

## Highlights

- **Dolby Vision Profile 7 to Profile 8 Conversion:** Can convert Profile 7 FEL and MEL videos to
  Profile 8 (discards the Enhancement Layer in FEL).

- **Dolby Vision Stripping:** Can remove the Dolby Vision layer entirely from videos while keeping
  the underlying HDR10 or HDR10+ layers intact.

- **HDR10+ Stripping:** Can remove the HDR10+ layer from Dolby Vision videos (mainly to resolve
  black screen playback bug on MediaTek MT8696D devices like the firestick 4k).

## Installation

```kts
implementation("com.suyashbelekar:exoplayerhdrutils:0.1.0")
```

## Usage

```Kotlin
// Transformation rules
val transformStrategy = TransformStrategy(
    doviP7Fel = DoviStrategy.CONVERT_TO_P8, // Or DoviStrategy.DISCARD to discard entire Dovi layer, or DoviStrategy.KEEP to keep it unchanged
    doviP7Mel = DoviStrategy.CONVERT_TO_P8,
    doviHdr10Plus = Hdr10PlusStrategy.DISCARD // Or Hdr10PlusStrategy.KEEP to keep it unchanged
)

val player = ExoPlayer.Builder(context)
    .setMediaSourceFactory(HdrCompatMediaSourceFactory(context, transformStrategy))
    .build().apply {
        setMediaItem(MediaItem.fromUri(videoUrl))
        prepare()
        playWhenReady = true
    }
```

Any existing `MediaSourceFactory` can be passed as a delegate, if needed.

```Kotlin
HdrCompatMediaSourceFactory(existingMediaSourceFactory, transformStrategy)
```

## Prerequisites & Compatibility

- **Minimum SDK:** 23
- **Media3 Version:** 1.10.1
- **Architecture Support:** arm64-v8a, armeabi-v7a, x86_64

## Acknowledgments

All credit goes to [quietvoid](https://github.com/quietvoid) for creating and
maintaining [libdovi](https://github.com/quietvoid/dovi_tool), which is the core of this
project's Dolby Vision parsing and processing capabilities.

## AI Disclosure

AI has been used in the development of this project, but except for some Android framework classes
the rest of it (especially the core logic) has been understood by me.

## Disclaimer

This library is provided "as-is" without any warranty of any kind, either expressed or implied. By
using this library, you acknowledge that I assume no liability for any bugs, app crashes, or
unexpected behavior resulting from its use. (See the MIT License for full details).