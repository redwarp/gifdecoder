# Changelog

## [0.7.1] (2021-08-07)

- Memory optimization (free data from memory early).
- Get rid of the useless BitmapShader in the GifDrawable.

## [0.7.0] (2021-08-07)

- Update documentation.
- Minor refactor (Point becomes Position).
- Upgrade Android Gradle Plugin to 7.0.0, and dependencies in general.
- Deprecate setRepeatCount and getRepeatCount in the GifDrawable.

## [0.6.1] (2021-07-09)

- Upgrade gradle wrapper to 7.1.1.
- Bump libraries, including AGP and kotlin.
- Add a few comments there and there.
- Change package of utility classes.

## [0.6.0] (2021-05-15)

- Update kotlin to 1.5.0.
- Update AGP to 4.2.1.

## [0.5.1] (2021-03-31)

- Get rid of netty, replaced by a naive homebrew solution.

## [0.5.0] (2021-03-26)

- Breaking: parser and Gif now avoid Exceptions, in favor of Result<>.
- Bump a few libs and kotlin.

## [0.4.0] (2021-02-27)

- Introduce an API to decode files directly via a random access file instead of loading everthing from an InputStream.

## [0.3.0] (2021-02-26)

- Change package name to app.redwarp.gif (because I don't own the domain redwarp.net).
- Change publication repository to Maven Central.

## [0.2.2] (2021-01-08)

- Adds a constant state to the GifDrawable, to facilitate copy. Uncharted territory.

## [0.2.1] (2021-01-08)

- Increase robustness of GIF parsing by not throwing an exception on a wrong disposal method.
  Instead, default to "not specified".

## [0.2.0] (2021-01-08)

- Improve timing of frames by having the loop independant of the draw call.
- Made sure that if the drawable is not drawn again, the loop stops. No need to calculate in vacum.

## [0.1.1] (2021-01-06)

- Code cleanup

## [0.1.1] (2021-01-05)

- Initial release
- API is not stable/final yet.
