# Sophisticated Integrations

An addon for Minecraft that connects **Tom's Simple Storage** terminals to **Sophisticated Backpacks**.

## Inventory access

- Carried and equipped backpacks join the terminal's item list
- Withdrawals use the network first, then the viewer's backpacks. Deposits use the network first and put any remainder in accepting backpacks. Full inventories return the remainder.
- The **Re-sort storage** button below the backpack toggle consolidates matching items across the linked physical storage. Personal backpacks and crafting grids are not re-sorted.
- Deposits fill matching stacks while respecting configured priorities and item filters. Withdrawals drain overflow first, keeping terminal-managed storage consolidated automatically. Re-sort is useful for existing scattered stock or changes made outside the terminal.

## Recipe viewers

JEI, EMI, and REI are all supported.

## Backpack crafting

While your normal inventory is open, JEI, EMI, and REI transfers prefer a reachable linked **Tom's crafting terminal** when a recipe needs more than 2×2 or extra ingredients. An equipped or carried backpack with a **Crafting Upgrade** is the fallback. Recipes that fit and can be filled locally continue using the inventory grid.

Carry a bound **Advanced Wireless Terminal** and open a backpack with a **Crafting Upgrade**. Recipe transfers use backpack/player ingredients first, then the linked Tom's network. Enable Sophisticated's existing refill toggle to refill the grid from the network after crafting.

## Dependencies

The development environment uses these NeoForge releases:

| Mod                     | Version       |
|-------------------------|---------------|
| Tom's Simple Storage    | 2.4.2         |
| Sophisticated Backpacks | 3.26.9.2195   |
| Sophisticated Core      | 1.5.7.2381    |
| JEI                     | 19.57.0.451   |
| EMI                     | 1.1.24+1.21.1 |
| REI                     | 16.0.799      |

Install the addon and the three storage dependencies on both client and server.

## Configuration

`sophisticatedintegrations-server.toml` provides:

- `terminal.includeBackpacks` (default `true`): enable personal backpack access in terminals.
- `terminal.insertIntoBackpacks` (default `true`): allow deposit overflow into backpacks.
- `backpackCrafting.useLinkedNetwork` (default `true`): enable network ingredients and refill in backpack crafting upgrades.
- `backpackCrafting.openFromInventory` (default `true`): automatically open the linked crafting terminal or a crafting backpack for inventory transfers needing a larger grid or extra ingredients.

## Development

Java 21 is required.

```sh
./gradlew build
./gradlew :1.21.1:runClient
./gradlew :1.21.1:runClient -PrecipeViewer=emi
./gradlew :1.21.1:runClient -PrecipeViewer=rei
./gradlew :1.21.1:runClient -PrecipeViewer=none
./gradlew :1.21.1:runGameTestServer -PrecipeViewer=none
```
