val archivesBaseName = providers.gradleProperty("archives_base_name")
rootProject.name = archivesBaseName.get()
pluginManagement {
    fun isRepoHealthy(url: String): Boolean {
        var connection: javax.net.ssl.HttpsURLConnection? = null
        return try {
            connection = java.net.URI(url).toURL().openConnection() as javax.net.ssl.HttpsURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.instanceFollowRedirects = true
            connection.connect()
            val code = connection.responseCode
            code in 200..399
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
    fun repoUrlWithFallbacks(candidates: List<String>): String {
        if (candidates.isEmpty()) {
            val badLink = "https://mock.httpstatus.io/500"
            logger.error("No repositories have been provided. Defaulting to: {}", badLink)
            return badLink
        }
        val chosenRepository = candidates.firstOrNull { isRepoHealthy(it) } ?: run {
            if (candidates.size == 1) {
                logger.error("\"{}\" could not be resolved.", candidates.first())
            } else {
                logger.error("All {} repositories could not be resolved. Defaulting to: {}", candidates.size, candidates.first())
            }
            return candidates.first()
        }
        logger.lifecycle("Using \"{}\" as the Fabric repository.", chosenRepository)
        return chosenRepository
    }
    repositories {
        maven(
            repoUrlWithFallbacks(
                listOf(
                    "https://maven.fabricmc.net",
                    "https://maven2.fabricmc.net",
                    "https://maven3.fabricmc.net"
                )
            )
        ) { name = "Fabric" }
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