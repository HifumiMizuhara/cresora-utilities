package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

object HotbarOverrideService {
    private data class OverrideSession(
        val originalHotbar: Array<ItemStack>,
        val originalSelectedSlot: Int,
        val expireTick: Long,
        val weaponId: String,
        val weaponData: WeaponData,
        val subSkillEffectIds: Set<String>
    )

    private val sessions = mutableMapOf<UUID, OverrideSession>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            val toRestore = mutableListOf<UUID>()

            for ((uuid, session) in sessions) {
                if (now >= session.expireTick) {
                    toRestore.add(uuid)
                }
            }

            for (uuid in toRestore) {
                server.playerManager.getPlayer(uuid)?.let { restoreHotbar(it) }
            }
            pruneOfflineSessions(server.playerManager.playerList.mapTo(linkedSetOf(), ServerPlayerEntity::getUuid))
        }
    }

    fun overrideHotbar(player: ServerPlayerEntity, weaponId: String, subSkillEffectIds: List<String>, durationTicks: Long) {
        if (sessions.containsKey(player.uuid)) {
            restoreHotbar(player)
        }

        val hotbar = Array(9) { player.inventory.getStack(it).copy() }
        val now = player.server?.overworld?.time ?: 0L
        val definition = WeaponContentRegistry.requireWeapon(weaponId)
        val weaponData = WeaponStackSupport.getWeaponData(player.mainHandStack)
            ?: WeaponData(weaponId, definition.craft.craftedRarity, 1, 1)

        sessions[player.uuid] = OverrideSession(
            hotbar,
            player.inventory.selectedSlot,
            now + durationTicks,
            weaponId,
            weaponData.normalized(definition),
            subSkillEffectIds.toSet()
        )

        // Clear hotbar and set sub-skill items
        for (i in 0 until 9) {
            if (i < subSkillEffectIds.size) {
                val effectId = subSkillEffectIds[i]
                val dummyStack = createSubSkillStack(effectId)
                player.inventory.setStack(i, dummyStack)
            } else {
                player.inventory.setStack(i, ItemStack.EMPTY)
            }
        }

        player.inventory.selectedSlot = 0
        player.sendMessage(Text.translatable("message.cresora.hotbar_overridden").formatted(Formatting.GOLD), true)
    }

    fun restoreHotbar(player: ServerPlayerEntity) {
        restoreSession(player, sendMessage = true)
    }

    fun isOverridden(player: ServerPlayerEntity): Boolean = sessions.containsKey(player.uuid)

    fun activeWeaponContext(player: ServerPlayerEntity): Pair<WeaponDefinition, WeaponData>? {
        val session = sessions[player.uuid] ?: return null
        val definition = runCatching { WeaponContentRegistry.requireWeapon(session.weaponId) }.getOrNull() ?: return null
        return definition to session.weaponData.normalized(definition)
    }

    fun activateSubSkill(player: ServerPlayerEntity, effectId: String, access: WeaponSkillAccess): net.minecraft.util.ActionResult {
        val session = sessions[player.uuid] ?: return net.minecraft.util.ActionResult.FAIL
        if (effectId !in session.subSkillEffectIds) {
            return net.minecraft.util.ActionResult.FAIL
        }

        val definition = WeaponContentRegistry.requireWeapon(session.weaponId)
        val handler = hifumi.cresora.skill.WeaponSkillRegistry.getHandler(effectId)
            ?: return net.minecraft.util.ActionResult.FAIL
        return handler.activate(player, definition, session.weaponData.normalized(definition), access)
    }

    fun clearSession(player: ServerPlayerEntity) {
        restoreSession(player, sendMessage = false)
    }

    private fun createSubSkillStack(effectId: String): ItemStack {
        val stack = ItemStack(CreSoraUtilities.SUB_SKILL_DUMMY)
        stack.set(ModDataComponents.SUB_SKILL_EFFECT_ID, effectId)

        // Apply CustomModelData based on effectId for icon mapping
        val modelData = when (effectId) {
            "danro" -> 1.0f
            "zanso" -> 2.0f
            "bokuchu_munen" -> 3.0f
            else -> 0.0f
        }

        if (modelData > 0.0f) {
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                net.minecraft.component.type.CustomModelDataComponent(listOf(modelData), emptyList(), emptyList(), emptyList()))
        }

        return stack
    }

    private fun pruneOfflineSessions(onlinePlayerIds: Set<UUID>) {
        sessions.keys.removeIf { it !in onlinePlayerIds }
    }

    private fun restoreSession(player: ServerPlayerEntity, sendMessage: Boolean): Boolean {
        val session = sessions.remove(player.uuid) ?: return false

        for (i in 0 until 9) {
            player.inventory.setStack(i, session.originalHotbar[i])
        }
        player.inventory.selectedSlot = session.originalSelectedSlot.coerceIn(0, 8)

        if (sendMessage) {
            player.sendMessage(Text.translatable("message.cresora.hotbar_restored").formatted(Formatting.GRAY), true)
        }
        return true
    }
}
