<!--
SPDX-FileCopyrightText: 2024-2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>

SPDX-License-Identifier: Apache-2.0
-->

Contributor Guide
=================

Build
-----
To assemble the code (only compile, without tests), run the following shell command:
```console
$ ./gradlew assemble
```

To run tests:
```console
$ ./gradlew check
```

To do everything with a single command:
```console
$ ./gradlew build
```

License Automation
------------------
<!-- REUSE-IgnoreStart -->
If the CI asks you to update the file licenses, follow one of these:
1. Update the headers manually (look at the existing files), something like this:
   ```csharp
   // SPDX-FileCopyrightText: %year% %your name% <%your contact info, e.g. email%>
   //
   // SPDX-License-Identifier: Apache-2.0
   ```
   (accommodate to the file's comment style if required).
2. Alternately, use the [REUSE][reuse] tool:
   ```console
   $ reuse annotate --license Apache-2.0 --copyright '%your name% <%your contact info, e.g. email%>' %file names to annotate%
   ```

(Feel free to attribute the changes to "kdmg contributors <https://github.com/ForNeVeR/kdmg>" instead of your name in a multi-author file, or if you don't want your name to be mentioned in the project's source: this doesn't mean you'll lose the copyright.)
<!-- REUSE-IgnoreEnd -->

[reuse]: https://reuse.software/
