# TODO

- Follow the dedicated refactor plan in [TODO_EQUIPMENT_REFACTOR.md](/Users/hifumimizuhara/IdeaProjects/cresora-utilities-1.21.1/TODO_EQUIPMENT_REFACTOR.md)
- Expand the generalized equipment system so future equipment classes can reuse the same generation and growth backend
- Add more Trinkets slot mappings and item categories for future non-pendant equipment parts
- Revisit balance numbers for rarity rates, stat roll ranges, and sacrifice-upgrade payoff
- Continue in-game GUI polishing, especially visual balance between slots, metrics, and button placement
- Sub-TODO: Move upgrade details out of the fragile main GUI and into mouse-hover tooltips wherever possible
- Keep reducing text noise and rely more on layout, hierarchy, and short labels
- Reserve more generic slot validation and display logic for future upgradable equipment beyond the current item
- Re-evaluate whether the current upgrade button should become a more vanilla workstation-like interaction
- Decide whether `upgrade.png` should remain as a resource or be fully replaced by code-driven / vanilla texture rendering
- Re-run in-game verification that `hinagatastue` can be inserted into the Trinkets UI `necklace` slot after the explicit Trinket registration fallback
- Re-run in-game verification of the player-facing `/cresora_stats` command in each supported language and confirm all 10 localized stat lines render cleanly
- Re-run in-game verification of the revised drop tiers and confirm zombie, skeleton, and warden drops now match their intended rarity and level bands
- Re-run in-game combat verification of the new base crit profile and confirm the default `5%` crit chance feels correct with the current damage pacing
- Add enemy-family-biased stat pools so different mobs are worth farming for different build archetypes
- Expand combat feedback beyond crits so final-damage bonus and damage-reduction effects are easier to validate in live play
