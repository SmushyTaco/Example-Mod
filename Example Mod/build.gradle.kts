import com.google.gson.Gson
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.curseForgeGradle)
    alias(libs.plugins.dotenv)
}
val archivesBaseName: Provider<String> = providers.gradleProperty("archives_base_name")
val modVersion: Provider<String> = providers.gradleProperty("mod_version")
val mavenGroup: Provider<String> = providers.gradleProperty("maven_group")

val javaVersion: Provider<Int> = libs.versions.java.map { it.toInt() }

base.archivesName = archivesBaseName
version = modVersion.get()
group = mavenGroup.get()

class AccountsJson(val accounts: List<Account>)
class Account(val profile: Profile, val ygg: YGG)
class YGG(val token: String)
class Profile(val name: String, val id: String)

val prismAccountsFile = providers.provider {
    val explicit = providers.gradleProperty("prism.accounts.file").orNull
    if (explicit != null) return@provider File(explicit)

    val home = System.getProperty("user.home")

    val candidates = buildList {
        System.getenv("APPDATA")?.let { add(File(it, "PrismLauncher/accounts.json")) }
        System.getenv("HOMEPATH")?.let { add(File(it, "scoop/persist/prismlauncher/accounts.json")) }
        val xdgDataHome = System.getenv("XDG_DATA_HOME")
        if (xdgDataHome != null) {
            add(File(xdgDataHome, "PrismLauncher/accounts.json"))
        } else {
            add(File(home, ".local/share/PrismLauncher/accounts.json"))
        }
        add(File(home, ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/accounts.json"))
        add(File(home, "Library/Application Support/PrismLauncher/accounts.json"))
    }
    candidates.firstOrNull(File::exists)
}

loom {
    runs {
        prismAccountsFile.orNull?.let { file ->
            val account: Provider<Account> = providers.fileContents(layout.file(providers.provider { file }))
                .asText
                .map { jsonStr ->
                    val accountNumber = (providers.gradleProperty("prism.accounts.number").orNull?.toInt() ?: 1) - 1
                    val accounts = Gson().fromJson(jsonStr, AccountsJson::class.java).accounts
                    accounts.getOrNull(accountNumber.coerceIn(0, accounts.size - 1))
                        ?: error("No PrismLauncher accounts found in ${file.absolutePath}")
                }
            register("clientAuth") {
                inherit(getByName("client"))
                configName = "Minecraft Client (Auth)"
                val acc = account.get()
                programArgs("--username", acc.profile.name, "--uuid", acc.profile.id, "--accessToken", acc.ygg.token)
            }
        }
    }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)
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
val licenseFile = run {
    val rootLicense = layout.projectDirectory.file("LICENSE")
    val parentLicense = layout.projectDirectory.file("../LICENSE")
    when {
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
}
tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.get().toString()
        targetCompatibility = javaVersion.get().toString()
        if (javaVersion.get() > 8) options.release = javaVersion
    }
    withType<UpdateDaemonJvm>().configureEach {
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
    withType<Jar>().configureEach {
        licenseFile?.let {
            from(it) {
                rename { original -> "${original}_${archiveBaseName.get()}" }
            }
        }
    }
    processResources {
        val resourceMap = mapOf(
            "version" to modVersion.get(),
            "fabricloader" to libs.versions.loader.get(),
            "fabric_api" to libs.versions.fabric.api.get(),
            "fabric_language_kotlin" to libs.versions.fabric.language.kotlin.get(),
            "minecraft" to libs.versions.minecraft.get(),
            "java" to libs.versions.java.get()
        )
        inputs.properties(resourceMap)
        filesMatching("fabric.mod.json") { expand(resourceMap) }
        filesMatching("**/*.mixins.json") { expand(resourceMap.filterKeys { it == "java" }) }
    }
    register<TaskPublishCurseForge>("publishCurseForge") {
        group = "publishing"
        disableVersionDetection()
        apiToken = env.fetch("CURSEFORGE_TOKEN", "")
        val file = upload("Replace this with the CurseForge project ID as an Integer", jar)
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
    uploadFile.set(tasks.jar)
    gameVersions.add(libs.versions.minecraft)
    versionName = libs.versions.minecraft.map { "[$it] Mod Name" }
    dependencies { required.project("fabric-api", "fabric-language-kotlin") }
}