// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    ivy {
        url = uri("https://download.jetbrains.com/")
        patternLayout { artifact("idea/[module]-[revision].[ext]") }
        content { includeGroup("intellij-idea") }
        metadataSources { artifact() }
    }
}

val testDmgFile: Configuration by configurations.creating

dependencies {
    implementation(libs.dd.plist)
    implementation(libs.lzfse.decode)
    testImplementation(kotlin("test"))

    // https://download.jetbrains.com/idea/ideaIC-2025.1.3.dmg
    testDmgFile(
        group = "intellij-idea",
        name = "ideaIC",
        version = "2025.1.3",
        ext = "dmg"
    )
}

tasks {
    test {
        inputs.files(testDmgFile)

        useJUnitPlatform()

        systemProperty("kdmg.test.dmg", testDmgFile.singleFile.absolutePath)
    }
}
