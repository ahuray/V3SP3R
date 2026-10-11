package com.vesper.flipper.ai

import com.vesper.flipper.data.SettingsStore
import com.vesper.flipper.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Comprehensive edge-case tests for AI tool-call JSON parsing.
 *
 * Every case drives the real production parser
 * [OpenRouterClient.parseCommandDetailed] / [OpenRouterClient.parseCommand] so the
 * tests cannot drift away from the shipped behavior. The client is constructed with a
 * mocked [SettingsStore]; the parser never touches it, so no stubbing is required.
 *
 * Inputs that omit `justification` / `expected_effect` deliberately bypass the strict
 * kotlinx.serialization fast-path and exercise the manual fallback parser (alias
 * resolution, shape coercion, required-arg validation).
 *
 * Groups:
 *  1. Strict-path: well-formed JSON that decodes via kotlinx.serialization directly
 *  2. Action alias resolution
 *  3. Args shape variations (string-wrapped, array-wrapped, missing, empty)
 *  4. Field alias coverage for CommandArgs properties
 *  5. Root-level field fallback
 *  6. Error cases (totally broken JSON, wrong types, missing action)
 *  7. `parameters` key as an alias for `args`
 *  8. ArgumentsAsStringSerializer coverage
 *  9. install_faphub_app field mapping
 * 10. End-to-end serializer round-trips
 */
class CommandParsingEdgeCaseTest {

    /** The parser is pure with respect to settings, so a bare mock is sufficient. */
    private val client = OpenRouterClient(mock<SettingsStore>())

    /** Serializer-only tests (sections 8 & 10) exercise kotlinx.serialization directly. */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private fun parseOk(arguments: String): ExecuteCommand {
        val result = client.parseCommandDetailed(arguments)
        assertNull("Expected successful parse but got error: ${result.error}", result.error)
        assertNotNull("Expected a parsed command for: $arguments", result.command)
        return result.command!!
    }

    private fun parseError(arguments: String): String {
        val result = client.parseCommandDetailed(arguments)
        assertNull("Expected parse failure but got command: ${result.command}", result.command)
        assertNotNull("Expected an error message for: $arguments", result.error)
        return result.error!!
    }

    // ═══════════════════════════════════════════════════════
    // 1. STRICT-PATH — well-formed JSON
    // ═══════════════════════════════════════════════════════

    @Test
    fun `strict path - perfect schema with all fields`() {
        val input = """
        {
            "action": "write_file",
            "args": {
                "path": "/ext/test.ir",
                "content": "Filetype: IR\nVersion: 1"
            },
            "justification": "Creating IR file",
            "expected_effect": "Write file to Flipper"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.WRITE_FILE, cmd.action)
        assertEquals("/ext/test.ir", cmd.args.path)
        assertEquals("Filetype: IR\nVersion: 1", cmd.args.content)
    }

    @Test
    fun `strict path - browse_repo with all fields`() {
        val input = """
        {
            "action": "browse_repo",
            "args": {
                "repo_id": "irdb",
                "sub_path": "TVs/Samsung"
            },
            "justification": "Browse IR database",
            "expected_effect": "List Samsung TV remotes"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.BROWSE_REPO, cmd.action)
        assertEquals("irdb", cmd.args.repoId)
        assertEquals("TVs/Samsung", cmd.args.subPath)
    }

    @Test
    fun `strict path - download_resource with download_url`() {
        val input = """
        {
            "action": "download_resource",
            "args": {
                "download_url": "https://raw.githubusercontent.com/foo/bar/main/remote.ir",
                "path": "/ext/infrared/remote.ir"
            },
            "justification": "Download IR remote",
            "expected_effect": "Save remote file"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.DOWNLOAD_RESOURCE, cmd.action)
        assertEquals("https://raw.githubusercontent.com/foo/bar/main/remote.ir", cmd.args.downloadUrl)
        assertEquals("/ext/infrared/remote.ir", cmd.args.path)
    }

    @Test
    fun `strict path - github_search with search_scope`() {
        val input = """
        {
            "action": "github_search",
            "args": {
                "command": "Samsung TV remote extension:ir",
                "search_scope": "code"
            },
            "justification": "Find Samsung TV IR files",
            "expected_effect": "Return GitHub search results"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.GITHUB_SEARCH, cmd.action)
        assertEquals("Samsung TV remote extension:ir", cmd.args.command)
        assertEquals("code", cmd.args.searchScope)
    }

    @Test
    fun `strict path - request_photo with photo_prompt`() {
        val input = """
        {
            "action": "request_photo",
            "args": {
                "photo_prompt": "Identify the TV brand and model"
            },
            "justification": "Need to identify device",
            "expected_effect": "Capture photo from glasses"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.REQUEST_PHOTO, cmd.action)
        assertEquals("Identify the TV brand and model", cmd.args.photoPrompt)
    }

    @Test
    fun `strict path - led_control with RGB values`() {
        val input = """
        {
            "action": "led_control",
            "args": {
                "red": 255,
                "green": 128,
                "blue": 0
            },
            "justification": "Set LED color",
            "expected_effect": "LED turns orange"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.LED_CONTROL, cmd.action)
        assertEquals(255, cmd.args.red)
        assertEquals(128, cmd.args.green)
        assertEquals(0, cmd.args.blue)
    }

    @Test
    fun `strict path - empty args object for actions with no required fields`() {
        val input = """
        {
            "action": "get_device_info",
            "args": {},
            "justification": "Check device",
            "expected_effect": "Return device info"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.GET_DEVICE_INFO, cmd.action)
    }

    // ═══════════════════════════════════════════════════════
    // 2. ACTION ALIAS RESOLUTION via the fallback parser
    // ═══════════════════════════════════════════════════════

    @Test
    fun `action alias - camelCase forgePayload normalizes to forge_payload`() {
        val cmd = parseOk("""{"action":"forgePayload","args":{"prompt":"craft a payload"}}""")
        assertEquals(CommandAction.FORGE_PAYLOAD, cmd.action)
    }

    @Test
    fun `action alias - executeCommand maps to execute_cli`() {
        val cmd = parseOk("""{"action":"executeCommand","args":{"command":"storage info"}}""")
        assertEquals(CommandAction.EXECUTE_CLI, cmd.action)
    }

    @Test
    fun `action alias - hyphenated and spaced forms normalize correctly`() {
        val variants = listOf("list-directory", "LIST DIRECTORY", "list_directory", "List-Directory", "  list_directory  ")
        for (variant in variants) {
            val cmd = parseOk("""{"action":"$variant","args":{}}""")
            assertEquals("Input '$variant' should resolve to LIST_DIRECTORY", CommandAction.LIST_DIRECTORY, cmd.action)
        }
    }

    // ═══════════════════════════════════════════════════════
    // 3. ARGS SHAPE VARIATIONS
    // ═══════════════════════════════════════════════════════

    @Test
    fun `fallback - args as stringified JSON object`() {
        // Some models return args as a JSON string: "args": "{\"path\":\"/ext\"}"
        val cmd = parseOk("""{"action":"list_directory","args":"{\"path\":\"/ext/subghz\"}"}""")
        assertEquals(CommandAction.LIST_DIRECTORY, cmd.action)
        assertEquals("/ext/subghz", cmd.args.path)
    }

    @Test
    fun `fallback - args wrapped in single-element array`() {
        // Some models: "args": [{"path": "/ext"}]
        val cmd = parseOk("""{"action":"read_file","args":[{"path":"/ext/nfc"}]}""")
        assertEquals(CommandAction.READ_FILE, cmd.action)
        assertEquals("/ext/nfc", cmd.args.path)
    }

    @Test
    fun `fallback - args as empty array yields empty object`() {
        val cmd = parseOk("""{"action":"get_device_info","args":[]}""")
        assertEquals(CommandAction.GET_DEVICE_INFO, cmd.action)
    }

    @Test
    fun `fallback - null args yields empty object`() {
        val cmd = parseOk("""{"action":"get_device_info"}""")
        assertEquals(CommandAction.GET_DEVICE_INFO, cmd.action)
    }

    @Test
    fun `fallback - empty string args yields empty object`() {
        val cmd = parseOk("""{"action":"get_storage_info","args":""}""")
        assertEquals(CommandAction.GET_STORAGE_INFO, cmd.action)
    }

    @Test
    fun `fallback - top-level JSON wrapped in single-element array`() {
        // Some models: [{"action":"list_directory","args":{"path":"/ext"}}]
        val cmd = parseOk("""[{"action":"list_directory","args":{"path":"/ext"}}]""")
        assertEquals(CommandAction.LIST_DIRECTORY, cmd.action)
        assertEquals("/ext", cmd.args.path)
    }

    // ═══════════════════════════════════════════════════════
    // 4. FIELD ALIAS COVERAGE
    // ═══════════════════════════════════════════════════════

    @Test
    fun `field alias - command can be extracted from cmd key`() {
        val cmd = parseOk("""{"action":"execute_cli","args":{"cmd":"storage list /ext"}}""")
        assertEquals("storage list /ext", cmd.args.command)
    }

    @Test
    fun `field alias - path can be extracted from file_path key`() {
        val cmd = parseOk("""{"action":"read_file","args":{"file_path":"/ext/test.sub"}}""")
        assertEquals("/ext/test.sub", cmd.args.path)
    }

    @Test
    fun `field alias - destination_path can be extracted from dest key`() {
        val cmd = parseOk("""{"action":"move","args":{"path":"/ext/a.sub","dest":"/ext/backup/a.sub"}}""")
        assertEquals("/ext/backup/a.sub", cmd.args.destinationPath)
    }

    @Test
    fun `field alias - download_url separated from content`() {
        // Critical: download_url must map to downloadUrl, NOT content.
        val cmd = parseOk(
            """{"action":"download_resource","args":{"download_url":"https://example.com/file.ir","content":"some text content","path":"/ext/infrared/file.ir"}}"""
        )
        assertEquals("https://example.com/file.ir", cmd.args.downloadUrl)
        assertEquals("some text content", cmd.args.content)
    }

    @Test
    fun `field alias - repo_id from repoId camelCase`() {
        val cmd = parseOk("""{"action":"browse_repo","args":{"repoId":"irdb"}}""")
        assertEquals("irdb", cmd.args.repoId)
    }

    @Test
    fun `field alias - sub_path from subPath camelCase`() {
        val cmd = parseOk("""{"action":"browse_repo","args":{"repo_id":"irdb","subPath":"TVs/LG"}}""")
        assertEquals("TVs/LG", cmd.args.subPath)
    }

    @Test
    fun `field alias - search_scope from scope shorthand`() {
        val cmd = parseOk("""{"action":"github_search","args":{"command":"query","scope":"repositories"}}""")
        assertEquals("repositories", cmd.args.searchScope)
    }

    @Test
    fun `field alias - photo_prompt from photoPrompt camelCase`() {
        val cmd = parseOk("""{"action":"request_photo","args":{"photoPrompt":"What TV is this?"}}""")
        assertEquals("What TV is this?", cmd.args.photoPrompt)
    }

    @Test
    fun `field alias - boolean recursive from string true`() {
        val cmd = parseOk("""{"action":"delete","args":{"path":"/ext/dir","recursive":"true"}}""")
        assertTrue(cmd.args.recursive)
    }

    @Test
    fun `field alias - boolean enabled from string 1`() {
        val cmd = parseOk("""{"action":"vibro_control","args":{"enabled":"1"}}""")
        assertEquals(true, cmd.args.enabled)
    }

    @Test
    fun `field alias - integer red from string`() {
        val cmd = parseOk("""{"action":"led_control","args":{"red":"255"}}""")
        assertEquals(255, cmd.args.red)
    }

    @Test
    fun `field alias - frequency from string`() {
        val cmd = parseOk("""{"action":"subghz_transmit","args":{"path":"/ext/subghz/x.sub","freq":"433920000"}}""")
        assertEquals(433920000L, cmd.args.frequency)
    }

    // ═══════════════════════════════════════════════════════
    // 5. FALLBACK FIELD LOOKUP IN ROOT (not just args)
    // ═══════════════════════════════════════════════════════

    @Test
    fun `fallback - field found at root level when missing from args`() {
        // Some models put fields at root instead of inside args:
        // {"action":"read_file","path":"/ext/test","args":{}}
        val cmd = parseOk("""{"action":"read_file","path":"/ext/test.sub","args":{}}""")
        assertEquals("/ext/test.sub", cmd.args.path)
    }

    // ═══════════════════════════════════════════════════════
    // 6. ERROR CASES
    // ═══════════════════════════════════════════════════════

    @Test
    fun `error - completely broken JSON`() {
        val error = parseError("this is not json at all")
        assertTrue(error.isNotBlank())
    }

    @Test
    fun `error - JSON object missing action field`() {
        val error = parseError("""{"args":{"path":"/ext"}}""")
        assertTrue("Error should mention the action field: $error", error.contains("action"))
    }

    @Test
    fun `error - unsupported action value`() {
        val error = parseError("""{"action":"do_something_unknown","args":{}}""")
        assertTrue("Error should flag the unsupported action: $error", error.contains("Unsupported action"))
    }

    @Test
    fun `error - args is a multi-element array`() {
        val error = parseError("""{"action":"list_directory","args":[{"path":"/ext/a"},{"path":"/ext/b"}]}""")
        assertTrue("Error should mention args: $error", error.contains("args"))
    }

    @Test
    fun `error - args is a bare number`() {
        val error = parseError("""{"action":"list_directory","args":42}""")
        assertTrue("Error should mention args: $error", error.contains("args"))
    }

    // ═══════════════════════════════════════════════════════
    // 7. PARAMETERS ALIAS KEY
    // ═══════════════════════════════════════════════════════

    @Test
    fun `fallback - parameters key used instead of args`() {
        val cmd = parseOk("""{"action":"read_file","parameters":{"path":"/ext/infrared/tv.ir"}}""")
        assertEquals("/ext/infrared/tv.ir", cmd.args.path)
    }

    // ═══════════════════════════════════════════════════════
    // 8. ArgumentsAsStringSerializer coverage (serializer-level)
    // ═══════════════════════════════════════════════════════

    @Test
    fun `ArgumentsAsString - string arguments deserialize correctly`() {
        // Standard form: arguments is a JSON string
        val input = """{"id":"call_123","type":"function","function":{"name":"execute_command","arguments":"{\"action\":\"list_directory\",\"args\":{\"path\":\"/ext\"}}"}}"""
        val tc = json.decodeFromString<OpenRouterToolCall>(input)
        assertEquals("call_123", tc.id)
        assertEquals("execute_command", tc.function.name)
        assertTrue(tc.function.arguments.contains("list_directory"))
    }

    @Test
    fun `ArgumentsAsString - object arguments deserialize to string`() {
        // Non-standard form: arguments is a raw JSON object (not string-encoded)
        val input = """{"id":"call_456","type":"function","function":{"name":"execute_command","arguments":{"action":"read_file","args":{"path":"/ext/test"}}}}"""
        val tc = json.decodeFromString<OpenRouterToolCall>(input)
        assertEquals("call_456", tc.id)
        assertTrue(tc.function.arguments.contains("read_file"))
        assertTrue(tc.function.arguments.contains("/ext/test"))
    }

    // ═══════════════════════════════════════════════════════
    // 9. INSTALL FAPHUB with download_url properly separated
    // ═══════════════════════════════════════════════════════

    @Test
    fun `install_faphub_app with command and download_url`() {
        // Strict path: "command" maps to CommandArgs.command, "download_url" maps to downloadUrl.
        val input = """
        {
            "action": "install_faphub_app",
            "args": {
                "command": "wifi_marauder",
                "download_url": "https://example.com/marauder.fap"
            },
            "justification": "Install WiFi Marauder",
            "expected_effect": "App installed"
        }
        """.trimIndent()

        val cmd = parseOk(input)
        assertEquals(CommandAction.INSTALL_FAPHUB_APP, cmd.action)
        assertEquals("wifi_marauder", cmd.args.command)
        assertEquals("https://example.com/marauder.fap", cmd.args.downloadUrl)
    }

    @Test
    fun `install_faphub_app fallback - app_id alias maps to command`() {
        // In the fallback parser, "app_id" is an alias for "command".
        val cmd = parseOk("""{"action":"install_faphub_app","args":{"app_id":"wifi_marauder"}}""")
        assertEquals(CommandAction.INSTALL_FAPHUB_APP, cmd.action)
        assertEquals("wifi_marauder", cmd.args.command)
    }

    // ═══════════════════════════════════════════════════════
    // 10. FULL END-TO-END SERIALIZER ROUND-TRIPS
    // ═══════════════════════════════════════════════════════

    @Test
    fun `every CommandAction can be deserialized from its SerialName`() {
        // Ensure every enum value has a valid SerialName that round-trips
        for (action in CommandAction.entries) {
            val serialized = json.encodeToString(CommandAction.serializer(), action)
            val deserialized = json.decodeFromString(CommandAction.serializer(), serialized)
            assertEquals("Action ${action.name} should round-trip", action, deserialized)
        }
    }

    @Test
    fun `CommandArgs with all fields set round-trips`() {
        val args = CommandArgs(
            command = "test_cmd",
            path = "/ext/test",
            destinationPath = "/ext/dest",
            content = "file content",
            newName = "renamed.txt",
            recursive = true,
            artifactType = "fap",
            artifactData = "base64data",
            prompt = "do something",
            resourceType = "IR_REMOTE",
            runbookId = "link_health",
            payloadType = "BAD_USB",
            filter = "SUB_GHZ",
            appName = "Sub-GHz",
            appArgs = "-v",
            frequency = 433920000L,
            protocol = "Princeton",
            address = "0x1234",
            signalName = "Power",
            enabled = true,
            red = 255,
            green = 128,
            blue = 64,
            repoId = "irdb",
            subPath = "TVs/LG",
            downloadUrl = "https://example.com/file.ir",
            searchScope = "code",
            photoPrompt = "What device is this?"
        )

        val serialized = json.encodeToString(CommandArgs.serializer(), args)
        val deserialized = json.decodeFromString(CommandArgs.serializer(), serialized)

        assertEquals(args.command, deserialized.command)
        assertEquals(args.path, deserialized.path)
        assertEquals(args.destinationPath, deserialized.destinationPath)
        assertEquals(args.content, deserialized.content)
        assertEquals(args.downloadUrl, deserialized.downloadUrl)
        assertEquals(args.repoId, deserialized.repoId)
        assertEquals(args.subPath, deserialized.subPath)
        assertEquals(args.searchScope, deserialized.searchScope)
        assertEquals(args.photoPrompt, deserialized.photoPrompt)
        assertEquals(args.frequency, deserialized.frequency)
        assertEquals(args.red, deserialized.red)
        assertEquals(args.green, deserialized.green)
        assertEquals(args.blue, deserialized.blue)
        assertEquals(args.enabled, deserialized.enabled)
    }
}
