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
    maven {
      url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
    }
  }
}

rootProject.name = "WatchPilot"

include(":app")
