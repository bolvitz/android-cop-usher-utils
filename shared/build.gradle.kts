plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // Kotlin serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // Koin DI
                implementation("io.insert-koin:koin-core:3.5.6")

                // Firebase KMP SDK (GitLiveKit)
                implementation("dev.gitlive:firebase-firestore:1.12.0")
                implementation("dev.gitlive:firebase-auth:1.12.0")
                implementation("dev.gitlive:firebase-common:1.12.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific Firebase dependencies
                implementation("com.google.firebase:firebase-firestore-ktx:24.11.0")
                implementation("com.google.firebase:firebase-auth-ktx:22.3.1")

                // AndroidX lifecycle
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

                // Koin for Android
                implementation("io.insert-koin:koin-android:3.5.6")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }
    }
}

android {
    namespace = "com.eventmonitor.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
