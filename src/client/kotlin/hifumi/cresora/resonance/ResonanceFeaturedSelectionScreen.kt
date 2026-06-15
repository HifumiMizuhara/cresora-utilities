package hifumi.cresora.resonance
import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ResonanceFeaturedSelectionScreen(
    handler: ResonanceFeaturedSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ResonanceFeaturedSelectionScreenHandler>(
    handler,
    inventory,
    title,
    ResonanceFeaturedSelectionScreenHandler.ROWS
)
