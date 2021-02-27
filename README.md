# Gif Decoder

An implementation of a gif decoder written 100% in kotlin

This project contains several parts.

## decoder

![Maven Central](https://img.shields.io/maven-central/v/app.redwarp.gif/decoder)

A simple jvm library written 100% in kotlin that handles the parsing of the gif format, headers, lzw decoder and so on.

`decoder` is available on `mavenCentral()`

`implementation "app.redwarp.gif:decoder:x.y.z"`

See [decoder](decoder).

## android-drawable

![Maven Central](https://img.shields.io/maven-central/v/app.redwarp.gif/android-drawable)

An implementation of an Android Drawable, using the decoder, as a simple way to display a gif in an
Android app.

`android-drawable` is available on `mavenCentral()`

`implementation "app.redwarp.gif:android-drawable:x.y.z"`

See [android-drawable](android-drawable).

### android-drawable-native

Currently broken, an alternative to the android drawable that uses a bit of rust code for some critical sections.
More like an experiment, to see if we can be more efficient and use less battery.

See [android-drawable-native](android-drawable-native) and [giflzwdecoder](giflzwdecoder).
