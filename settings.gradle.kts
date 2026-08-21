pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Fallback when dl.google.com is unreachable.
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // dl.google.com is blackholed on some networks and stalls resolution
        // for minutes per module. Ask reachable repos first: Maven Central for
        // most things, the Huawei mirror for Google-hosted artifacts, and only
        // then Google Maven itself for anything the mirror lacks.
        mavenCentral()
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
    }
}

rootProject.name = "Boris"
include(":app")
