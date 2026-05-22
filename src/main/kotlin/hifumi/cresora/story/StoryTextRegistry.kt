package hifumi.cresora.story
import hifumi.cresora.CreSoraUtilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.util.Locale

object StoryTextRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/story_texts.json"
    private const val BUILTIN_FALLBACK_LOCALE = "ja_jp"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/story-texts")

    @Volatile
    private var fallbackLocale: String = BUILTIN_FALLBACK_LOCALE

    @Volatile
    private var textsByLocale: Map<String, Map<String, String>> = emptyMap()

    fun init() {
        applyBundle(defaultFallbackLocale = BUILTIN_FALLBACK_LOCALE, bundle = defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle.first, bundle.second)
                logger.info("Loaded story texts from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load story texts from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun resolve(locale: String?, textId: String): String {
        val normalizedLocale = normalizeLocale(locale)
        val localeCandidates = linkedSetOf(
            normalizedLocale,
            normalizedLocale.substringBefore('_'),
            fallbackLocale,
            BUILTIN_FALLBACK_LOCALE
        )
        for (candidate in localeCandidates) {
            val resolved = textsByLocale[candidate]?.get(textId)
            if (!resolved.isNullOrBlank()) {
                return resolved
            }
        }
        for (entries in textsByLocale.values) {
            val resolved = entries[textId]
            if (!resolved.isNullOrBlank()) {
                return resolved
            }
        }
        return textId
    }

    fun normalizeLocale(locale: String?): String {
        return locale
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('-', '_')
            ?.ifBlank { null }
            ?: fallbackLocale
    }

    fun resolvePlayerLocale(player: ServerPlayerEntity?): String {
        val actualPlayer = player ?: return normalizeLocale(null)
        val options = runCatching {
            actualPlayer.javaClass.methods
                .firstOrNull { it.name == "getClientOptions" || it.name == "clientOptions" }
                ?.invoke(actualPlayer)
        }.getOrNull() ?: return normalizeLocale(null)

        val language = runCatching {
            options.javaClass.methods
                .firstOrNull { it.name == "language" || it.name == "getLanguage" }
                ?.invoke(options)
                ?.toString()
        }.getOrNull()

        return normalizeLocale(language)
    }

    fun chapterTitle(locale: String?, chapter: StoryChapterDefinition): String {
        val titleTextId = chapter.titleTextId ?: return chapter.displayName
        return resolve(locale, titleTextId)
    }

    fun chapterLabel(locale: String?, chapter: StoryChapterDefinition): String {
        val title = chapterTitle(locale, chapter)
        if (title == chapter.displayName || title.isBlank()) {
            return chapter.displayName
        }
        val normalizedLocale = normalizeLocale(locale)
        return if (normalizedLocale == "ja_jp" || normalizedLocale == "zh_cn" || normalizedLocale == "lzh") {
            "${chapter.displayName}「$title」"
        } else {
            "${chapter.displayName}: $title"
        }
    }

    private fun loadBundledContent(): Pair<String, Map<String, Map<String, String>>> {
        val stream = StoryTextRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val root = JsonParser.parseReader(reader).asJsonObject
            val loadedFallbackLocale = normalizeLocale(root.get("fallbackLocale")?.asString ?: BUILTIN_FALLBACK_LOCALE)
            val textsObject = root.getAsJsonObject("texts")
            return loadedFallbackLocale to parseTexts(textsObject)
        }
    }

    private fun parseTexts(textsObject: JsonObject): Map<String, Map<String, String>> {
        val result = linkedMapOf<String, Map<String, String>>()
        for ((localeKey, localeValue) in textsObject.entrySet()) {
            val normalizedLocale = normalizeLocale(localeKey)
            val entryObject = localeValue.asJsonObject
            val entries = linkedMapOf<String, String>()
            for ((textId, textValue) in entryObject.entrySet()) {
                entries[textId] = textValue.asString
            }
            result[normalizedLocale] = entries
        }
        return result
    }

    private fun applyBundle(defaultFallbackLocale: String, bundle: Map<String, Map<String, String>>) {
        require(bundle.isNotEmpty()) { "Story text bundle must contain at least one locale" }
        val normalizedBundle = linkedMapOf<String, Map<String, String>>()
        for ((locale, entries) in bundle) {
            val normalizedLocale = normalizeLocale(locale)
            require(entries.isNotEmpty()) { "Story text locale '$normalizedLocale' must contain at least one entry" }
            for ((textId, value) in entries) {
                require(textId.isNotBlank()) { "Story text id must not be blank" }
                require(value.isNotBlank()) { "Story text '$textId' in locale '$normalizedLocale' must not be blank" }
            }
            normalizedBundle[normalizedLocale] = LinkedHashMap(entries)
        }
        val resolvedFallbackLocale = normalizeLocale(defaultFallbackLocale)
        fallbackLocale = if (normalizedBundle.containsKey(resolvedFallbackLocale)) resolvedFallbackLocale else normalizedBundle.keys.first()
        textsByLocale = normalizedBundle
    }

    private fun defaultBundle(): Map<String, Map<String, String>> {
        return linkedMapOf(
            "ja_jp" to linkedMapOf(
                "story.cresora.chapter.0_0.title" to "微かな囁き",
                "story.cresora.chapter.0_0a.title" to "円舞曲の鍛房",
                "story.cresora.chapter.0_0b.title" to "仮面舞踏への回廊",
                "story.cresora.chapter.0_0.speaker" to "???",
                "story.cresora.chapter.0_0.pre_0" to "覚えておけ、■■■■。どんなことがあろうと、『希望』は絶対に見失わないように。",
                "story.cresora.chapter.0_0.post_0" to "たとえ『■■』を切り裂くような...苦痛に遭っても。",
                "story.cresora.chapter.0_1.title" to "チューナーの憶測",
                "story.cresora.chapter.0_1a.title" to "信用試練場",
                "story.cresora.chapter.0_1.speaker.unknown" to "？？？",
                "story.cresora.chapter.0_1.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_1.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_1.pre_0" to "川のせせらぎ。そして、虫の鳴き声。",
                "story.cresora.chapter.0_1.pre_1" to "見てごらん、この独特な『残響』を。",
                "story.cresora.chapter.0_1.pre_2" to "ぼんやりした意識がだんだんと覚める。美しいけれど、どこか悲しい顔をしている少女が、自分を見つめていた。",
                "story.cresora.chapter.0_1.pre_3" to "あなたは誰だ？",
                "story.cresora.chapter.0_1.pre_4" to "見渡す限り、麗しき景色で埋め尽くされた川の水辺だった。こんなにも美しい景色は、これまで一回も見たことがなかった。",
                "story.cresora.chapter.0_1.pre_5" to "ここはどこなのか、見覚えもないようだね。",
                "story.cresora.chapter.0_1.pre_6" to "確かにそうだ。これまで何回も■■■（この景色を見た覚えがあるのに）",
                "story.cresora.chapter.0_1.pre_7" to "なぜか考えがまとまらない。一つわかるのは、ここは「現実世界」じゃない空間だ。なぜなら、■■■",
                "story.cresora.chapter.0_1.pre_8" to "時間はもうあまり多くはないね。私はLumine、このCreSoraという世界での『チューナー』よ。",
                "story.cresora.chapter.0_1.pre_9" to "この世界に姿を現したのは、きっとそれなりの原因があるはずよ。私たち「チューナー」は、あなたのような道を迷った「独奏者《ソリスト》」の案内役を果たしているの。",
                "story.cresora.chapter.0_1.pre_10" to "話ばかりしてもわかってくれないからね。一番手っ取り早い方法で、CreSoraの『ルール』を見せるわ。",
                "story.cresora.chapter.0_1.pre_11" to "さあ、ついてきて、『ソリスト』。あなたの演奏が、いずれCreSoraの『謎』を解けることを願って。",
                "story.cresora.chapter.0_1.pre_12" to "嘗て奏者有り。其の音律、絶えざること縷の如し。",
                "story.cresora.chapter.0_1.pre_13" to "其は旅に出た。其の旅が、いずれ其れ自身の「ためらい」を紐解くことを願わん。",
                "story.cresora.chapter.0_1.hint_0" to "チュートリアル武器を一時配布しました。戦闘終了後に回収されます。",
                "story.cresora.chapter.0_1.hint_1" to "メインハンドに持って右クリックすると、『音律エコースキル』を発動できます。",
                "story.cresora.chapter.0_1.hint_2" to "『湖畔を歩む』は高火力、『円舞曲のメロディ』はシールド向きです。",
                "story.cresora.chapter.0_1.post_0" to "エコーウェポン（武器）、そして『残響』。",
                "story.cresora.chapter.0_1.post_1" to "どれも目新しいものばかりだったが、意外と妙に身にしみる。",
                "story.cresora.chapter.0_1.post_2" to "それは...CreSoraの『音律エコー』に親和性が高かったかもしれないね。",
                "story.cresora.chapter.0_1.post_3" to "少女は話し終わると、急に「あっ」って唸っては、一瞬の表情の変化を見せた。",
                "story.cresora.chapter.0_1.post_4" to "もしくは...■■■（これはもうはじめてじゃないとか）",
                "story.cresora.chapter.0_1.post_5" to "！！ もしかして...!",
                "story.cresora.chapter.0_1.post_6" to "あ、なんでもないわ。念の為、それは忘れてください。何の根拠もない憶測でしかないわ。『ソリスト』さん。そして...",
                "story.cresora.chapter.0_1.post_7" to "たとえこれが本当に真実でも、それはあなたの手で解き明かすべき。ネタバレは、よくないですよ。",
                "story.cresora.chapter.0_1.post_8" to "スッ。さきほどのパニック。驚愕。それにあの「憶測」。すべてはきれいに脳から消し去ったような感じだった。まるで、存在すらしていなかったように。",
                "story.cresora.chapter.0_1.post_9" to "これも、『音律エコー』の力...",
                "story.cresora.chapter.0_1.post_10" to "後ほどわかる。これはすべて『ソリスト』さんのためですよ。",
                "story.cresora.chapter.0_1.post_11" to "『チューナー』は、そんなに『音律エコー』の権限が強かったのか。でもなぜ、私の記憶をいじる...？私は本当に、チューナーを信用すべきだろうか。この世界は、一体何なんだ。",
                "story.cresora.chapter.0_2.title" to "狂奏",
                "story.cresora.chapter.d0_1.title" to "湖畔の聖域",
                "story.cresora.chapter.d0_2.title" to "雛形の書庫",
                "story.cresora.chapter.0_2.speaker.oldman" to "？？？",
                "story.cresora.chapter.0_2.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_2.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_2.pre_0" to "逃げろ！",
                "story.cresora.chapter.0_2.pre_1" to "遠くまで、とりあえず早く！",
                "story.cresora.chapter.0_2.pre_2" to "得も言えぬ違和感と相まって体を染み渡る恐怖に駆られ、思わず川沿いをひた走っていた。",
                "story.cresora.chapter.0_2.pre_3" to "気がつけば、川の流れ行く先は、あたかも海のように広がっていた湖でした。",
                "story.cresora.chapter.0_2.pre_4" to "おい！若造！お前はソリストか？",
                "story.cresora.chapter.0_2.pre_5" to "！",
                "story.cresora.chapter.0_2.pre_6" to "すまん、驚かせたな。ソリスト。悪いが、ちょっと手伝ってくれないか？",
                "story.cresora.chapter.0_2.pre_7" to "ここはまもなくコンサートをやる予定じゃのう",
                "story.cresora.chapter.0_2.pre_8" to "突如現れた年配者の指差す先を目で追ったら、そこにはゾンビとスケルトンの群れしかなかった。",
                "story.cresora.chapter.0_2.pre_9" to "おじいさん、それはモンスターですよ",
                "story.cresora.chapter.0_2.pre_10" to "何を言っとる、若造め。これは立派なコンサートの観衆じゃ。ほら、コンサートはもう始まっている。",
                "story.cresora.chapter.0_2.pre_11" to "え、出演者は、私？",
                "story.cresora.chapter.0_2.pre_12" to "こうして、ゾンビとスケルトンのうめき声の中で、「コンサート」が幕を開けたのだった。",
                "story.cresora.chapter.0_2.hint_0" to "最大Lvの『円舞曲のメロディ』を一時配布しました。戦闘終了後に回収されます。",
                "story.cresora.chapter.0_2.hint_1" to "このスケルトンは被ダメージ軽減100%かつ確定ダメージ無効。倒す必要はありません。",
                "story.cresora.chapter.0_2.hint_2" to "右クリックでシールドを張り、残り45秒のカウントダウンが尽きるまで生き延びてください。",
                "story.cresora.chapter.0_2.post_0" to "チッ、手強い！",
                "story.cresora.chapter.0_2.post_1" to "ソリストさん、もうちょっと持ちこたえなさい！",
                "story.cresora.chapter.0_2.post_2" to "えっ！",
                "story.cresora.chapter.0_2.post_3" to "後ろを見たら、そこには心配顔のLumineが立ち尽くしていた。彼女が手持ちのウェポンをかざすと、さっきまで無傷だったスケルトンが溶けるように残骸と化した。",
                "story.cresora.chapter.0_2.post_4" to "ソリストさんったら、勝手に走り回ったら困るよ",
                "story.cresora.chapter.0_2.post_5" to "Lumine、一つ聞いてもいい？",
                "story.cresora.chapter.0_2.post_6" to "はい？",
                "story.cresora.chapter.0_2.post_7" to "もちろんですよ。なぜなら、「■■■■」の意味は、「■■■」だから。",
                "story.cresora.chapter.0_2.post_8" to "気がつけば、彼女はもう答え終わった。頑張って思い出そうとしたが、さきほど自分がした質問さえ、霧散してしまい、やはり何も出なかった。",
                "story.cresora.chapter.0_2.post_9" to "そして、よく見ると、Lumineの表情はいつのまにか憐れみを帯びたものへと変わっていた。"
            ),
            "en_us" to linkedMapOf(
                "story.cresora.chapter.0_0.title" to "A Faint Whisper",
                "story.cresora.chapter.0_0a.title" to "Rondo Forge",
                "story.cresora.chapter.0_0b.title" to "Masquerade Passage",
                "story.cresora.chapter.0_0.speaker" to "???",
                "story.cresora.chapter.0_0.pre_0" to "Remember this, ■■■■. No matter what happens, never lose sight of 'hope.'",
                "story.cresora.chapter.0_0.post_0" to "Even if you are struck by pain sharp enough to tear through '■■' itself.",
                "story.cresora.chapter.0_1.title" to "The Tuner's Speculation",
                "story.cresora.chapter.0_1a.title" to "Credit Drill",
                "story.cresora.chapter.0_1.speaker.unknown" to "???",
                "story.cresora.chapter.0_1.speaker.player" to "<Player>",
                "story.cresora.chapter.0_1.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_1.pre_0" to "The murmur of the river. And the song of insects.",
                "story.cresora.chapter.0_1.pre_1" to "Look closely at this peculiar 'Echo.'",
                "story.cresora.chapter.0_1.pre_2" to "Your hazy consciousness gradually clears. A girl, beautiful yet wearing a strangely sorrowful expression, is staring at you.",
                "story.cresora.chapter.0_1.pre_3" to "Who are you?",
                "story.cresora.chapter.0_1.pre_4" to "As far as the eye could see, it was a riverside wrapped in exquisite scenery. You had never seen a landscape this beautiful before.",
                "story.cresora.chapter.0_1.pre_5" to "So you do not know where this is. It seems unfamiliar to you.",
                "story.cresora.chapter.0_1.pre_6" to "That was true. Even though ■■■ (I feel as if I have seen this scenery many times before).",
                "story.cresora.chapter.0_1.pre_7" to "For some reason your thoughts refuse to settle. One thing alone is clear: this is not the 'real world.' Because, ■■■",
                "story.cresora.chapter.0_1.pre_8" to "There is not much time. I am Lumine, a 'Tuner' in this world called CreSora.",
                "story.cresora.chapter.0_1.pre_9" to "There must be a reason you appeared in this world. We 'Tuners' guide wandering 'Soloists' such as you.",
                "story.cresora.chapter.0_1.pre_10" to "Words alone will not make you understand. The quickest way is to show you the 'rules' of CreSora directly.",
                "story.cresora.chapter.0_1.pre_11" to "Come, then, Soloist. I hope your performance will one day unravel the 'mystery' of CreSora.",
                "story.cresora.chapter.0_1.pre_12" to "Once there was a performer. Their music endured like an unbroken thread.",
                "story.cresora.chapter.0_1.pre_13" to "And so they set out on a journey. May that journey one day untangle their own hesitation.",
                "story.cresora.chapter.0_1.hint_0" to "Temporary tutorial weapons have been issued. They will be reclaimed after the battle ends.",
                "story.cresora.chapter.0_1.hint_1" to "Hold one in your main hand and right-click to activate its Music Echo skill.",
                "story.cresora.chapter.0_1.hint_2" to "'Stride by the Lakeside' excels at burst damage, while 'Melody of the Rondo' is suited for shielding.",
                "story.cresora.chapter.0_1.post_0" to "Echo Weapons... and 'Echoes.'",
                "story.cresora.chapter.0_1.post_1" to "All of them were unfamiliar, and yet they sank into me with unsettling ease.",
                "story.cresora.chapter.0_1.post_2" to "That may mean... you have high affinity with CreSora's 'Music Echo.'",
                "story.cresora.chapter.0_1.post_3" to "The girl finished speaking, then suddenly let out a small gasp and her expression changed for an instant.",
                "story.cresora.chapter.0_1.post_4" to "Or perhaps... ■■■ (this is not your first time after all).",
                "story.cresora.chapter.0_1.post_5" to "!! Could it be...!?",
                "story.cresora.chapter.0_1.post_6" to "Ah, it is nothing. Just in case, please forget that. It is only speculation without any proof, Soloist. And...",
                "story.cresora.chapter.0_1.post_7" to "Even if it truly is the truth, it is something you must uncover with your own hands. Spoilers are in poor taste.",
                "story.cresora.chapter.0_1.post_8" to "Then, in a sweep, the panic from moments ago, the shock, even that 'speculation,' all seemed cleanly erased from your mind, as if none of it had ever existed.",
                "story.cresora.chapter.0_1.post_9" to "This too... is the power of 'Music Echo'...?",
                "story.cresora.chapter.0_1.post_10" to "You will understand later. All of this is for your sake, Soloist.",
                "story.cresora.chapter.0_1.post_11" to "Does a Tuner really wield that much authority over Music Echo? Then why alter my memories...? Should I really trust the Tuners? What is this world, truly?",
                "story.cresora.chapter.0_2.title" to "Mad Crescendo",
                "story.cresora.chapter.d0_1.title" to "Lakeside Sanctum",
                "story.cresora.chapter.d0_2.title" to "Hinagata Archive",
                "story.cresora.chapter.0_2.speaker.oldman" to "???",
                "story.cresora.chapter.0_2.speaker.player" to "<Player>",
                "story.cresora.chapter.0_2.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_2.pre_0" to "Run!",
                "story.cresora.chapter.0_2.pre_1" to "Far away, quickly, anywhere!",
                "story.cresora.chapter.0_2.pre_2" to "Seized by a nameless terror that seeped through your body alongside an indescribable sense of wrongness, you found yourself sprinting along the riverbank.",
                "story.cresora.chapter.0_2.pre_3" to "Before you realized it, the river had led you to a vast lake, wide as though it were the sea itself.",
                "story.cresora.chapter.0_2.pre_4" to "Hey! Young one! Are you a Soloist?",
                "story.cresora.chapter.0_2.pre_5" to "!",
                "story.cresora.chapter.0_2.pre_6" to "Sorry, did I startle you? Soloist. I hate to ask, but could you lend me a hand?",
                "story.cresora.chapter.0_2.pre_7" to "A concert is about to begin here, you see.",
                "story.cresora.chapter.0_2.pre_8" to "You followed the old man's finger and found, at the end of it, nothing but a horde of zombies and skeletons.",
                "story.cresora.chapter.0_2.pre_9" to "Sir, those are monsters.",
                "story.cresora.chapter.0_2.pre_10" to "What are you saying, boy? These are a fine concert audience. Look, the concert has already begun.",
                "story.cresora.chapter.0_2.pre_11" to "Wait, I'm the performer?",
                "story.cresora.chapter.0_2.pre_12" to "And so, amid the groans of zombies and skeletons, the 'concert' raised its curtain.",
                "story.cresora.chapter.0_2.hint_0" to "A temporary max-level 'Melody of the Rondo' has been issued. It will be reclaimed after the battle.",
                "story.cresora.chapter.0_2.hint_1" to "This skeleton has 100% damage reduction and is immune to fixed damage. You do not need to kill it.",
                "story.cresora.chapter.0_2.hint_2" to "Right-click to raise your shield and survive until the 45-second countdown reaches zero.",
                "story.cresora.chapter.0_2.post_0" to "Tch, this thing is tough!",
                "story.cresora.chapter.0_2.post_1" to "Soloist, hold on just a little longer!",
                "story.cresora.chapter.0_2.post_2" to "Huh!?",
                "story.cresora.chapter.0_2.post_3" to "When you turned around, Lumine was standing there, worry written across her face. The moment she raised the weapon in her hand, the skeleton that had been utterly unharmed until then melted into ruin.",
                "story.cresora.chapter.0_2.post_4" to "Honestly, Soloist, you cannot just run off on your own like that.",
                "story.cresora.chapter.0_2.post_5" to "Lumine, can I ask you one thing?",
                "story.cresora.chapter.0_2.post_6" to "Yes?",
                "story.cresora.chapter.0_2.post_7" to "Of course. Because the meaning of '■■■■' is '■■■'.",
                "story.cresora.chapter.0_2.post_8" to "Before you knew it, she had already finished answering. You tried hard to remember, yet even the question you had just asked scattered like mist, and nothing came back.",
                "story.cresora.chapter.0_2.post_9" to "And when you looked closely, Lumine's expression had somehow changed into one tinged with pity."
            ),
            "zh_cn" to linkedMapOf(
                "story.cresora.chapter.0_0.title" to "微弱的低语",
                "story.cresora.chapter.0_0a.title" to "圆舞曲锻房",
                "story.cresora.chapter.0_0b.title" to "通往假面舞会的回廊",
                "story.cresora.chapter.0_0.speaker" to "？？？",
                "story.cresora.chapter.0_0.pre_0" to "记住，■■■■。无论发生什么，都绝对不要失去对“希望”的注视。",
                "story.cresora.chapter.0_0.post_0" to "即便遭遇仿佛要将“■■”撕裂般的……苦痛。",
                "story.cresora.chapter.0_1.title" to "调律者的臆测",
                "story.cresora.chapter.0_1a.title" to "信用试炼场",
                "story.cresora.chapter.0_1.speaker.unknown" to "？？？",
                "story.cresora.chapter.0_1.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_1.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_1.pre_0" to "溪流潺潺，还有虫鸣。",
                "story.cresora.chapter.0_1.pre_1" to "看吧，这道独特的“残响”。",
                "story.cresora.chapter.0_1.pre_2" to "朦胧的意识渐渐苏醒。一个美丽却带着几分忧伤神情的少女，正注视着你。",
                "story.cresora.chapter.0_1.pre_3" to "你是谁？",
                "story.cresora.chapter.0_1.pre_4" to "放眼望去，是被绮丽风景填满的河畔。如此美丽的景色，你此前从未见过。",
                "story.cresora.chapter.0_1.pre_5" to "这里是哪里，你似乎一点印象也没有呢。",
                "story.cresora.chapter.0_1.pre_6" to "确实如此。可明明■■■（我总觉得自己曾无数次见过这片风景）。",
                "story.cresora.chapter.0_1.pre_7" to "不知为何，思绪始终无法理清。唯一能确定的是，这里不是“现实世界”。因为，■■■",
                "story.cresora.chapter.0_1.pre_8" to "时间已经不多了。我是Lumine，是这个名为CreSora的世界中的“调律者”。",
                "story.cresora.chapter.0_1.pre_9" to "你会在这个世界显现，一定有相应的原因。我们“调律者”负责为像你这样迷失道路的“独奏者《Soloist》”引路。",
                "story.cresora.chapter.0_1.pre_10" to "光靠说是不会让你明白的。最直接的方法，就是让你亲眼看看CreSora的“规则”。",
                "story.cresora.chapter.0_1.pre_11" to "来吧，跟上我吧，“独奏者”。愿你的演奏终有一日能解开CreSora的“谜团”。",
                "story.cresora.chapter.0_1.pre_12" to "昔有奏者，其音律绵延不绝，如丝如缕。",
                "story.cresora.chapter.0_1.pre_13" to "于是其踏上旅途，愿那场旅途终有一日能解开其自身的“迟疑”。",
                "story.cresora.chapter.0_1.hint_0" to "已临时发放教程武器。战斗结束后会自动回收。",
                "story.cresora.chapter.0_1.hint_1" to "将武器持于主手并右键，即可发动其“音律回响技能”。",
                "story.cresora.chapter.0_1.hint_2" to "“行于湖畔”偏向高爆发，“圆舞曲的旋律”则偏向护盾防护。",
                "story.cresora.chapter.0_1.post_0" to "回响武器，还有“残响”。",
                "story.cresora.chapter.0_1.post_1" to "明明全都是陌生的东西，却又意外地令人感到熟悉。",
                "story.cresora.chapter.0_1.post_2" to "那或许说明……你与CreSora的“音律回响”有很高的亲和性。",
                "story.cresora.chapter.0_1.post_3" to "少女话音刚落，忽然轻轻“啊”了一声，神情在一瞬间发生了变化。",
                "story.cresora.chapter.0_1.post_4" to "又或者……■■■（这并不是第一次之类的）",
                "story.cresora.chapter.0_1.post_5" to "！！ 难道说……！？",
                "story.cresora.chapter.0_1.post_6" to "啊，没什么。保险起见，请把那句话忘掉吧。那终究只是毫无根据的臆测而已，“独奏者”先生。以及……",
                "story.cresora.chapter.0_1.post_7" to "就算那真的是事实，也该由你亲手去解明。剧透，可不是什么好事。",
                "story.cresora.chapter.0_1.post_8" to "下一瞬，方才的慌乱、惊愕，以及那句“臆测”，仿佛都被从脑海中抹得干干净净，如同从未存在过一般。",
                "story.cresora.chapter.0_1.post_9" to "这也是……“音律回响”的力量吗……",
                "story.cresora.chapter.0_1.post_10" to "之后你会明白的。这一切，都是为了“独奏者”你自己。",
                "story.cresora.chapter.0_1.post_11" to "“调律者”竟拥有这么强的“音律回响”权限吗。可为什么，要动我的记忆……？我真的该相信调律者吗？这个世界，到底是什么。",
                "story.cresora.chapter.0_2.title" to "狂奏",
                "story.cresora.chapter.d0_1.title" to "湖畔圣域",
                "story.cresora.chapter.d0_2.title" to "雏形书库",
                "story.cresora.chapter.0_2.speaker.oldman" to "？？？",
                "story.cresora.chapter.0_2.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_2.speaker.lumine" to "流明",
                "story.cresora.chapter.0_2.pre_0" to "快逃！",
                "story.cresora.chapter.0_2.pre_1" to "先跑远点，总之快点！",
                "story.cresora.chapter.0_2.pre_2" to "莫名的违和感与渗入全身的恐惧交织在一起，你不由得沿着河岸拼命奔跑。",
                "story.cresora.chapter.0_2.pre_3" to "回过神时，河流的去向已经通往一片宽阔得宛如大海般的湖泊。",
                "story.cresora.chapter.0_2.pre_4" to "喂！小子！你是独奏者吗？",
                "story.cresora.chapter.0_2.pre_5" to "！",
                "story.cresora.chapter.0_2.pre_6" to "抱歉，吓到你了吧，独奏者。不好意思，能不能请你帮我个忙？",
                "story.cresora.chapter.0_2.pre_7" to "这里马上就要举办一场演奏会了。",
                "story.cresora.chapter.0_2.pre_8" to "你顺着突然出现的老人手指的方向望去，那里只有成群的僵尸与骷髅。",
                "story.cresora.chapter.0_2.pre_9" to "老爷爷，那是怪物啊。",
                "story.cresora.chapter.0_2.pre_10" to "胡说什么，小子。这可是堂堂正正的演奏会观众。你看，演奏会已经开始了。",
                "story.cresora.chapter.0_2.pre_11" to "欸，演出者……是我？",
                "story.cresora.chapter.0_2.pre_12" to "就这样，在僵尸与骷髅的嘶吼声中，这场“演奏会”拉开了帷幕。",
                "story.cresora.chapter.0_2.hint_0" to "已临时发放满级“圆舞曲的旋律”，战斗结束后会回收。",
                "story.cresora.chapter.0_2.hint_1" to "这只骷髅拥有100%减伤，且免疫确定伤害，不需要击败它。",
                "story.cresora.chapter.0_2.hint_2" to "右键展开护盾，坚持到45秒倒计时归零即可。",
                "story.cresora.chapter.0_2.post_0" to "啧，真棘手！",
                "story.cresora.chapter.0_2.post_1" to "独奏者，再撑一会儿！",
                "story.cresora.chapter.0_2.post_2" to "欸！？",
                "story.cresora.chapter.0_2.post_3" to "回头一看，Lumine正带着担忧的神情站在那里。她举起手中的武器，方才还毫发无伤的骷髅就像融化一般化作残骸。",
                "story.cresora.chapter.0_2.post_4" to "独奏者，你可不能这样一个人乱跑呀。",
                "story.cresora.chapter.0_2.post_5" to "Lumine，我能问你一件事吗？",
                "story.cresora.chapter.0_2.post_6" to "嗯？",
                "story.cresora.chapter.0_2.post_7" to "当然可以。因为“■■■■”的意思，就是“■■■”。",
                "story.cresora.chapter.0_2.post_8" to "回过神时，她已经把答案说完了。你努力想要回忆，可就连刚刚自己问了什么，都像雾一样散去，终究什么也没留下。",
                "story.cresora.chapter.0_2.post_9" to "而且仔细一看，Lumine的表情不知何时已经染上了怜悯。"
            ),
            "lzh" to linkedMapOf(
                "story.cresora.chapter.0_0.title" to "微微之私語",
                "story.cresora.chapter.0_0a.title" to "圓舞鍛房",
                "story.cresora.chapter.0_0b.title" to "假面舞廊",
                "story.cresora.chapter.0_0.speaker" to "？？？",
                "story.cresora.chapter.0_0.pre_0" to "汝其識之，■■■■。無論何事，毋失其所望者。",
                "story.cresora.chapter.0_0.post_0" to "縱遭若將「■■」裂之之苦，亦當如是。",
                "story.cresora.chapter.0_1.title" to "調律者之臆測",
                "story.cresora.chapter.0_1a.title" to "信用試鍊場",
                "story.cresora.chapter.0_1.speaker.unknown" to "？？？",
                "story.cresora.chapter.0_1.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_1.speaker.lumine" to "Lumine",
                "story.cresora.chapter.0_1.pre_0" to "川流潺潺，蟲鳴唧唧。",
                "story.cresora.chapter.0_1.pre_1" to "試觀此獨異之「殘響」。",
                "story.cresora.chapter.0_1.pre_2" to "昏昏之識漸蘇。一少女姝而含悲，正凝視於汝。",
                "story.cresora.chapter.0_1.pre_3" to "汝為誰？",
                "story.cresora.chapter.0_1.pre_4" to "四顧所及，盡為綺景所覆之河濱。如此麗景，前此未嘗一見。",
                "story.cresora.chapter.0_1.pre_5" to "此間何所，汝似全無所識也。",
                "story.cresora.chapter.0_1.pre_6" to "誠然如是。然■■■（吾若曾數數見此景然）。",
                "story.cresora.chapter.0_1.pre_7" to "不知何故，思緒紛然，不可成理。惟知此處非「現實世界」。所以然者，■■■",
                "story.cresora.chapter.0_1.pre_8" to "時已無多。我名Lumine，乃此CreSora世界之「調律者」。",
                "story.cresora.chapter.0_1.pre_9" to "汝既現於此世，必有其故。我輩「調律者」，職在導引如汝般迷途之「獨奏者《Soloist》」。",
                "story.cresora.chapter.0_1.pre_10" to "徒以言辭，汝終未能悟。最快之法，莫若直示CreSora之「規則」。",
                "story.cresora.chapter.0_1.pre_11" to "來罷，隨我行，「獨奏者」。願汝之演奏，終有一日得解CreSora之「謎」。",
                "story.cresora.chapter.0_1.pre_12" to "昔有奏者，其音律綿綿不絕，如縷如絲。",
                "story.cresora.chapter.0_1.pre_13" to "於是其出而遠遊，願其旅終能解其自身之「躊躇」。",
                "story.cresora.chapter.0_1.hint_0" to "權授教習之兵，戰畢即當回收。",
                "story.cresora.chapter.0_1.hint_1" to "執之於主手而右擊，則可發其「音律回響之技」。",
                "story.cresora.chapter.0_1.hint_2" to "「行乎湖畔」長於烈擊，「圓舞曲之律」宜於持盾。",
                "story.cresora.chapter.0_1.post_0" to "回響之兵，與夫「殘響」。",
                "story.cresora.chapter.0_1.post_1" to "諸物皆新，而意外令人深有所感。",
                "story.cresora.chapter.0_1.post_2" to "是或示汝與CreSora之「音律回響」親和甚高。",
                "story.cresora.chapter.0_1.post_3" to "少女言畢，忽低呼一聲，其容色一瞬而變。",
                "story.cresora.chapter.0_1.post_4" to "抑或……■■■（此事恐非初次也）",
                "story.cresora.chapter.0_1.post_5" to "！！ 莫非……！？",
                "story.cresora.chapter.0_1.post_6" to "啊，無他。為慎故，請忘此言。此不過無據之臆測耳，「獨奏者」君。且……",
                "story.cresora.chapter.0_1.post_7" to "縱其果真，亦當由汝親手發明之。先泄其底，非善事也。",
                "story.cresora.chapter.0_1.post_8" to "須臾之間，方才之驚惶、震愕，與彼「臆測」，皆若自腦中泯然盡去，如未嘗有焉。",
                "story.cresora.chapter.0_1.post_9" to "此亦「音律回響」之力乎……",
                "story.cresora.chapter.0_1.post_10" to "後當自知之。凡此一切，皆為「獨奏者」汝也。",
                "story.cresora.chapter.0_1.post_11" to "「調律者」於「音律回響」之權，竟強若此乎。然何以改吾之憶……？吾果可盡信調律者乎？此世界，竟何所是。",
                "story.cresora.chapter.0_2.title" to "狂奏",
                "story.cresora.chapter.d0_1.title" to "湖畔聖域",
                "story.cresora.chapter.d0_2.title" to "雛形書庫",
                "story.cresora.chapter.0_2.speaker.oldman" to "？？？",
                "story.cresora.chapter.0_2.speaker.player" to "＜Player＞",
                "story.cresora.chapter.0_2.speaker.lumine" to "流明",
                "story.cresora.chapter.0_2.pre_0" to "速走！",
                "story.cresora.chapter.0_2.pre_1" to "且遠去之，務速！",
                "story.cresora.chapter.0_2.pre_2" to "莫名之乖戾與滲體之怖交作，遂不覺沿河狂奔。",
                "story.cresora.chapter.0_2.pre_3" to "及自省時，川流所向，已成巨湖，廣若海然。",
                "story.cresora.chapter.0_2.pre_4" to "噫！少年！汝為獨奏者乎？",
                "story.cresora.chapter.0_2.pre_5" to "！",
                "story.cresora.chapter.0_2.pre_6" to "失敬，驚汝矣，獨奏者。煩汝少助我，可乎？",
                "story.cresora.chapter.0_2.pre_7" to "此間頃刻將啟一場樂會。",
                "story.cresora.chapter.0_2.pre_8" to "汝循其所指而視，彼處惟見殭屍與骷髏成群。",
                "story.cresora.chapter.0_2.pre_9" to "翁翁，此乃怪也。",
                "story.cresora.chapter.0_2.pre_10" to "何出此言，少年。此乃堂堂樂會之觀眾也。子試觀之，樂會已啟矣。",
                "story.cresora.chapter.0_2.pre_11" to "咦，登臺者乃我乎？",
                "story.cresora.chapter.0_2.pre_12" to "於是，在殭屍與骷髏之呻吟中，此「樂會」遂啟其幕。",
                "story.cresora.chapter.0_2.hint_0" to "權授滿級「圓舞曲之律」一柄，戰畢即收。",
                "story.cresora.chapter.0_2.hint_1" to "此骷髏減傷百之百，且免於確定之傷，毋庸擊之。",
                "story.cresora.chapter.0_2.hint_2" to "右擊張盾，守至四十五秒倒數既盡即可。",
                "story.cresora.chapter.0_2.post_0" to "咄，勁敵也！",
                "story.cresora.chapter.0_2.post_1" to "獨奏者，再持頃刻！",
                "story.cresora.chapter.0_2.post_2" to "咦！？",
                "story.cresora.chapter.0_2.post_3" to "回首而視，流明憂然佇立其後。其舉手中之兵，方才全然無傷之骷髏，遂若融化而為餘骸。",
                "story.cresora.chapter.0_2.post_4" to "獨奏者，汝不可擅自奔走如此也。",
                "story.cresora.chapter.0_2.post_5" to "流明，我可問一事乎？",
                "story.cresora.chapter.0_2.post_6" to "然。何也？",
                "story.cresora.chapter.0_2.post_7" to "固可。蓋「■■■■」之義，即「■■■」也。",
                "story.cresora.chapter.0_2.post_8" to "及自覺時，其答已畢。力思而追之，然方才所問之語亦如霧散，卒無所得。",
                "story.cresora.chapter.0_2.post_9" to "且更視之，流明之色，不知何時已帶憫然。"
            )
        )
    }
}
