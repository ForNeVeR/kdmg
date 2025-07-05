<!--
SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>

SPDX-License-Identifier: Apache-2.0
-->

kdmg [![Status Zero][status-zero]][andivionian-status-classifier]
====

kdmg is a Kotlin library to unpack [DMG files][spec.dmg] (disk images often used for macOS software distribution).

This Repository
---------------
This repository includes the library code in the `lib` module,
and a command-line application to perform some basic tasks in the `app` module.

### Kotlin Library
The library will be published to Maven Central, the usage documentation will be available later.

### Diagnostic Application
Use the following shell command to run the application:
```console
$ ./gradlew :app:run --args="enter the real args here"
```

Where args could be:
- `dmg <path to a .dmg file>` - will find a HFS+ image in the file, unpack and save it to the path `app/build/image.hfs`.

Documentation
-------------
- [Contributor Guide][docs.contributing]
- [Maintainer Guide][docs.maintaining]

Format Specifications
---------------------
- [Technical Note TN1150: HFS Plus Volume Format][spec.hfs-plus] (Apple)
- [Demystifying the DMG File Format][spec.dmg] (Jonathan Levin)

License
-------
The project is distributed under the terms of [the Apache 2.0 license][docs.license].

The license indication in the project's sources is compliant with the [REUSE specification v3.3][reuse.spec].

[andivionian-status-classifier]: https://andivionian.fornever.me/v1/#status-zero-
[docs.contributing]: CONTRIBUTING.md
[docs.license]: LICENSE.txt
[docs.maintaining]: MAINTAINING.md
[reuse.spec]: https://reuse.software/spec-3.3/
[spec.dmg]: https://newosxbook.com/DMG.html
[spec.hfs-plus]: https://developer.apple.com/library/archive/technotes/tn/tn1150.html
[status-zero]: https://img.shields.io/badge/status-zero-lightgrey.svg
