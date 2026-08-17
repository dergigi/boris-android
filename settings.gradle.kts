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
        google()
        mavenCentral()
        // Fallback when Google Maven is unreachable.
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    }
}

rootProject.name = "Boris"
include(":app")
