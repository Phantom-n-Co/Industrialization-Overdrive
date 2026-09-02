---
navigation:
  title: "Multiblock Builder"
  icon: "industrialization_overdrive:terminal"
  parent: "io_guide.md"
  position: 3
item_ids:
  - industrialization_overdrive:terminal
---

# Multiblock Builder

<RecipesFor id="industrialization_overdrive:terminal" />

The Multiblock Builder helps you build, copy, paste, and dismantle Modern Industrialization multiblocks.

## Modes

Press <KeyBind id="key.industrialization_overdrive.terminal_mode_switch" /> to cycle through the available modes:

- **Build** places the missing parts of an MI multiblock. Sneak-use the builder on the multiblock controller to build it automatically.
- **Copy/Paste** copies and pastes complete multiblock structures. Sneak-use it on a formed multiblock controller to copy the structure, then use it on a block to paste it.
- **Tear Down** dismantles a formed multiblock and returns its parts to you.

## Copying And Pasting

Copy/Paste mode preserves machine configuration where possible, including orientation, automatic item and fluid extraction, slot locks, upgrades, hulls, and supported mod-specific settings.

When pasting, the structure is rotated relative to the direction you are facing. Use the builder on the side of a block to place the structure beside it. When sneaking, the targeted block is treated as the floor beneath the structure.

## Materials

The builder checks your inventory before building or pasting a structure. If Applied Energistics 2 is installed, it can also use a linked ME system to provide missing parts.

## Extended Industrialization

When Extended Industrialization is installed, sneak-copying a Machine Chainer copies its connected machines, nested Machine Chainers, and tagged relay blocks. The `machine_chainer_copy_depth` config option controls how far nested paths are copied:

- `-1` copies without a depth limit.
- `0` disables Machine Chainer copying.
- Positive values limit the number of nested chainer path levels.
