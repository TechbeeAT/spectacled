package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ai.AiProvider
import at.techbee.spectacled.screens.core.data.ai.ClaudeModel
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import at.techbee.spectacled.theme.ThemeFont
import at.techbee.spectacled.theme.ThemeOption
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

const val APP_PREFERENCES_FILE_NAME = "app_preferences"

const val LAST_USED_CALENDAR_ID = "last_used_calendar_id"
const val LIST_SORTED_BY = "list_sorted_by"
const val LIST_SORTED_BY_ASCENDING = "list_sorted_by_ascending"
const val LIST_LAYOUT = "list_layout"
const val LIST_COLLAPSED_GROUPS = "list_collapsed_groups"
const val LIST_COLLAPSED_GROUP_TRASHBIN = "list_collapsed_group_trashbin"
const val LIST_COLLAPSED_GROUP_PINNED = "list_collapsed_group_pinned"

const val THEME_OPTION = "theme_option"
const val THEME_DYNAMIC_COLORS_ENABLED = "theme_dynamic_colors_enabled"
const val THEME_PALETTE_STYLE = "theme_palette_style"
const val THEME_AMOLED = "theme_amoled"
const val THEME_FONT = "theme_font"

const val CLAUDE_USER_API_KEY = "claude_user_api_key"
const val USER_PROXY_SERVER = "user_proxy_server"

const val AI_PROVIDER = "ai_provider"
const val CLAUDE_MODEL = "claude_model"
const val OPENAI_BASE_URL = "openai_base_url"
const val OPENAI_MODEL = "openai_model"
const val OPENAI_API_KEY = "openai_api_key"

interface UserAppPreferencesStore {

    val variant: SpectacledVariant

    fun save(key: String, value: String)
    fun saveEncrypted(key: String, value: String)
    fun load(key: String): String?
    fun loadAsFlow(key: String): Flow<String?>
    fun remove(key: String)

    /** No-op everywhere except Web, where it awaits KSafe's async key setup. */
    suspend fun awaitReady() {}

    var lastUsedCalendarId: Long?
        get() = this.load(LAST_USED_CALENDAR_ID)?.toLongOrNull()
        set(value) {
            if (value == null) this.remove(LAST_USED_CALENDAR_ID)
            else this.save(LAST_USED_CALENDAR_ID, value.toString())
        }

    var listSortedBy: ListSortedBy
        get() = this.load(LIST_SORTED_BY)?.let { savedSortedBy -> ListSortedBy.entries.find { savedSortedBy == it.name } } ?: variant.defaultListSortedBy
        set(value) { this.save(LIST_SORTED_BY, value.name) }

    var listSortedByAscending: Boolean
        get() = this.load(LIST_SORTED_BY_ASCENDING)?.toBooleanStrictOrNull() ?: false
        set(value) = this.save(LIST_SORTED_BY_ASCENDING, if(value) "true" else "false")

    var listLayout: ListLayout
        get() = this.load(LIST_LAYOUT)?.let { savedLayout -> ListLayout.entries.find { savedLayout == it.name } } ?: variant.defaultListLayout
        set(value) { this.save(LIST_LAYOUT, value.name) }

    var listCollapsedGroups: Set<String>
        get() = this.load(LIST_COLLAPSED_GROUPS)
            ?.split("|")?.toSet() ?: emptySet()
        set(value) = this.save(LIST_COLLAPSED_GROUPS, value.joinToString("|"))


    /* SETTINGS */
    var themeOption: ThemeOption
        get() = this.load(THEME_OPTION)?.let { ThemeOption.entries.find { themeOption -> themeOption.name == it } } ?: ThemeOption.SYSTEM
        set(value) = this.save(THEME_OPTION, value.name)
    fun getThemeOptionAsFlow(): Flow<ThemeOption> = this.loadAsFlow(THEME_OPTION).map { ThemeOption.fromString(it) }

    var themeDynamicColorsEnabled: Boolean
        get() = this.load(THEME_DYNAMIC_COLORS_ENABLED)?.toBooleanStrictOrNull()?: false
        set(value) = this.save(THEME_DYNAMIC_COLORS_ENABLED, value.toString())
    fun getThemeDynamicColorsEnabledAsFlow(): Flow<Boolean> = this.loadAsFlow(THEME_DYNAMIC_COLORS_ENABLED).map { it?.toBooleanStrictOrNull() ?: false }

    var themePaletteStlye: PaletteStyle
        get() = this.load(THEME_PALETTE_STYLE)?.let { PaletteStyle.entries.find { paletteStyle -> paletteStyle.name == it } } ?: PaletteStyle.Expressive
        set(value) = this.save(THEME_PALETTE_STYLE, value.name)
    fun getThemePaletteStlyeAsFlow(): Flow<PaletteStyle> = this.loadAsFlow(THEME_PALETTE_STYLE).map { name ->
        PaletteStyle.entries.find { it.name == name } ?: PaletteStyle.Expressive
    }

    var themeAmoled: Boolean
        get() = this.load(THEME_AMOLED)?.toBooleanStrictOrNull()?: false
        set(value) = this.save(THEME_AMOLED, value.toString())
    fun getThemeAmoledAsFlow(): Flow<Boolean> = this.loadAsFlow(THEME_AMOLED).map { it?.toBooleanStrictOrNull() ?: false }

    var themeFont: ThemeFont
        get() = this.load(THEME_FONT)?.let { ThemeFont.entries.find { themeFont -> themeFont.name == it } } ?: variant.defaultThemeFont
        set(value) = this.save(THEME_FONT, value.name)
    fun getThemeFontAsFlow(): Flow<ThemeFont> = this.loadAsFlow(THEME_FONT).map { name ->
        ThemeFont.entries.find { it.name == name } ?: variant.defaultThemeFont
    }

    var claudeUserApiKey: String?
        get() = this.load(CLAUDE_USER_API_KEY)?.ifEmpty { null }
        set(value) = if(value.isNullOrBlank()) this.remove(CLAUDE_USER_API_KEY) else this.saveEncrypted(CLAUDE_USER_API_KEY, value)
    fun getClaudeUserApiKeyAsFlow(): Flow<String?> = this.loadAsFlow(CLAUDE_USER_API_KEY)

    /** Anthropic model id sent on Claude requests. Defaults to [ClaudeModel.DEFAULT]. */
    var claudeModel: String
        get() = this.load(CLAUDE_MODEL) ?: ClaudeModel.DEFAULT.id
        set(value) = this.save(CLAUDE_MODEL, value)
    fun getClaudeModelAsFlow(): Flow<String> = this.loadAsFlow(CLAUDE_MODEL).map { it ?: ClaudeModel.DEFAULT.id }

    var userProxyServer: String?
        get() = this.load(USER_PROXY_SERVER)?.ifEmpty { null }
        set(value) = if(value == null) this.remove(USER_PROXY_SERVER) else this.save(USER_PROXY_SERVER, value)
    fun getUserProxyServerAsFlow(): Flow<String?> = this.loadAsFlow(USER_PROXY_SERVER)

    /** Which backend fulfils the "derive entries from text" AI feature. Defaults to [AiProvider.CLAUDE]. */
    var aiProvider: AiProvider
        get() = AiProvider.fromString(this.load(AI_PROVIDER))
        set(value) = this.save(AI_PROVIDER, value.name)
    fun getAiProviderAsFlow(): Flow<AiProvider> = this.loadAsFlow(AI_PROVIDER).map { AiProvider.fromString(it) }

    /** Root URL of a self-hosted OpenAI-compatible server (e.g. Ollama at http://localhost:11434). */
    var openAiBaseUrl: String?
        get() = this.load(OPENAI_BASE_URL)?.ifEmpty { null }
        set(value) = if(value.isNullOrBlank()) this.remove(OPENAI_BASE_URL) else this.save(OPENAI_BASE_URL, value)
    fun getOpenAiBaseUrlAsFlow(): Flow<String?> = this.loadAsFlow(OPENAI_BASE_URL)

    /** Served model name for the OpenAI-compatible endpoint (e.g. "llama3.2"). */
    var openAiModel: String?
        get() = this.load(OPENAI_MODEL)?.ifEmpty { null }
        set(value) = if(value.isNullOrBlank()) this.remove(OPENAI_MODEL) else this.save(OPENAI_MODEL, value)
    fun getOpenAiModelAsFlow(): Flow<String?> = this.loadAsFlow(OPENAI_MODEL)

    /** Optional bearer token for the OpenAI-compatible endpoint (Ollama needs none). Stored encrypted. */
    var openAiApiKey: String?
        get() = this.load(OPENAI_API_KEY)?.ifEmpty { null }
        set(value) = if(value.isNullOrBlank()) this.remove(OPENAI_API_KEY) else this.saveEncrypted(OPENAI_API_KEY, value)
    fun getOpenAiApiKeyAsFlow(): Flow<String?> = this.loadAsFlow(OPENAI_API_KEY)


    companion object {
        fun getEmptyPreferenceStoreForPreview(variant: SpectacledVariant = SpectacledVariant.NOTES) = object: UserAppPreferencesStore {
            override val variant: SpectacledVariant = variant
            override fun save(key: String, value: String) {}
            override fun saveEncrypted(key: String, value: String) {}
            override fun load(key: String): String? {return null }
            override fun loadAsFlow(key: String): Flow<String?> { return flowOf(null) }
            override fun remove(key: String) {}
        }
    }
}

expect class PlatformUserAppPreferencesStore: UserAppPreferencesStore {
    override val variant: SpectacledVariant
    override fun save(key: String, value: String)
    override fun saveEncrypted(key: String, value: String)
    override fun load(key: String): String?
    override fun loadAsFlow(key: String): Flow<String?>
    override fun remove(key: String)
}
