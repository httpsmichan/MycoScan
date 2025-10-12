plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    packaging {
        resources {
            pickFirsts += "META-INF/*"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    implementation(libs.tflite)
    implementation(libs.tflite.support)
    implementation(libs.tflite.gpu)
    implementation(libs.tflite.metadata)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    implementation(libs.osmdroid)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    implementation(libs.cloudinary.android) {
        exclude(group = "com.cloudinary", module = "cloudinary-android-preprocess")
        exclude(group = "com.linkedin.android.litr", module = "litr")
    }

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.glide.okhttp3)

    implementation(libs.mpandroidchart)

}

configurations.all {
    exclude(group = "com.google.ai.edge.litert", module = "litert-api")
}
