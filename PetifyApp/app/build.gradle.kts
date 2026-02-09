plugins {
    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.loginfirebaseapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.loginfirebaseapp"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15" // ou versão compatível com o teu Kotlin
    }
}

dependencies {
    // 1. Firebase - Usar apenas o BoM (centraliza as versões)
    // 1. Firebase - BoM centraliza as versões para evitar conflitos
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    // ADICIONA ESTA LINHA AQUI (Sem o -ktx, pois o BoM moderno prefere assim)
    implementation("com.google.firebase:firebase-messaging")

    // 2. Imagens (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // 3. Navegação e Datas
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // 4. Compose e AndroidX (Usando as tuas variáveis libs que funcionam)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.appcompat:appcompat:1.6.1")



        implementation("io.coil-kt:coil-compose:2.5.0")

    // --- REMOVIDAS AS LINHAS QUE DAVAM ERRO ---
    // libs.firebase.storage.ktx e libs.firebase.firestore.ktx foram eliminadas
    // pois o Firebase BoM já as inclui acima.

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}