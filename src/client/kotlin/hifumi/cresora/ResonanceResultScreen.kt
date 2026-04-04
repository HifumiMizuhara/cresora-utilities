package hifumi.cresora

import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ResonanceResultScreen(
    handler: ResonanceResultScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ResonanceResultScreenHandler>(handler, inventory, title, 1)
