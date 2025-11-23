pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Event Monitor"
include(":app")
include(":core:common")
include(":core:data")
include(":core:domain")
include(":feature:headcounter")
include(":feature:lostandfound")
include(":feature:incidents")
