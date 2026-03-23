package hifumi.cresora

import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier

object ModItemTags {
    val ADVENTURE_RANK_ARTIFACTS: TagKey<Item> = TagKey.of(
        RegistryKeys.ITEM,
        Identifier.of(CreSoraUtilities.MOD_ID, "adventure_rank_artifacts")
    )

    val ADVENTURE_RANK_UPGRADE_MATERIALS: TagKey<Item> = TagKey.of(
        RegistryKeys.ITEM,
        Identifier.of(CreSoraUtilities.MOD_ID, "adventure_rank_upgrade_materials")
    )
}
