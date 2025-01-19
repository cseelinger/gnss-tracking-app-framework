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
        // Add paho for MQTT
        maven {
            url = uri("https://repo.eclipse.org/content/repositories/paho-snapshots/")
        }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add paho for MQTT
        maven {
            url = uri("https://repo.eclipse.org/content/repositories/paho-snapshots/")
        }
    }
}

rootProject.name = "GNSSTrackingApp"
include(":app")
 