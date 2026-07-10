package hifumi.cresora

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class LocalizationCompletenessTest {

    @Test
    fun `all configured locales share the canonical translation key set`() {
        val canonicalLocale = System.getProperty("cresora.i18n.canonicalLocale", DEFAULT_CANONICAL_LOCALE)
        val locales = System.getProperty("cresora.i18n.locales", DEFAULT_LOCALES.joinToString(","))
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        assertTrue(canonicalLocale in locales, "Canonical locale '$canonicalLocale' must be included in $locales")

        val canonicalKeys = loadKeys(canonicalLocale)
        for (locale in locales - canonicalLocale) {
            val localeKeys = loadKeys(locale)
            val missing = (canonicalKeys - localeKeys).sorted()
            val unexpected = (localeKeys - canonicalKeys).sorted()

            assertTrue(missing.isEmpty(), "Locale '$locale' is missing keys: ${missing.joinToString()}")
            assertTrue(unexpected.isEmpty(), "Locale '$locale' has unexpected keys: ${unexpected.joinToString()}")
        }
    }

    private fun loadKeys(locale: String): Set<String> {
        val path = LANGUAGE_DIRECTORY.resolve("$locale.json")
        assertTrue(Files.isRegularFile(path), "Missing language file: $path")
        return Files.newBufferedReader(path).use { reader ->
            JsonParser.parseReader(reader).asJsonObject.keySet().toSet()
        }
    }

    private companion object {
        val LANGUAGE_DIRECTORY: Path = Path.of("src/main/resources/assets/cresora-utilities/lang")
        const val DEFAULT_CANONICAL_LOCALE = "en_us"
        val DEFAULT_LOCALES = listOf("en_us", "ja_jp", "zh_cn", "lzh")
    }
}
