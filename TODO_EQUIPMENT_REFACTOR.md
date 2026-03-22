# Equipment Refactor TODO

## Goal

Build a Genshin-like equipment growth system without elemental reactions.
The first version should focus on reusable affix-based equipment growth, not on a single attack multiplier item.

## Scope

- Keep the system non-elemental for now
- Reuse the current upgrade workflow as the entry point
- Generalize the design so future equipment types can share the same backend
- Avoid adding more persistent text blocks to the main upgrade GUI
- Move expandable upgrade details into mouse-hover tooltips wherever possible

## Core Data Model

- Add a new equipment data component instead of relying on `level` alone
- Store at least:
  - `rarity`
  - `level`
  - `mainStat`
  - `subStats`
  - `upgradeCount`
- Keep `level` compatibility only as a temporary migration bridge if needed

## Stat Pool

- Add a first-pass stat pool for non-elemental equipment:
  - `ATK_FLAT`
  - `ATK_PERCENT`
  - `HP_FLAT`
  - `HP_PERCENT`
  - `DEF_FLAT`
  - `DEF_PERCENT`
  - `CRIT_RATE`
  - `CRIT_DMG`
  - `ALL_DMG_BONUS`
  - `DAMAGE_REDUCTION`

## Affix Rules

- Each equipment piece should have exactly one main stat
- Each equipment piece should have multiple sub stats
- Main stat and sub stats must not duplicate each other
- Sub stats must not duplicate each other
- Critical stats should start as sub-stat-only unless balance proves otherwise

## Rarity and Growth

- Introduce at least three rarity tiers:
  - `3-star`
  - `4-star`
  - `5-star`
- Use rarity to control:
  - max level
  - initial sub-stat count
  - roll strength
- Recommended first-pass caps:
  - `3-star`: `+8`
  - `4-star`: `+12`
  - `5-star`: `+16`

## Upgrade Rules

- Upgrade should increase equipment level, not just a raw item component
- Every `+4` levels should trigger one sub-stat growth event
- If sub-stat count is below the cap, add a new sub stat on growth
- If sub-stat count is already full, randomly strengthen one existing sub stat
- Keep upgrade material logic compatible with the current GUI flow

## Tooltip and UI Direction

- Keep the main upgrade GUI minimal and structurally stable
- Do not add more permanent labels or dense stat text into the main panel
- Show detailed equipment information in hover tooltips:
  - main stat
  - sub stats
  - growth preview hints
  - rarity
- If preview text is needed, prefer short labels over explanatory sentences

## Runtime Architecture

- Create a dedicated stat type definition such as `StatType`
- Create a reusable equipment data structure such as `EquipmentData`
- Add a roller/service responsible for initial stat generation
- Add an upgrade service responsible for level-up and sub-stat growth
- Add a stat calculator responsible for aggregating all equipped item bonuses
- Move item-specific combat bonuses out of individual item classes when possible

## Integration Plan

### Phase 1

- Define `StatType`
- Define `StatEntry`
- Define `EquipmentData`
- Register the new equipment data component

### Phase 2

- Implement random generation for rarity, main stat, and sub stats
- Add validation and normalization logic for malformed or legacy data

### Phase 3

- Refactor upgrade logic to operate on `EquipmentData`
- Preserve current upgrade entry flow and screen handler behavior
- Add tooltip-based preview details instead of expanding the GUI surface

### Phase 4

- Introduce a shared player stat aggregation layer
- Apply aggregated stats to combat and survivability logic
- Reduce direct per-item tick-side stat patching where possible

### Phase 5

- Migrate the current wand-like equipment into the generalized system
- Reserve naming and validation rules for future equipment classes

## Open Balancing Questions

- Whether `DAMAGE_REDUCTION` should stay, or be replaced by `ENERGY_REGEN`
- Whether `ALL_DMG_BONUS` should remain global long-term, or later split by attack category
- How much of upgrade outcome preview should be deterministic versus hidden until roll time

## Implementation Notes

- Keep root progress documents in English
- If code comments are added during implementation, keep them in Japanese
- Prefer migration-safe logic where possible, but old save compatibility is no longer the main constraint
