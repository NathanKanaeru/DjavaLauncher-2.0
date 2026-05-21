plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore_playmarket2.jks")
            storePassword = "hayk2010"
            keyPassword = "hayk2010"
            keyAlias = "key0"
        }
        create("release") {
            storeFile = file("../keystore_playmarket2.jks")
            keyAlias = "key0"
            storePassword = "hayk2010"
            keyPassword = "hayk2010"
        }
    }
    namespace = "com.nathan.djavarp"
    //noinspection GradleDependency
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nathan.djavarp"
        minSdk = 26
        targetSdk = 36
        versionCode = 130
        versionName = "1.0"

        multiDexEnabled = true

        /*ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }*/

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        /*externalNativeBuild {
            cmake {
                cppFlags += "-std=c++11"
            }
        }*/
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")

            // Debug: Use default symbol visibility for native builds
            externalNativeBuild {
                cmake {
                    cppFlags += "-fvisibility=default"
                }
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")

            // Release: Hide native symbols to reduce binary size and surface
            externalNativeBuild {
                cmake {
                    cppFlags += "-fvisibility=hidden"
                }
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            //version = "3.22.1"
        }
    }

    ndkVersion = "26.2.11394342"

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        prefab = true
        viewBinding = true
    }

    packaging {
        jniLibs {
            excludes += "META-INF/*"
        }
        resources {
            excludes += "META-INF/*"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.fragment)
    implementation(libs.swiperefreshlayout)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.process)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.prdownloader)
    implementation(libs.volley)
    implementation(libs.sdp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.messaging)

    implementation(libs.ini4j)
    implementation(libs.glide)
    implementation(libs.paranoid)
    implementation(libs.shadowhook)

    implementation("com.github.smarteist:autoimageslider:1.4.0")
    implementation("com.github.hzy3774:AndroidP7zip:v1.7.2")
    implementation("com.hzy:un7zip:1.7.0")
    implementation("com.squareup.retrofit2:converter-gson:2.1.0")
}


afterEvaluate {
    android.applicationVariants.all {
        val variant = this
        val taskName = "copy${variant.name.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }}ApksToLaragon"
        
        tasks.register<Copy>(taskName) {
            doNotTrackState("Destination directory contains unreadable content")
            description = "Copies ${variant.name} APKs to Laragon www directory"
            into("C:\\laragon\\www")
            
            variant.outputs.all {
                @Suppress("DEPRECATION")
                val output = this as com.android.build.gradle.api.BaseVariantOutput
                from(output.outputFile)
            }
            
            doLast {
                println("Copied APKs for variant '${variant.name}' to C:\\laragon\\www")
            }
        }
        
        variant.assembleProvider.configure {
            finalizedBy(taskName)
        }
    }
}
