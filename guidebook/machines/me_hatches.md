---
navigation:
  title: "ME Hatches"
  icon: "me_mixed_output_hatch"
  parent: "machines/machines-index.md"
  position: 1
item_ids:
  - me_item_input_hatch
  - me_fluid_input_hatch
  - me_mixed_input_hatch
  - me_item_output_hatch
  - me_fluid_output_hatch
  - me_mixed_output_hatch
---

# ME Hatches
<OptionalSection modId="ae2">
<ItemGrid>
    <ItemIcon id="me_item_input_hatch"/>
    <ItemIcon id="me_fluid_input_hatch"/>
    <ItemIcon id="me_mixed_input_hatch"/>
    <ItemIcon id="me_item_output_hatch"/>
    <ItemIcon id="me_fluid_output_hatch"/>
    <ItemIcon id="me_mixed_output_hatch"/>
</ItemGrid>

ME hatches connect Modern Industrialization multiblocks directly to an Applied Energistics 2 network, much like an ME
interface. Each comes in item, fluid and mixed variants. A hatch connects to the network through its output face.

## Transfer Interval

Hatches exchange resources with the network every few ticks, set by the **Interval** field in their screen. New hatches
start at the `transfer_interval_ticks` config value (40 by default). Type a value and press Enter, or scroll over the
field.

Moving resources to and from the network draws AE power, as an ME interface does.

## Input Hatches

Set a resource and an amount in each channel of the configuration row. The hatch keeps that amount stocked from the
network. Changing a channel returns the stock it no longer needs. Unconfigured channels stock nothing.

**Smart pull**, on by default, stocks enough for the multiblock's running recipe to keep crafting until the next
transfer. Configured amounts stay the minimum.

### Pull Priority

The priority button at the top right opens AE2's priority screen. When input hatches on the same network want a
resource the network runs short of, the **higher priority stocks first**:

- A hatch leaves alone what any higher-priority input hatch still needs to reach its stocking target, smart pull
  included, and pulls only the rest.
- That share stays set aside while the higher-priority hatch is connected and powered, even between its transfers or
  while its machine is idle.
- Hatches of equal priority share as before: whichever transfers first takes what it needs.
- Stock a hatch already holds is never taken back for a higher-priority one.

A higher-priority hatch whose target the network can never meet keeps lower-priority hatches from that resource for
good, so keep high-priority targets within what the network holds.

## Output Hatches

An output hatch with no configured channels accepts any output. Once a channel is configured, the hatch accepts only the
configured resources.

### Storage Cells

The screen's side panel holds AE2 storage cells: an item cell slot on item and mixed hatches, and a fluid cell slot on
fluid and mixed hatches. Outputs move into the installed cell every tick, so the cell works as the hatch's buffer:

- The machine only starts what fits in the cell. As the cell fills, batching machines run smaller batches, then stop.
- Contents stay in the cell when it is removed or the hatch is broken.
- Each cell adds its usual idle power draw to the network.
- Void cards and partitions work as they do in an ME drive. A partition that refuses an output stops the machine
  instead of sending that output elsewhere.

Without a cell, outputs go straight to the network, and the machine only starts what the network can take. If the
network is full, offline or out of power, the machine stops.

The room is checked on each transfer, so a machine can exceed it by up to one interval's worth of output, and
multi-output recipes by up to one batch. The excess waits in the hatch's slots until there is room; nothing is voided.

### Push And Storage Modes

The **Storage** checkbox picks what happens to the cell's contents:

- **Push** (unchecked, the default) sends everything in the cell to the network on each transfer, including what the
  cell held when it was installed.
- **Storage** keeps the contents in the cell and offers the cell to the network as storage, like a cell in an ME drive.
  The network can take from it, for example from a terminal or for autocrafting, but cannot insert into it: only the
  machine fills it.

Storage mode needs a cell. Without one, the label turns red and the hatch accepts no outputs, so the machine stops.
The interval does not apply in Storage mode and its field is greyed out. Switching back to Push empties the cell right
away.

### Storage Priority

The priority button at the top right opens AE2's priority screen. The priority applies in Storage mode and works like an
ME drive's priority.

AE2 inserts into the highest priority first but **takes from the lowest priority first**. Set a priority below your
other storage to have the network use the hatch's outputs before anything else.

## Copying Settings

Extended Industrialization's <ItemLink id="extended_industrialization:machine_config_card"/> and the [Multiblock Builder](../items/multiblock_builder.md) copy the configured channels, interval,
smart pull, mode and priority. They never copy installed cells.

## Breaking A Hatch

Breaking a hatch returns its stock to the network. If the network cannot take it, the hatch refuses to break; break it
again while sneaking to destroy the remaining stock. Installed cells always drop with their contents.

When something other than a player removes the hatch, such as an explosion, stock the network cannot take is not
protected: each item channel drops at most 27 stacks, and the rest of it is lost, along with any fluid.
</OptionalSection>

<OptionalSection modId="!ae2">
## Applied Energistics 2 isn't currently installed, these hatches will not work.
</OptionalSection>
