# TODO

## CWC Migration (Next Phases)
- [ ] Migrate `lakeside_stride` (Current HP burst logic)
- [ ] Migrate `hanwu_juanxue` (Snow Frost logic)
- [ ] Migrate `kyokusui_no_ryusho` (Orchid Pavilion Echo logic - complex tick intervals)
- [ ] Migrate `gaoshan_liushui` (Healing Aura logic)
- [ ] Migrate `dark_lux` (Complex transform logic)

## Compiler Improvements
- [ ] Implement `/* ... */` block comment support within the `.cresora` language itself (currently only works inside `execute` blocks because they are extracted as raw text).
- [ ] Add automatic `import` detection or simplified import DSL to avoid long qualified names in `execute` blocks.
- [ ] Support `area_of_effect` block content extraction from source similar to `execute` blocks.
