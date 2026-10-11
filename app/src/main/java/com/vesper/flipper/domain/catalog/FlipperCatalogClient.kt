package com.vesper.flipper.domain.catalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class CatalogApplication(
    val alias: String,
    val versionId: String,
    val version: String,
    val currentApi: String?,
)

data class DownloadedFap(
    val alias: String,
    val version: String,
    val versionId: String,
    val sdkApi: String,
    val bytes: ByteArray,
)

/**
 * Resolves a compiled Flipper application from catalog.flipperzero.one.
 * The build URL is version id + SDK API, and the body is a FAP (ELF), not the source zip.
 */
object FlipperCatalogJson {
    const val HOST = "https://catalog.flipperzero.one"
    const val FALLBACK_RELEASE_API = "87.1"
    const val MAX_FAP_BYTES = 6 * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun applicationsUrl(): String = "$HOST/api/v0/0/application?limit=500"

    fun sdkUrl(): String = "$HOST/api/v0/0/sdk"

    fun buildUrl(versionId: String, api: String): String =
        "$HOST/api/v0/0/application/version/$versionId/build/f7/$api"

    fun isFapElf(bytes: ByteArray): Boolean =
        bytes.size > 4 &&
            bytes[0] == 0x7f.toByte() &&
            bytes[1] == 'E'.code.toByte() &&
            bytes[2] == 'L'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()

    /**
     * Map a Flipper firmware banner onto a catalog SDK API.
     * Returns null when the banner is missing or not a known release.
     */
    fun sdkApiForFirmware(firmware: String?): String? {
        if (firmware.isNullOrBlank()) return null
        val normalized = firmware.lowercase()
        if (normalized == "unknown") return null
        return when {
            normalized.contains("88.2") || normalized.contains("1.5") -> "88.2"
            normalized.contains("87.1") || normalized.contains("1.4.1") || normalized.contains("mntm-012") -> "87.1"
            normalized.contains("87.0") || normalized.contains("1.4") -> "87.0"
            normalized.contains("86.0") ||
                normalized.contains("1.3.4") ||
                normalized.contains("1.3.3") ||
                normalized.contains("1.3.2") -> "86.0"
            normalized.contains("85.0") || normalized.contains("1.3") -> "85.0"
            else -> null
        }
    }

    /**
     * Accept a device-reported SDK API string ("major.minor", e.g. "88.2") and
     * return it only when well-formed. This is the most accurate source because
     * the device states its own API regardless of firmware fork.
     */
    fun normalizeApi(api: String?): String? {
        val trimmed = api?.trim() ?: return null
        return if (trimmed.matches(Regex("""\d+\.\d+"""))) trimmed else null
    }

    fun apiMajor(api: String?): Int? = normalizeApi(api)?.substringBefore('.')?.toIntOrNull()

    fun apiMinor(api: String?): Int? = normalizeApi(api)?.substringAfter('.')?.toIntOrNull()

    /**
     * Unleashed dev (October 2026) reports API 88.x, while the public catalog's
     * newest 88.x artifact is 88.2. The loader compares major only — the minor
     * check in application_manifest.c is compiled out — so an 88.2 FAP loads
     * on 88.17 / 88.18 firmware and an 87.x FAP does not.
     */
    fun majorHintForFirmware(firmware: String?): Int? {
        val normalized = firmware?.lowercase()?.trim().orEmpty()
        if (normalized.isBlank() || normalized == "unknown") return null
        if (normalized.contains("unlshd") || normalized.contains("unleashed")) return 88
        return apiMajor(sdkApiForFirmware(firmware))
    }

    /**
     * APIs to try, exact device API first. When that build 404s, continue with
     * other published catalog APIs of the same major (closest minor at or below
     * the device, then newer).
     */
    fun compatibleApis(
        deviceApi: String?,
        firmware: String?,
        publishedApis: List<String>,
        latestReleaseApi: String?,
        appCurrentApi: String?,
    ): List<String> {
        val exact = normalizeApi(deviceApi)
        if (exact != null) {
            return pinToMajor(exact, publishedApis, latestReleaseApi, appCurrentApi)
        }
        val mapped = sdkApiForFirmware(firmware)
        if (mapped != null) {
            return pinToMajor(mapped, publishedApis, latestReleaseApi, appCurrentApi)
        }
        val major = majorHintForFirmware(firmware)
        if (major != null) {
            val ranked = sameMajorApis(major, publishedApis, latestReleaseApi, appCurrentApi, exclude = null)
            if (ranked.isNotEmpty()) return ranked
        }
        return candidateApis(firmware, latestReleaseApi, appCurrentApi)
    }

    private fun pinToMajor(
        exact: String,
        publishedApis: List<String>,
        latestReleaseApi: String?,
        appCurrentApi: String?,
    ): List<String> {
        val major = apiMajor(exact) ?: return listOf(exact)
        val minor = apiMinor(exact) ?: return listOf(exact)
        val others = sameMajorApis(major, publishedApis, latestReleaseApi, appCurrentApi, exclude = exact)
        val notNewer = others.filter { (apiMinor(it) ?: Int.MAX_VALUE) <= minor }
            .sortedByDescending { apiMinor(it) ?: -1 }
        val newer = others.filter { (apiMinor(it) ?: -1) > minor }
            .sortedBy { apiMinor(it) ?: Int.MAX_VALUE }
        return (listOf(exact) + notNewer + newer).distinct()
    }

    private fun sameMajorApis(
        major: Int,
        publishedApis: List<String>,
        latestReleaseApi: String?,
        appCurrentApi: String?,
        exclude: String?,
    ): List<String> {
        return (publishedApis + listOfNotNull(latestReleaseApi, appCurrentApi))
            .mapNotNull(::normalizeApi)
            .filter { apiMajor(it) == major && it != exclude }
            .distinct()
            .sortedByDescending { apiMinor(it) ?: -1 }
    }

    fun candidateApis(firmware: String?, latestReleaseApi: String?, currentApi: String?): List<String> {
        return listOfNotNull(
            sdkApiForFirmware(firmware),
            latestReleaseApi?.takeIf { it.isNotBlank() },
            FALLBACK_RELEASE_API,
            currentApi?.takeIf { it.isNotBlank() },
        ).distinct()
    }

    fun parseApplications(body: String): List<CatalogApplication> {
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val alias = obj.string("alias") ?: return@mapNotNull null
            val current = obj["current_version"] as? JsonObject ?: return@mapNotNull null
            val versionId = current.string("_id") ?: return@mapNotNull null
            val version = current.string("version") ?: return@mapNotNull null
            val api = (current["current_build"] as? JsonObject)
                ?.let { it["sdk"] as? JsonObject }
                ?.string("api")
            CatalogApplication(alias, versionId, version, api)
        }
    }

    fun parseSdkApis(body: String): List<String> {
        val array = json.parseToJsonElement(body) as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            obj.string("api")
        }
    }

    fun parseLatestReleaseApi(body: String): String? {
        val array = json.parseToJsonElement(body) as? JsonArray ?: return null
        return array.firstNotNullOfOrNull { element ->
            val obj = element as? JsonObject ?: return@firstNotNullOfOrNull null
            val latest = obj["is_latest_release"]?.jsonPrimitive?.booleanOrNull == true
            if (latest) obj.string("api") else null
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

class FlipperCatalogClient(
    private val httpClient: OkHttpClient,
    private val userAgent: String = "VesperFlipper/1.0 (Android)",
) {
    @Volatile private var applications: List<CatalogApplication>? = null
    @Volatile private var latestReleaseApi: String? = null
    @Volatile private var publishedSdkApis: List<String>? = null

    suspend fun downloadFap(
        alias: String,
        firmwareVersion: String?,
        deviceApi: String? = null,
    ): DownloadedFap = withContext(Dispatchers.IO) {
        val needle = alias.trim()
        if (needle.isEmpty()) throw IllegalArgumentException("Catalog alias is blank")

        val app = loadApplications().firstOrNull { it.alias.equals(needle, ignoreCase = true) }
            ?: throw IOException("Flipper catalog has no app named \"$needle\"")

        // Prefer the SDK API the device reports. Unleashed often sits on a minor
        // the catalog never built (88.17 vs catalog 88.2). The loader accepts any
        // build with the same major, so try that exact API and then the other
        // published builds of that major.
        val publishedApis = loadSdkApis()
        val apis = FlipperCatalogJson.compatibleApis(
            deviceApi = deviceApi,
            firmware = firmwareVersion,
            publishedApis = publishedApis,
            latestReleaseApi = latestReleaseApi,
            appCurrentApi = app.currentApi,
        )
        val pinnedApi = FlipperCatalogJson.normalizeApi(deviceApi)
            ?: FlipperCatalogJson.sdkApiForFirmware(firmwareVersion)
        var lastError: Exception? = null
        for (api in apis) {
            try {
                val bytes = getBytes(FlipperCatalogJson.buildUrl(app.versionId, api))
                if (bytes.size > FlipperCatalogJson.MAX_FAP_BYTES) {
                    throw IOException("FAP for ${app.alias} is too large (${bytes.size} bytes)")
                }
                if (!FlipperCatalogJson.isFapElf(bytes)) {
                    throw IOException("Catalog build for ${app.alias} at API $api was not a FAP")
                }
                return@withContext DownloadedFap(
                    alias = app.alias,
                    version = app.version,
                    versionId = app.versionId,
                    sdkApi = api,
                    bytes = bytes,
                )
            } catch (error: Exception) {
                lastError = error
            }
        }
        if (pinnedApi != null) {
            val extras = apis.filter { it != pinnedApi }
            val extraNote = if (extras.isEmpty()) {
                "The catalog has no other build with the same API major."
            } else {
                "Also tried ${extras.joinToString()} (same API major, which Unleashed accepts)."
            }
            throw IOException(
                "No ${app.alias} build is available for your firmware's SDK (API $pinnedApi). " +
                    "$extraNote ${lastError?.message ?: ""}".trim()
            )
        }
        throw IOException(
            "Could not download ${app.alias} for SDK ${apis.joinToString()}. ${lastError?.message ?: ""}".trim()
        )
    }

    private fun loadApplications(): List<CatalogApplication> {
        applications?.let { return it }
        val body = getText(FlipperCatalogJson.applicationsUrl())
        val parsed = FlipperCatalogJson.parseApplications(body)
        if (parsed.isEmpty()) throw IOException("Flipper catalog returned no applications")
        applications = parsed
        return parsed
    }

    private fun loadLatestReleaseApi(): String? {
        latestReleaseApi?.let { return it }
        loadSdkApis()
        return latestReleaseApi
    }

    private fun loadSdkApis(): List<String> {
        publishedSdkApis?.let { return it }
        return try {
            val body = getText(FlipperCatalogJson.sdkUrl())
            val apis = FlipperCatalogJson.parseSdkApis(body)
            publishedSdkApis = apis
            if (latestReleaseApi == null) {
                latestReleaseApi = FlipperCatalogJson.parseLatestReleaseApi(body)
            }
            apis
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getText(url: String): String = getBytes(url).toString(Charsets.UTF_8)

    private fun getBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.bytes() ?: throw IOException("Empty response from $url")
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from $url")
            }
            return body
        }
    }
}
