# Sophisticated Integrations

An addon for Minecraft that connects **Tom's Simple Storage** terminals to **Sophisticated Backpacks**.

## Inventory access

- Carried and equipped backpacks join the terminal's item list
- Withdrawals use the network first, then the viewer's backpacks. Deposits use the network first and put any remainder in accepting backpacks. Full inventories return the remainder.

## Recipe viewers

JEI, EMI, and REI are all supported.

## Backpack crafting

Carry a bound **Advanced Wireless Terminal** and open a backpack with a **Crafting Upgrade**. Recipe transfers use backpack/player ingredients first, then the linked Tom's network. Maximum transfers preserve complete recipe sets. Enable Sophisticated's existing refill toggle to refill the grid from the network after crafting; refill also works without a recipe viewer.

The first carried, bound Advanced Wireless Terminal selects the network. Tom's range, dimension, and beacon rules apply, and the target chunk must already be loaded. Removing the terminal or losing access stops extraction. Results and crafting remainders keep Sophisticated's normal destinations.

Network recipe transfers support registered crafting recipes with ingredient lists. Viewer-only synthetic recipes and special recipes without ingredient lists keep their native behavior. This does not add network browsing or support for other backpack upgrades.

## Dependencies

The development environment pins these NeoForge releases:

| Mod                     | Version       |
|-------------------------|---------------|
| Tom's Simple Storage    | 2.4.2         |
| Sophisticated Backpacks | 3.26.9.2195   |
| Sophisticated Core      | 1.5.7.2381    |
| JEI                     | 19.39.0.369   |
| EMI                     | 1.1.24+1.21.1 |
| REI                     | 16.0.799      |

Install the addon and the three storage dependencies on both client and server.

## Configuration

`sophisticatedintegrations-server.toml` provides:

- `terminal.includeBackpacks` (default `true`): enable personal backpack access in terminals.
- `terminal.insertIntoBackpacks` (default `true`): allow deposit overflow into backpacks.
- `backpackCrafting.useLinkedNetwork` (default `true`): enable network ingredients and refill in backpack crafting upgrades.

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
