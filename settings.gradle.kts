pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "autoshorts"

include(":app")
include(":core:model")
include(":core:automation")
include(":core:platform-api")
include(":platform:youtube")
include(":data:settings")
include(":billing:api")
include(":billing:noop")
include(":billing:play")
include(":testing:fake-shorts-app")
