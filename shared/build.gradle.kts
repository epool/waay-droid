import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.skie)
    alias(libs.plugins.kover)
}

kotlin {
    // Constitution V: minimal, deliberate public surface for Swift (see plan.md visibility rule).
    explicitApi()

    android {
        namespace = "dev.epool.waay.shared"
        compileSdk {
            version =
                release(
                    libs.versions.android.compileSdk
                        .get()
                        .toInt(),
                ) {
                    minorApiLevel =
                        libs.versions.android.compileSdkMinor
                            .get()
                            .toInt()
                }
        }
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "dev.epool.waay.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // api: androidApp sees ViewModel/Koin types; Swift sees them through the framework.
            api(libs.jetbrains.lifecycle.viewmodel)
            api(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.multiplatform.settings.test)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

// Constitution VIII / ADR-011: line coverage of the shared brain, measured by the Android host tests.
kover {
    reports {
        filters {
            includes {
                classes(
                    "dev.epool.waay.game.domain.*",
                    "dev.epool.waay.*.presentation.*",
                    "dev.epool.waay.core.i18n.*",
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
    }
}
