pluginManagement {
    resolutionStrategy {
        this.eachPlugin {
            if (requested.id.id == "io.reflekt") {
                useModule("io.reflekt:gradle-plugin:${this.requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven(url = uri("https://packages.jetbrains.team/maven/p/reflekt/reflekt"))
    }
    
}
rootProject.name = "HSEditor"

