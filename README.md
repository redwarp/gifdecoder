# Gif Decoder

An implementation of a gif decoder written 100% in kotlin

This project contains several parts.

## decoder

[![Maven Central](https://img.shields.io/maven-central/v/app.redwarp.gif/decoder?style=flat-square)](https://search.maven.org/artifact/app.redwarp.gif/decoder)

A simple jvm library written 100% in kotlin that handles the parsing of the gif format, headers, lzw decoder and so on.

`decoder` is available on `mavenCentral()`

 `implementation "app.redwarp.gif:decoder:x.y.z"`

See [decoder](decoder).

## android-drawable

[![Maven Central](https://img.shields.io/maven-central/v/app.redwarp.gif/android-drawable?style=flat-square)](https://search.maven.org/artifact/app.redwarp.gif/android-drawable)

An implementation of an Android Drawable, using the decoder, as a simple way to display a gif in an
Android app.

`android-drawable` is available on `mavenCentral()`

 `implementation "app.redwarp.gif:android-drawable:x.y.z"`

See [android-drawable](android-drawable).

## License

The decoder and android-drawable library are both under an **Apache 2.0 License**.

You might notice a few classes in the project copyrighted to *Google*: they are the OG decoder copied from the [bumptech/glide library](https://github.com/bumptech/glide), and are included in the project to do benchmarking and compare performance between the java implementation and the kotlin one.

They are not actually included in the two deliverables, `decoder` and `android-drawable` .

The `bitmap.h` in the giflzwdecoder folder is copyrighted to the *Android Open Source Project*, but is also not included in the deliverables: it's part of a currently failed attempt at making a native implementation of the decoder.

Android ndk + rust is not trivial, and that attempt is currently on hold.

Even though my first attempt at it kind of worked, it was naive and did come with a set of issues so I dropped it. I might get back to it in the future.
