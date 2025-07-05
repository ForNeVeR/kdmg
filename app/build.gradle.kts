// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    application
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass = "me.fornever.kdmg.AppKt"
}
