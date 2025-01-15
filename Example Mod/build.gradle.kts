import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    id("fabric-loom")
    kotlin("jvm")
    id("com.modrinth.minotaur")
    id("net.darkhax.curseforgegradle")
    id("co.uzzu.dotenv.gradle")
}
base.archivesName = project.extra["archives_base_name"] as String
version = project.extra["mod_version"] as String
group = project.extra["maven_group"] as String
dependencies {
    minecraft("com.mojang", "minecraft", project.extra["minecraft_version"] as String)
    mappings("net.fabricmc", "yarn", project.extra["yarn_mappings"] as String, null, "v2")
    modImplementation("net.fabricmc", "fabric-loader", project.extra["loader_version"] as String)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", project.extra["fabric_version"] as String)
    modImplementation("net.fabricmc", "fabric-language-kotlin", project.extra["fabric_language_kotlin_version"] as String)
}
tasks {
    val javaVersion = JavaVersion.toVersion((project.extra["java_version"] as String).toInt())
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release = javaVersion.toString().toInt()
    }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            extraWarnings = true
            jvmTarget = JvmTarget.valueOf("JVM_$javaVersion")
        }
    }
    jar { from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } } }
    processResources {
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.extra["mod_version"] as String, "fabricloader" to project.extra["loader_version"] as String, "fabric_api" to project.extra["fabric_version"] as String, "fabric_language_kotlin" to project.extra["fabric_language_kotlin_version"] as String, "minecraft" to project.extra["minecraft_version"] as String, "java" to project.extra["java_version"] as String)) }
        filesMatching("*.mixins.json") { expand(mutableMapOf("java" to project.extra["java_version"] as String)) }
    }
    java {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion.toString())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    register<TaskPublishCurseForge>("publishCurseForge") {
        disableVersionDetection()
        apiToken = env.fetch("CURSEFORGE_TOKEN", "")
        val file = upload("Replace this with the CurseForge project ID as an Integer", remapJar)
        file.displayName = "[${project.extra["minecraft_version"] as String}] Mod Name"
        file.addEnvironment("Client", "Server")
        file.changelog = ""
        file.releaseType = "release"
        file.addModLoader("Fabric")
        file.addGameVersion(project.extra["minecraft_version"] as String)
    }
}
modrinth {
    token.set(env.fetch("MODRINTH_TOKEN", ""))
    projectId.set("Replace this with the slug to the Modrinth mod page")
    uploadFile.set(tasks.remapJar)
    gameVersions.addAll(project.extra["minecraft_version"] as String)
    versionName.set("[${project.extra["minecraft_version"] as String}] Mod Name")
    dependencies { required.project("fabric-api", "fabric-language-kotlin") }
}