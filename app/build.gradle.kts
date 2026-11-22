import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

jacoco { toolVersion = "0.8.16" }

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

fun literalJava(valor: String) = "\"" + valor.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

val archivoFirmaRelease = providers.environmentVariable("SANA_RELEASE_STORE_FILE").orNull
val claveAlmacenRelease = providers.environmentVariable("SANA_RELEASE_STORE_PASSWORD").orNull
val claveFirmaRelease = providers.environmentVariable("SANA_RELEASE_KEY_PASSWORD").orNull
val firmaReleaseDisponible = listOf(archivoFirmaRelease, claveAlmacenRelease, claveFirmaRelease).all { !it.isNullOrBlank() }

android {
    namespace = "com.example.sanaesencia"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sanaesencia"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "USUARIOS_API_URL", literalJava(providers.gradleProperty("USUARIOS_API_URL").getOrElse("https://54.204.182.160/usuarios/")))
        buildConfigField("String", "CATALOGO_API_URL", literalJava(providers.gradleProperty("CATALOGO_API_URL").getOrElse("https://54.204.182.160/catalogo/")))
        buildConfigField("String", "RESERVAS_API_URL", literalJava(providers.gradleProperty("RESERVAS_API_URL").getOrElse("https://54.204.182.160/reservas/")))
    }

    signingConfigs {
        if (firmaReleaseDisponible) {
            create("sanaRelease") {
                storeFile = file(archivoFirmaRelease!!)
                storePassword = claveAlmacenRelease
                keyAlias = "sanaesencia"
                keyPassword = claveFirmaRelease
                storeType = "JKS"
            }
        }
    }
    buildTypes {
        release {
            if (firmaReleaseDisponible) signingConfig = signingConfigs.getByName("sanaRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            optimization {
                // La optimización parcial de Kotlin causaba IllegalAccessError al iniciar release.
                // La firma y la protección de credenciales no dependen de ofuscar el APK.
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val jacocoLogicaReport = tasks.register<JacocoReport>("jacocoLogicaReport") {
    dependsOn("testDebugUnitTest")
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
        include("com/example/sanaesencia/viewmodel/**", "com/example/sanaesencia/data/repository/**",
            "com/example/sanaesencia/domain/**", "com/example/sanaesencia/utils/**")
    })
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/logica"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/logica.xml"))
    }
}

tasks.register("createDebugUnitTestCoverageReport") {
    dependsOn(jacocoLogicaReport)
}

tasks.register<JacocoCoverageVerification>("verificarCoberturaLogica") {
    dependsOn(jacocoLogicaReport)
    executionData.setFrom(jacocoLogicaReport.map { it.executionData })
    classDirectories.setFrom(jacocoLogicaReport.map { it.classDirectories })
    violationRules {
        rule {
            limit { counter = "LINE"; minimum = "0.80".toBigDecimal() }
            limit { counter = "INSTRUCTION"; minimum = "0.80".toBigDecimal() }
        }
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    ksp(libs.androidx.room.compiler)         // genera el código de los DAOs

}
