# Changelog

## [0.2.2] (2010-01-08)

- Adds a constant state to the GifDrawable, to facilitate copy. Uncharted territory.

## [0.2.1] (2010-01-08)

- Increase robustness of GIF parsing by not throwing an exception on a wrong disposal method.
  Instead, default to "not specified".

## [0.2.0] (2010-01-08)

- Improve timing of frames by having the loop independant of the draw call.
- Made sure that if the drawable is not drawn again, the loop stops. No need to calculate in vacum.

## [0.1.1] (2021-01-06)

- Code cleanup

## [0.1.1] (2021-01-05)

- Initial release
- API is not stable/final yet.
