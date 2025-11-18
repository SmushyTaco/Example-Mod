val archivesBaseName = providers.gradleProperty("archives_base_name")
rootProject.name = archivesBaseName.get()
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
    val foojayResolverVersion = providers.gradleProperty("foojay_resolver_version")
    val loomVersion = providers.gradleProperty("loom_version")
    val kotlinVersion = providers.gradleProperty("kotlin_version")
    val minotaurVersion = providers.gradleProperty("minotaur_version")
    val curseforgeGradleVersion = providers.gradleProperty("curseforge_gradle_version")
    val dotenvVersion = providers.gradleProperty("dotenv_version")
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention").version(foojayResolverVersion.get())
        id("fabric-loom").version(loomVersion.get())
        kotlin("jvm").version(kotlinVersion.get())
        id("com.modrinth.minotaur").version(minotaurVersion.get())
        id("net.darkhax.curseforgegradle").version(curseforgeGradleVersion.get())
        id("co.uzzu.dotenv.gradle").version(dotenvVersion.get())
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}