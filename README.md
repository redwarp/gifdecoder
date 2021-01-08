# Gif Decoder

An implementation of a gif decoder written 100% in kotlin

This project contains several parts.

## decoder

[ ![Download](https://api.bintray.com/packages/redwarp/maven/gif-decoder/images/download.svg) ](https://bintray.com/redwarp/maven/gif-decoder/_latestVersion)

A simple jvm library written 100% in kotlin that handles the parsing of the gif format, headers, lzw decoder and so on.

`decoder` is available on `jcenter()`

`implementation "net.redwarp.gif:decoder:x.y.z"`

See [decoder](decoder).

## android-drawable

[ ![Download](https://api.bintray.com/packages/redwarp/maven/gif-android-drawable/images/download.svg) ](https://bintray.com/redwarp/maven/gif-android-drawable/_latestVersion)

An implementation of an Android Drawable, using the decoder, as a simple way to display a gif in an
Android app.

`android-drawable` is available on `jcenter()`

`implementation "net.redwarp.gif:android-drawable:x.y.z"`

See [android-drawable](android-drawable).

### android-drawable-native

Currently broken, an alternative to the android drawable that uses a bit of rust code for some critical sections.
More like an experiment, to see if we can be more efficient and use less battery.

See [android-drawable-native](android-drawable-native) and [giflzwdecoder](giflzwdecoder).
