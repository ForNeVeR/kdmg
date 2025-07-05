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
$ ./gradlew :app:run
```

Documentation
-------------
- [Contributor Guide][docs.contributing]
- [Maintainer Guide][docs.maintaining]

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
[status-zero]: https://img.shields.io/badge/status-zero-lightgrey.svg
