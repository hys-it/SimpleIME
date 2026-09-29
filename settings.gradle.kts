pluginManagement {
    repositories {
        maven(url = uri("local-maven"))
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = uri("local-maven"))
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

rootProject.name = "SimpleIME"
include(":app")
