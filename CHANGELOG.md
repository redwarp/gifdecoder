# Changelog

## [Unreleased]

### Decoder

* Fix opening a corrupted GIFs could result in an endless loop.

### Drawable

* GifDrawable implements `mutate` properly.
* Optimization during decoding: transparency of frames is cached.

## [1.4.4] (2022-09-18)

* Hot fix: Fix compatibiliy with Android pre-Tiramisu.

## [1.4.3] (2022-09-17)

* Hot fix: Fix BufferedReplayInputStream trying to read after close.

## [1.4.2] (2022-09-17)

* Fix concurrency bug in BufferedReplayInputStream.

## [1.4.1] (2022-09-16)

* Bump libraries.
* Fix publication of sources and doc.

## [1.4.0] (2022-08-27)

* Rewrite the BufferedRandomAccessFile so that the file gets closed between read.
* android-drawable now targets Android SDK 33.

## [1.3.1] (2022-07-26)

* Fix concurrency issue with the bitmap field in GifDrawable.

## [1.3.0] (2022-07-26)

* Fix paint dithering to use 565 for non transparent gifs.
* Fix recycle of bitmaps, should behave nicer for memory.
* Target kotlin 1.7.10

## [1.2.0] (2022-06-13)

* Rewrite GifDrawable (again), it will now work properly on Android 12, and Android at large (Yup, was broken).
* Save current frame index, so that we display proper frame when starting, stopping or pausing the GifDrawable.
* Bump Android sdk to 32, AGP and dependencies
* Add `getDelay` method to GIF.

## [1.1.2] (2022-02-16)

* `setVisible` will try to be less smart, and not assume that visibility change is actually reliable.

## [1.1.1] (2022-02-16)

* Better frame timing for GifDrawawble.
* GifDrawable now implements setVisible properly, and the frame preparation future will be properly cancelled.

## [1.1.0] (2022-02-16)

* Gif now implements AutoClosable.
* Rewrite GifDrawable in pure android to ditch dependencies to coroutines.

## [1.0.0] (2022-02-10)

* Arbitrarily decide it's the 1.0.0. Pretty stable I would say.
* Project updated to kotlin 1.6.0.
* Project now targets java 11.
* Opening a Gif from a file will now use a buffered RandomAccessFile, to limit read operations.
* As GifDescriptor can be used by multiple Gifs, we synchronize the read block when decoding a frame to avoid weird multi-threading artifacts.

## [0.9.0] (2021-11-10)

* Simplify bitmap pool, aka stop pooling. It's not that useful.
* GifDrawable's backgroundColor is now a val.

## [0.8.1] (2021-10-11)

* Fix an issue where the decoder would not reset the canvas before restarting the animation loop.
  Depending on the disposal method of the last frame, it could lead to artifacts.

## [0.8.0] (2021-10-01)

* Breaking: `advance()` now decodes the frame. It used to be done by the `getFrame` methods.
  With this change, `getFrames` will either simply copy the internal buffer, or called `advance` if needed.
* Breaking: `decoder.Result` class has been replaced by the `kotlin.Result` class, now stable.
  Several function in the Gif class that would return nullable now return `kotlin.Result` instead.
* Bump kotlin to 1.5.31, AGP to 7.0.2
* Target SDK 31

## [0.7.3] (2021-09-21)

* Add a helper method to create a shallow copy of a Gif sharing GifDescriptors.

## [0.7.2] (2021-08-12)

* Fix a signed error in Dimension and Position: they were short instead of unsigned short, causing issues for giant gifs.

## [0.7.1] (2021-08-07)

* Memory optimization (free data from memory early).
* Get rid of the useless BitmapShader in the GifDrawable.

## [0.7.0] (2021-08-07)

* Update documentation.
* Minor refactor (Point becomes Position).
* Upgrade Android Gradle Plugin to 7.0.0, and dependencies in general.
* Deprecate setRepeatCount and getRepeatCount in the GifDrawable.

## [0.6.1] (2021-07-09)

* Upgrade gradle wrapper to 7.1.1.
* Bump libraries, including AGP and kotlin.
* Add a few comments there and there.
* Change package of utility classes.

## [0.6.0] (2021-05-15)

* Update kotlin to 1.5.0.
* Update AGP to 4.2.1.

## [0.5.1] (2021-03-31)

* Get rid of netty, replaced by a naive homebrew solution.

## [0.5.0] (2021-03-26)

* Breaking: parser and Gif now avoid Exceptions, in favor of Result<>.
* Bump a few libs and kotlin.

## [0.4.0] (2021-02-27)

* Introduce an API to decode files directly via a random access file instead of loading everthing from an InputStream.

## [0.3.0] (2021-02-26)

* Change package name to app.redwarp.gif (because I don't own the domain redwarp.net).
* Change publication repository to Maven Central.

## [0.2.2] (2021-01-08)

* Adds a constant state to the GifDrawable, to facilitate copy. Uncharted territory.

## [0.2.1] (2021-01-08)

* Increase robustness of GIF parsing by not throwing an exception on a wrong disposal method.
  Instead, default to "not specified".

## [0.2.0] (2021-01-08)

* Improve timing of frames by having the loop independant of the draw call.
* Made sure that if the drawable is not drawn again, the loop stops. No need to calculate in vacum.

## [0.1.1] (2021-01-06)

* Code cleanup

## [0.1.1] (2021-01-05)

* Initial release
* API is not stable/final yet.
