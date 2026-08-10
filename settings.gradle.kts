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
        // Repositorio de Vosk (motor de voz 100% offline, sin API keys)
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}

rootProject.name = "Panda"
include(":app")
