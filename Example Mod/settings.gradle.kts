val archivesBaseName = providers.gradleProperty("archives_base_name")
rootProject.name = archivesBaseName.get()
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
    val foojayResolverVersion = providers.gradleProperty("foojay_resolver_version")
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention").version(foojayResolverVersion.get())
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}