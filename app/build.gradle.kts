import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun gitCommit(): String {
    return runCatching {
        ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(rootProject.projectDir)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
    }.getOrNull()?.takeIf { it.matches(Regex("[0-9a-f]{7,40}")) } ?: "unknown"
}

fun localProp(name: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return System.getenv(name)
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props.getProperty(name) ?: System.getenv(name)
}

android {
    namespace = "org.dergigi.boris"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "org.dergigi.boris"
        minSdk = 26
        targetSdk = 35
        versionCode = 157
        versionName = "1.4.50"
        buildConfigField("String", "GIT_COMMIT", "\"${gitCommit()}\"")
    }

    val storeFilePath = localProp("OEM_STORE_FILE")
    val storePassword = localProp("OEM_STORE_PASSWORD")
    val keyAlias = localProp("OEM_KEY_ALIAS")
    val keyPassword = localProp("OEM_KEY_PASSWORD")
    val hasReleaseSigning =
        !storeFilePath.isNullOrBlank() &&
            !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank() &&
            file(storeFilePath).exists()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(storeFilePath!!)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
        lintConfig = file("lint.xml")
        htmlReport = true
        xmlReport = true
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

androidComponents {
    beforeVariants { variant ->
        variant.enableAndroidTest = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
    implementation(libs.secp256k1.kmp)
    implementation(libs.secp256k1.jni.android)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.secp256k1.jni.jvm)
}
