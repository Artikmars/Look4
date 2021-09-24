plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
    kotlin("kapt")
}

val composeVersion = "1.0.2"
val hiltVersion = "2.36"


android {
    compileSdk = 30

    defaultConfig {
        applicationId = "com.artamonov.look4"
        minSdk = 23
        targetSdk = 30
        versionCode = 10008
        versionName = "1.04.5"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("android.arch.lifecycle:extensions:1.1.1")

    // Android X
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.0")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.preference:preference-ktx:1.1.1")

    implementation("com.github.bumptech.glide:glide:4.9.0")
    implementation("com.github.dhaval2404:imagepicker:1.7.1")
    implementation("com.google.android.material:material:1.1.0")
    implementation("com.google.android.gms:play-services-nearby:18.0.0")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation("com.github.chnouman:AwesomeDialog:1.0.4")

    //Firebase Crashlytics SDK
    implementation("com.google.firebase:firebase-analytics-ktx:19.0.1")
    implementation("com.google.firebase:firebase-crashlytics:18.2.1")

    implementation("de.hdodenhof:circleimageview:3.0.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:${hiltVersion}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    kapt("com.google.dagger:hilt-android-compiler:${hiltVersion}")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    // Leak Canary
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.4")

    // Jetpack Compose Toolkit
    implementation("androidx.compose.ui:ui:${composeVersion}")
    // Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.foundation:foundation:${composeVersion}")
    // Material Design
    implementation("androidx.compose.material:material:${composeVersion}")
    // Material design icons
    implementation("androidx.compose.material:material-icons-core:${composeVersion}")
    implementation("androidx.compose.material:material-icons-extended:${composeVersion}")
    // Integration with activities
    implementation("androidx.activity:activity-compose:1.3.1")
    // Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07")
    // Integration with observables
    implementation("androidx.compose.runtime:runtime-livedata:${composeVersion}")
    implementation("androidx.compose.runtime:runtime-rxjava2:${composeVersion}")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0-beta02")


    androidTestImplementation("androidx.compose.ui:ui-test-junit4:${composeVersion}")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")
    implementation("com.google.android.gms:play-services-ads:20.4.0")
}
