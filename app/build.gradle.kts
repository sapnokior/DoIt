/*
 * Copyright 2026 Stanislav Aleshin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
}

val localProperties = gradleLocalProperties(rootDir, providers)

android {
    namespace = libs.versions.applicationId.get()
    compileSdk = libs.versions.compileSdkVersion.get().toIntOrNull()
    flavorDimensions += libs.versions.productionDimension.get()

    defaultConfig {
        applicationId = libs.versions.applicationId.get()
        minSdk = libs.versions.minSdkVersion.get().toIntOrNull()
        targetSdk = libs.versions.targetSdkVersion.get().toIntOrNull()
        compileSdk = libs.versions.compileSdkVersion.get().toIntOrNull()

        versionCode = libs.versions.versionCode.get().toIntOrNull()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = libs.versions.testInstrumentRunner.get()
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("storePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        getByName("debug") {
            val storeFilePath = localProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("storePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isCrunchPngs = false
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    productFlavors {
        create("fdroid") {
            dimension = libs.versions.productionDimension.get()
        }
        create("rustore") {
            dimension = libs.versions.productionDimension.get()
        }
        create("huawei") {
            dimension = libs.versions.productionDimension.get()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            resources.pickFirsts.add("META-INF/INDEX.LIST")
            resources.merges.add("META-INF/DEPENDENCIES")
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

val rustoreImplementation = "rustoreImplementation"
val fdroidImplementation = "fdroidImplementation"
val huaweiImplementation = "huaweiImplementation"

dependencies {

    implementation(project(":core:utils"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:presentation"))
    implementation(project(":core:ui"))
    implementation(project(":widgets"))

    implementation(project(":features:home:api"))
    implementation(project(":features:home:impl"))
    implementation(project(":features:overview:api"))
    implementation(project(":features:overview:impl"))
    implementation(project(":features:templates:api"))
    implementation(project(":features:templates:impl"))
    implementation(project(":features:editor:api"))
    implementation(project(":features:editor:impl"))
    implementation(project(":features:analytics:api"))
    implementation(project(":features:analytics:impl"))
    implementation(project(":features:settings:api"))
    implementation(project(":features:settings:impl"))

    implementation(libs.compose.material3.adaptive.navigation.suite)

    ksp(libs.dagger.ksp)

    implementation(libs.room.core)
    ksp(libs.room.ksp)

    testImplementation(libs.jUnit)
    androidTestImplementation(libs.jUnitExt)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.composeJUnit)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.testmanifest)
}

tasks.register("debugBuild") {
    dependsOn("assembleFdroidDebug")
    description = "Builds the default debug APK"
    group = "build"
}
