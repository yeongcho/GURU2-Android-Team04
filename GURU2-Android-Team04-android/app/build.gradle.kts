plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.guru2_android_team04_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.guru2_android_team04_android"
        minSdk = 19
        targetSdk = 34
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

    kotlinOptions {
        jvmTarget = "11"
    }

    // ★★★ 여기만 추가! ★★★
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ViewPager2 (탭 전환)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // TabLayout (캘린더/리스트 탭)
    implementation("com.google.android.material:material:1.11.0")

    // RecyclerView (리스트)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // MPAndroidChart (파이 차트)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
}