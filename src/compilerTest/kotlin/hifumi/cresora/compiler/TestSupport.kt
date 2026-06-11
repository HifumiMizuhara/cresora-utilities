package hifumi.cresora.compiler

fun lex(source: String): List<Token> = Lexer(source).scanTokens()

fun parse(source: String): List<ASTNode> = Parser(source, lex(source)).parse()

fun parseWeapon(source: String): WeaponDefNode = parse(source).filterIsInstance<WeaponDefNode>().single()

/** Wraps handler bodies in a minimal valid weapon so tests can focus on action parsing. */
fun weaponWithHandler(handlerBody: String, eventName: String = "on_activate"): WeaponDefNode {
    val source = """
        weapon "Test Weapon" {
            id: "test_weapon"
            rarity: "rare"
            base_item: "minecraft:iron_sword"
            stats {
                base_damage: 5.0
                damage_per_level: 0.5
            }
            skill "Test Skill" {
                effect_id: "test_skill"
                cooldown: 10s
                duration: 5s
                $eventName {
                    $handlerBody
                }
            }
        }
    """.trimIndent()
    return parseWeapon(source)
}

fun handlerActions(weapon: WeaponDefNode, eventName: String = "on_activate"): List<ActionNode> =
    weapon.skill!!.handlers.single { it.eventName == eventName }.actions
