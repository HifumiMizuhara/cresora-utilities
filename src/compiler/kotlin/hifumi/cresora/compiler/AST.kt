package hifumi.cresora.compiler

sealed class ASTNode

data class WeaponDefNode(
    val name: String,
    val id: String,
    val rarity: String,
    val baseItem: String,
    val damageType: String,
    val role: String = "guard",
    val stats: StatsNode,
    val skill: SkillNode?,
    val translations: Map<String, Map<String, String>> = emptyMap(),
    val subSkills: List<SubSkillNode> = emptyList(),
    val customModelData: Int? = null,
    val texture: String? = null
) : ASTNode()

data class ArtifactDefNode(
    val name: String,
    val id: String,
    val bonuses: List<ArtifactBonusNode>,
    val translations: Map<String, Map<String, String>> = emptyMap()
) : ASTNode()

data class ArtifactBonusNode(
    val requiredPieces: Int,
    val stats: Map<String, Double> = emptyMap(),
    val handlers: List<SkillHandlerNode> = emptyList(),
    val buffs: List<BuffNode> = emptyList(),
    val requiresWeapon: String? = null,
    val displayStackBonuses: List<DisplayStackBonusNode> = emptyList()
) : ASTNode()

data class DisplayStackBonusNode(
    val buffId: String,
    val stacks: Int
) : ASTNode()

data class DictionaryDefNode(
    val id: String,
    val translations: Map<String, Map<String, String>>
) : ASTNode()

data class SubSkillNode(
    val name: String,
    val effectId: String,
    val icon: String,
    val handlers: List<SkillHandlerNode>
) : ASTNode()

data class StatsNode(
    val baseAttackDamage: Double,
    val attackDamagePerLevel: Double,
    val totalAttackSpeed: Double,
    val maxBaseLevel: Int,
    val maxSkillLevel: Int,
    val critRateBonusPercent: Double = 0.0,
    val maxAllDamageBonusPercent: Double = 0.0,
    val hpBonusPercent: Double = 0.0
) : ASTNode()

data class SkillNode(
    val name: String,
    val effectId: String,
    val cooldownSeconds: Double,
    val durationSeconds: Double,
    val baseValue: Double,
    val valuePerLevel: Double,
    val radiusMeters: Double,
    val handlers: List<SkillHandlerNode>,
    val buffs: List<BuffNode> = emptyList()
) : ASTNode()

data class BuffNode(
    val id: String,
    val translationKey: String?,
    val maxStacks: Int,
    val durationSeconds: Double,
    val stats: Map<String, Double>,
    val decay: String = "refresh"
) : ASTNode()

data class SkillHandlerNode(
    val eventName: String, // e.g., "on_activate", "on_damage_dealt"
    val actions: List<ActionNode>
) : ASTNode()

data class AreaOfEffectActionNode(
    val radius: Double,
    val actions: List<ActionNode>
) : ActionNode()

sealed class ActionNode : ASTNode()

data class CommandActionNode(
    val commandName: String,
    val arguments: List<String>
) : ActionNode()

data class InstructionCallNode(
    val functionName: String,
    val arguments: List<String>
) : ActionNode()

data class SendLocalizedMessageActionNode(
    val key: String,
    val color: String,
    val arguments: List<String>
) : ActionNode()

data class ExpressionNode(
    val content: String // 簡易化のため、現在は生の文字列でロジックを保持
) : ActionNode()

data class OpenSkillMenuActionNode(
    val subSkillEffectIds: List<String>,
    val durationSeconds: Double
) : ActionNode()

object CloseSkillMenuActionNode : ActionNode()

data class ExecuteActionNode(
    val statements: List<ActionNode>
) : ActionNode()

data class MovementDefNode(
    val name: String,
    val id: String,
    val displayName: String,
    val groupId: String?,
    val sortOrder: Int,
    val titleTextId: String?,
    val linkedDomainId: String?,
    val domainRewardIds: List<String>,
    val unlockRank: Int,
    val prerequisiteChapterId: String?,
    val preBattleStory: List<DialogueLineNode>,
    val combatHints: List<String>,
    val grantedWeapons: List<GrantedWeaponNode>,
    val battleObjective: BattleObjectiveNode,
    val battleWaves: List<BattleWaveNode>,
    val postBattleStory: List<DialogueLineNode>,
    val rewards: RewardsNode,
    val translations: Map<String, Map<String, String>>
) : ASTNode()

data class DialogueLineNode(
    val speakerId: String?,
    val textId: String
) : ASTNode()

data class GrantedWeaponNode(
    val weaponId: String,
    val rarity: String,
    val baseLevel: Int,
    val skillLevel: Int,
    val removeOnExit: Boolean
) : ASTNode()

data class BattleObjectiveNode(
    val type: String,
    val durationSeconds: Int
) : ASTNode()

data class BattleWaveNode(
    val enemyRank: Int,
    val spawnDelayTicks: Int,
    val spawns: List<SpawnNode>,
    val modifiers: ModifiersNode
) : ASTNode()

data class SpawnNode(
    val entityTypeId: String,
    val count: Int
) : ASTNode()

data class ModifiersNode(
    val damageReductionPercent: Double,
    val trueDamageImmune: Boolean
) : ASTNode()

data class RewardsNode(
    val credits: Int,
    val resonanceCurrencies: Map<String, Int>
) : ASTNode()
