buildscript {
    repositories {
        maven(url = uri("$rootDir/local-maven"))
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
    }
}
