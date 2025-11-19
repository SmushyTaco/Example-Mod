import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.curseForgeGradle)
    alias(libs.plugins.dotenv)
}
val archivesBaseName = providers.gradleProperty("archives_base_name")
val modVersion = providers.gradleProperty("mod_version")
val mavenGroup = providers.gradleProperty("maven_group")

val javaVersion = libs.versions.java.map { it.toInt() }

base.archivesName = archivesBaseName
version = modVersion.get()
group = mavenGroup.get()
dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarnMappings) { classifier("v2") })
    modImplementation(libs.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)
}
java {
    toolchain {
        languageVersion = javaVersion.map { JavaLanguageVersion.of(it) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    sourceCompatibility = JavaVersion.toVersion(javaVersion.get())
    targetCompatibility = JavaVersion.toVersion(javaVersion.get())
    withSourcesJar()
}
tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.get().toString()
        targetCompatibility = javaVersion.get().toString()
        if (javaVersion.get() > 8) options.release = javaVersion
    }
    named<UpdateDaemonJvm>("updateDaemonJvm") {
        languageVersion = libs.versions.gradleJava.map { JavaLanguageVersion.of(it.toInt()) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            extraWarnings = true
            jvmTarget = javaVersion.map { JvmTarget.valueOf("JVM_${if (it == 8) "1_8" else it}") }
        }
    }
    named<Jar>("jar") {
        val rootLicense = layout.projectDirectory.file("LICENSE")
        val parentLicense = layout.projectDirectory.file("../LICENSE")
        val licenseFile = when {
            rootLicense.asFile.exists() -> {
                logger.lifecycle("Using LICENSE from project root: {}", rootLicense.asFile)
                rootLicense
            }
            parentLicense.asFile.exists() -> {
                logger.lifecycle("Using LICENSE from parent directory: {}", parentLicense.asFile)
                parentLicense
            }
            else -> {
                logger.warn("No LICENSE file found in project or parent directory.")
                null
            }
        }
        licenseFile?.let {
            from(it) {
                rename { original -> "${original}_${archiveBaseName.get()}" }
            }
        }
    }
    processResources {
        val stringModVersion = modVersion.get()
        val stringLoaderVersion = libs.versions.loader.get()
        val stringFabricVersion = libs.versions.fabric.api.get()
        val stringFabricLanguageKotlinVersion = libs.versions.fabric.language.kotlin.get()
        val stringMinecraftVersion = libs.versions.minecraft.get()
        val stringJavaVersion = libs.versions.java.get()
        inputs.property("modVersion", stringModVersion)
        inputs.property("loaderVersion", stringLoaderVersion)
        inputs.property("fabricVersion", stringFabricVersion)
        inputs.property("fabricLanguageKotlinVersion", stringFabricLanguageKotlinVersion)
        inputs.property("minecraftVersion", stringMinecraftVersion)
        inputs.property("javaVersion", stringJavaVersion)
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to stringModVersion,
                    "fabricloader" to stringLoaderVersion,
                    "fabric_api" to stringFabricVersion,
                    "fabric_language_kotlin" to stringFabricLanguageKotlinVersion,
                    "minecraft" to stringMinecraftVersion,
                    "java" to stringJavaVersion
                )
            )
        }
        filesMatching("**/*.mixins.json") { expand(mapOf("java" to stringJavaVersion)) }
    }
    register<TaskPublishCurseForge>("publishCurseForge") {
        group = "publishing"
        disableVersionDetection()
        apiToken = env.fetch("CURSEFORGE_TOKEN", "")
        val file = upload("Replace this with the CurseForge project ID as an Integer", remapJar)
        file.displayName = "[${libs.versions.minecraft.get()}] Mod Name"
        file.addEnvironment("Client", "Server")
        file.changelog = ""
        file.releaseType = "release"
        file.addModLoader("Fabric")
        file.addGameVersion(libs.versions.minecraft.get())
    }
}
modrinth {
    token = env.fetch("MODRINTH_TOKEN", "")
    projectId = "Replace this with the slug to the Modrinth mod page"
    uploadFile.set(tasks.remapJar)
    gameVersions.add(libs.versions.minecraft)
    versionName = libs.versions.minecraft.map { "[$it] Mod Name" }
    dependencies { required.project("fabric-api", "fabric-language-kotlin") }
}