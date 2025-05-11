**SuperKamelPlugin Documentation**

This document provides comprehensive details for the `SuperKamelPlugin`, a Minecraft Bukkit/Spigot plugin that enhances camel entities with persistent storage, real‑time speed adjustment based on carried load, and dynamic size inheritance for bred camels. It guides server administrators and plugin developers through installation, configuration, core concepts, event flows, and customization points.

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Installation & Deployment](#installation--deployment)
4. [Configuration & Default Settings](#configuration--default-settings)
5. [Core Concepts](#core-concepts)
6. [Namespaces & Persistent Data Keys](#namespaces--persistent-data-keys)
7. [Event Handlers & Hooks](#event-handlers--hooks)

    * [onEnable](#onEnable)
    * [onCamelSpawn](#onCamelSpawn)
    * [onBreed](#onBreed)
    * [onCamelInteract](#onCamelInteract)
    * [onInventoryClick & onInventoryClose](#onInventoryClick--onInventoryClose)
    * [onCamelDeath](#onCamelDeath)
8. [Item Weight System](#item-weight-system)
9. [Inventory Lifecycle & GUI](#inventory-lifecycle--gui)
10. [Speed Calculation & Slowdown Formula](#speed-calculation--slowdown-formula)
11. [Customization & Extension Points](#customization--extension-points)
12. [Troubleshooting & FAQs](#troubleshooting--faqs)
13. [License & Contribution](#license--contribution)

---

## Overview

`SuperKamelPlugin` transforms camels from simple rideable mobs into versatile pack animals. It introduces:

* **Saddle‑based Storage**: A 54‑slot GUI opens when a player interacts with a saddled camel with an empty hand.
* **Weight Mechanics**: Each carried item has a weight; the camel’s move speed decreases proportionally to the total load.
* **Inherited Size**: Breeding produces offspring whose size is randomly chosen between parent sizes, affecting visual scale.
* **Persistent Attributes**: Uses Bukkit’s `PersistentDataContainer` to store custom attributes directly on each camel entity.

These features combine to provide an immersive, RPG‑style transport system for resource management on large multiplayer servers.

## Prerequisites

* **Minecraft Server**: Bukkit or Spigot-based, version 1.19+.
* **Java**: OpenJDK 11 or later.
* **Build Tools**: Maven or Gradle for compiling.

Ensure the server is configured to allow custom plugins (`plugins/` directory accessible) and that memory settings accommodate additional entity attributes.

## Installation & Deployment

1. **Clone Repository**: `git clone https://github.com/yourorg/SuperKamelPlugin.git`
2. **Build JAR**:

    * **Maven**: `mvn clean package`
    * **Gradle**: `gradle build`
3. **Deploy**: Copy `SuperKamelPlugin.jar` into the server’s `plugins/` folder.
4. **Restart Server**: Fully restart to load new classes and register event listeners.
5. **Verify**: Check console logs for `CamelCarryPlugin enabled!` to confirm successful load.

Optionally, enable detailed logging by adjusting `log4j2.xml` in the server config.

## Configuration & Default Settings

No external configuration file is included; all settings are hard‑coded in `onEnable()`. Default parameters:

* **Base Camel Size**: 1.0.
* **Base Speed Capture**: Recorded at spawn from `Attribute.MOVEMENT_SPEED`.
* **Weight Overrides**: Specific materials have custom weights, others default to 1.0.
* **Inventory Slots**: Fixed 54 (double‑chest style).
* **Slowdown Formula**: `newSpeed = baseSpeed / (1 + totalWeight / 50.0)` applies a diminishing speed curve.

To apply runtime configuration, consider externalizing parameters to a YAML file and loading values in `onEnable()`.

## Core Concepts

* **Camel Entity**: Represented by `org.bukkit.entity.Camel`. Plugin attaches custom data via `PersistentDataContainer` fields for size and base speed.
* **ItemWeights Map**: A `Map<Material, Double>` that maps certain `Material` enums to weight values.
* **Inventory Management**: Each camel’s storage GUI is tied to its UUID; `camelInventories` map persists inventories in memory until the camel dies.

Understanding these data structures is key to extending or debugging behavior.

## Namespaces & Persistent Data Keys

Two `NamespacedKey` identifiers store doubles in each camel’s PDC:

| Key Name    | Purpose                            | Data Type    |
| ----------- | ---------------------------------- | ------------ |
| `camelSize` | Visual and logical size scale      | DOUBLE (PDC) |
| `baseSpeed` | Original unburdened movement speed | DOUBLE (PDC) |

```java
private NamespacedKey keySize    = new NamespacedKey(this, "camelSize");
private NamespacedKey keyBaseSpeed = new NamespacedKey(this, "baseSpeed");
```

## Event Handlers & Hooks

Detailed behavior for each registered event:

### `onEnable()`

* Instantiate `NamespacedKey` objects.
* Populate `itemWeights` map with hard‑coded overrides.
* Register this class as a listener for all events.
* Log a startup message.

### `onCamelSpawn(EntitySpawnEvent e)`

When any entity spawns:

1. Verify `instanceof Camel`.
2. If `camelSize` absent, set to 1.0.
3. If `baseSpeed` absent, read from `Attribute.MOVEMENT_SPEED` and store.

### `onBreed(EntityBreedEvent event)`

During camel breeding:

1. Ensure both parents are camels.
2. Retrieve parent sizes (defaulting 1.0).
3. Calculate `childSize = random between min and max`.
4. Store and apply scale via `Attribute.SCALE`.

### `onCamelInteract(PlayerInteractEntityEvent event)`

On player right‑click:

1. Confirm target is saddled camel.
2. If player holds a chest, cancel interaction (prevents block placing).
3. If hand is empty, `computeIfAbsent` a 54‑slot inventory by UUID and open it.

### `onInventoryClick` & `onInventoryClose`

When a GUI slot is clicked or closed:

* If the inventory’s holder is a `Camel`, call `updateCamelSpeed(Camel camel)`.

### `onCamelDeath(EntityDeathEvent e)`

Upon camel death:

* Remove its inventory from `camelInventories`.
* Drop each stored item naturally at the death location.

## Item Weight System

`itemWeights` map holds special values; any material not present defaults to 1.0. Example entries:

```java
itemWeights.put(Material.NETHERITE_BLOCK, 100.0);
itemWeights.put(Material.DIAMOND_BLOCK,    64.0);
itemWeights.put(Material.IRON_BLOCK,       32.0);
// etc.
```

To add custom weights, modify or extend this map in `onEnable()` or externalize it for runtime editing.

## Inventory Lifecycle & GUI

* **Storage Map**: `Map<UUID, Inventory> camelInventories` persists GUI inventories in server memory.
* **Creation**: Lazy‑loaded via `computeIfAbsent()` on first interaction.
* **Slots**: Fixed size 54 to match double‑chest interface.
* **Persistence**: Lives until camel death or server shutdown.

## Speed Calculation & Slowdown Formula

Within `updateCamelSpeed(Camel camel)`, the plugin:

1. Iterates over `inv.getContents()` to sum `weight * amount`.

2. Retrieves `baseSpeed` from PDC.

3. Applies:

   ```java
   double newSpeed = baseSpeed / (1 + totalWeight / 50.0);
   ```

4. Sets the camel’s `Attribute.MOVEMENT_SPEED` to `newSpeed`, resulting in diminishing speed returns as weight increases.

## Customization & Extension Points

* **External Configuration**: Load weight values and formula parameters from a YAML file.
* **Inventory Size**: Change slot count in `Bukkit.createInventory()`.
* **Breed Logic**: Adjust inheritance algorithm or introduce modifiers (e.g., speed bonuses).
* **Visual Effects**: Hook into `onCamelInteract` to display actionbar messages or particles.

Consider subclassing or creating event interfaces to decouple logic for easier testing.

## Troubleshooting & FAQs

**Q**: Camel storage not opening?

* Ensure the camel is saddled and you interact with an empty hand.

**Q**: Weight not affecting speed?

* Confirm `updateCamelSpeed()` is invoked (check server logs).
* Verify PDC keys exist via in‑game commands or debugging.

**Q**: Data resets after server reload?

* Camel PDC data is ephemeral; consider persisting UUID and PDC values to disk on `onDisable()`.

## License & Contribution

`SuperKamelPlugin` is licensed under the MIT License.

Contributions, issues, and pull requests are welcome on GitHub:

`https://github.com/yourorg/SuperKamelPlugin`

Please follow the repository’s contribution guidelines and code style conventions.
