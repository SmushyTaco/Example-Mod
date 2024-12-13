rootProject.name = settings.extra["archives_base_name"] as String
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("fabric-loom").version(settings.extra["loom_version"] as String)
        kotlin("jvm").version(settings.extra["kotlin_version"] as String)
        id("com.modrinth.minotaur").version(settings.extra["minotaur_version"] as String)
        id("net.darkhax.curseforgegradle").version(settings.extra["curseforge_gradle_version"] as String)
        id("co.uzzu.dotenv.gradle").version(settings.extra["dotenv_version"] as String)
    }
}
