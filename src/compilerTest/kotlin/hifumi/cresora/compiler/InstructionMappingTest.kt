package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InstructionMappingTest {

    @Test
    fun `duration suffix is converted to ticks`() {
        val expanded = InstructionMapping.expand("apply_mark", listOf("target", "\"lux\"", "30s"))
        assertEquals("hifumi.cresora.weapon.WeaponSkillService.applyMark(target, \"lux\", 600L)", expanded)
    }

    @Test
    fun `identifier ending in s is not mistaken for a time literal`() {
        val expanded = InstructionMapping.expand("log", listOf("status"))
        assertEquals("player.sendMessage(net.minecraft.text.Text.literal(status))", expanded)
    }

    @Test
    fun `skill_value is substituted`() {
        val expanded = InstructionMapping.expand("heal", listOf("skill_value"))
        assertEquals("hifumi.cresora.weapon.WeaponCombatSupport.healHp(player, definition.skill.baseValue)", expanded)
    }

    @Test
    fun `skill_duration is substituted`() {
        val expanded = InstructionMapping.expand("grant_invulnerability", listOf("player", "skill_duration"))
        assertEquals(
            "hifumi.cresora.weapon.WeaponSkillService.grantInvulnerability(player, definition.skill.durationSeconds * 20L)",
            expanded
        )
    }

    @Test
    fun `apply_status_effect duration is converted to int ticks`() {
        val expanded = InstructionMapping.expand("apply_status_effect", listOf("\"minecraft:strength\"", "10s", "1"))
        assertEquals(
            "hifumi.cresora.weapon.WeaponCombatSupport.applyStatusEffect(player, \"minecraft:strength\", 200, 1)",
            expanded
        )
    }

    @Test
    fun `start_cooldown without args uses skill cooldown`() {
        val expanded = InstructionMapping.expand("start_cooldown", emptyList())
        assertEquals(
            "hifumi.cresora.weapon.WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)",
            expanded
        )
    }

    @Test
    fun `artifact context rejects start_cooldown`() {
        assertThrows<RuntimeException> {
            InstructionMapping.expand("start_cooldown", emptyList(), CompilerContext.ARTIFACT)
        }
    }

    @Test
    fun `artifact context rejects no-arg heal`() {
        assertThrows<RuntimeException> {
            InstructionMapping.expand("heal", emptyList(), CompilerContext.ARTIFACT)
        }
    }

    @Test
    fun `artifact context rejects skill_value`() {
        assertThrows<RuntimeException> {
            InstructionMapping.expand("heal", listOf("skill_value"), CompilerContext.ARTIFACT)
        }
    }

    @Test
    fun `unknown function expands to plain call`() {
        val expanded = InstructionMapping.expand("custom_thing", listOf("1", "2"))
        assertEquals("custom_thing(1, 2)", expanded)
    }

    @Test
    fun `expandAll expands bare instruction calls`() {
        val expanded = InstructionMapping.expandAll("ignite(3)", CompilerContext.WEAPON)
        assertEquals("target.setOnFireFor(3.toFloat())", expanded)
    }

    @Test
    fun `expandAll does not expand dot-prefixed calls`() {
        val content = "player.heal(5.0f)"
        assertEquals(content, InstructionMapping.expandAll(content, CompilerContext.WEAPON))
    }

    @Test
    fun `expandAll keeps commas inside string arguments intact`() {
        val expanded = InstructionMapping.expandAll("apply_mark(target, \"a,b\", 5s)", CompilerContext.WEAPON)
        assertEquals("hifumi.cresora.weapon.WeaponSkillService.applyMark(target, \"a,b\", 100L)", expanded)
    }

    @Test
    fun `expandAll handles nested parentheses in arguments`() {
        val expanded = InstructionMapping.expandAll("heal((base + 1.0) * 2.0)", CompilerContext.WEAPON)
        assertEquals("hifumi.cresora.weapon.WeaponCombatSupport.healHp(player, (base + 1.0) * 2.0)", expanded)
    }

    @Test
    fun `expandAll leaves unrelated identifiers untouched`() {
        val content = "val healing = baseHeal + 1"
        assertEquals(content, InstructionMapping.expandAll(content, CompilerContext.WEAPON))
    }

    @Test
    fun `isKnown covers mapped instructions and intrinsics`() {
        assertTrue(InstructionMapping.isKnown("apply_mark"))
        assertTrue(InstructionMapping.isKnown("add_buff"))
        assertTrue(!InstructionMapping.isKnown("not_a_thing"))
    }
}
