pluginManagement {
    repositories {
        // Huawei mirrors Google Maven; dl.google.com is not always reachable.
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
    }
}

rootProject.name = "Boris"
include(":app")
