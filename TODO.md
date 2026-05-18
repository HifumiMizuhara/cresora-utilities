# TODO


## CWC & System Polish
- [ ] Improve `execute` block Kotlin syntax highlighting support in IDEs (low priority).

## Compiler Improvements
- [x] Add automatic `import` detection or simplified import DSL to avoid long qualified names in `execute` blocks. (Completed 2026-05-18)
- [ ] Replace remaining raw `execute` string passthroughs with typed AST nodes for safer code generation.
- [ ] Add parser tests for malformed `.cresora` files and `area_of_effect` blocks.
 
## Runtime Hardening Follow-up
- [x] Harden active weapon-skill scope so non-held weapon handlers cannot bleed stats or carry buffs during weapon swap (Completed 2026-05-18).
- [x] Fix P2: Clear WeaponSkillService-level transient stats (e.g. `taoStacks`) on weapon swap (Completed 2026-05-18).
- [x] Fix P2: Auto-resolve and bind held weapon ID to dynamic shields in `grantShield` if omitted, ensuring dynamic shields clear reliably on weapon swap (Completed 2026-05-18).
- [ ] Add regression tests for weapon-skill scope so non-held weapon handlers cannot bleed into unrelated attribute calculations.
- [x] Integrate transient combat state cleanup and pruning across CWC-generated skills and WeaponSkillService (Completed 2026-05-18).
- [ ] Add disconnect/reconnect coverage for debuffs and Masquerade respawn.
- [ ] Add regression coverage for equipment mob loot injection and treasure chest reconnect scheduling.
- [ ] Add invalid-content tests for unsupported equipment `effectHooks` and malformed resonance pools.

## New Content
- [ ] Define detailed effects for special moon phases: Solar Eclipse, Lunar Eclipse, Death Moon, and Unknown.
- [ ] Replace Blood Moon placeholder rewards with final implementations for `血色音符`, `失色之冠`, and `血之泪`.
- [ ] Add dedicated visual texture assets for `moon_brick` and `moon_altar` (currently uses vanilla placeholder textures/models).
- [ ] Add persistence for in-progress Blood Moon battles if sessions need to survive server restart. (Reward chest ownership/seed persistence is now implemented.)
- [ ] Add dedicated regression or machine-test coverage for Blood Moon wave spawning, bed protection, reward chest ownership, and natural-spawn pressure.
