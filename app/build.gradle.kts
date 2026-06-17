    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.compose)
        alias(libs.plugins.ksp)
        alias(libs.plugins.hilt)
        kotlin("plugin.serialization") version "2.2.10"
    }

    android {
        namespace = "com.sumitupdat.universalfileeditorviewer"
        compileSdk = 37

        defaultConfig {
            applicationId = "com.sumitupdat.universalfileeditorviewer"
            minSdk = 26
            targetSdk = 37
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        buildFeatures {
            compose = true
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "META-INF/DEPENDENCIES"
                excludes += "META-INF/NOTICE"
                excludes += "META-INF/LICENSE"
                excludes += "META-INF/LICENSE.txt"
                excludes += "META-INF/NOTICE.txt"
            }
        }
    }

    dependencies {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.material.icons.extended)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.biometric)
        implementation(libs.androidx.work.runtime.ktx)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.documentfile)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        ksp(libs.androidx.room.compiler)
        implementation(libs.hilt.android)
        ksp(libs.hilt.compiler)
        implementation(libs.androidx.hilt.navigation.compose)
        implementation(libs.coil.compose)
        implementation(libs.androidx.media3.exoplayer)
        implementation(libs.androidx.media3.ui)
        implementation(libs.commons.compress)
        implementation(libs.poi.ooxml)
        implementation(libs.poi.scratchpad)
        implementation(libs.junrar)
        implementation(libs.odfdom.java)
        implementation(libs.androidx.datastore)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.kotlinx.serialization.json)

        testImplementation(libs.junit)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.junit)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
        debugImplementation(libs.androidx.compose.ui.tooling)
    }
