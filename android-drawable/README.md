# android-drawable

[ ![Download](https://api.bintray.com/packages/redwarp/maven/gif-android-drawable/images/download.svg) ](https://bintray.com/redwarp/maven/gif-android-drawable/_latestVersion)

An implementation of an Android Drawable, using the decoder, as a simple way to display a gif in an
Android app.

## Setup

`android-drawable` is available on `jcenter()`

Add it to your dependencies with `implementation "net.redwarp.gif:android-drawable:x.y.z"`

In code, create a GifDrawable from any InputStream.
GifDrawable implements `Animatable2Compat`, and should be started if animated.

```kotlin
// Example: with a gif included in your app as an asset
val inputStream = context.assets.open("some_animated.gif")

val drawable:GifDrawable = GifDrawable.from(inputStream)
drawable.start()

imageView.setImageDrawable(drawable)
```
