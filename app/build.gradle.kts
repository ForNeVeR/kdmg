// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.lzfse.decode)
}

application {
    mainClass = "me.fornever.kdmg.app.AppKt"
}
