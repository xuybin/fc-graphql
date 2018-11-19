pluginManagement {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
        maven("https://novoda.bintray.com/snapshots/")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "bintray-release" -> {
                    useModule("com.novoda:bintray-release:${requested.version}")
                }
            }
        }
    }
}
