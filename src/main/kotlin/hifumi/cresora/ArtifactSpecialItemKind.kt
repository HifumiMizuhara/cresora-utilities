package hifumi.cresora

import com.mojang.serialization.Codec

enum class ArtifactSpecialItemKind(
    val id: String
) {
    ALPHA("alpha"),
    BETA("beta"),
    NOTE("note");

    companion object {
        private val BY_ID = entries.associateBy(ArtifactSpecialItemKind::id)

        val CODEC: Codec<ArtifactSpecialItemKind> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown artifact special item kind: $id") },
            ArtifactSpecialItemKind::id
        )
    }
}
