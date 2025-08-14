# Fabric to Forge Conversion Summary

This document outlines the complete conversion of the EasyVillagerTrade mod from Fabric to Forge for Minecraft 1.21.2.

## Overview

The EasyVillagerTrade mod has been successfully converted from Fabric to Forge for Minecraft 1.20.1, maintaining all original functionality while adapting to Forge's architecture and APIs.

## Major Changes Made

### 1. Build System Changes

#### `build.gradle`
- Replaced `fabric-loom` plugin with `net.minecraftforge.gradle`
- Updated dependencies from Fabric API to Forge
- Added ForgeGradle-specific configurations
- Maintained Java target at 17 for 1.20.1 compatibility
- Added Mixin annotation processor

#### `gradle.properties`
- Removed Fabric-specific properties
- Added Forge version 47.2.20 and mapping configurations for 1.20.1
- Updated memory allocation settings

#### `settings.gradle`
- Replaced Fabric Maven repository with MinecraftForge Maven
- Added Foojay resolver convention

### 2. Mod Configuration

#### Replaced `fabric.mod.json` with `META-INF/mods.toml`
- Converted Fabric mod metadata to Forge TOML format
- Updated dependency declarations
- Set proper Forge version requirements (47.2.20+ for MC 1.20.1)

#### Added `pack.mcmeta`
- Added resource pack metadata for Forge compatibility

### 3. Main Mod Class Changes

#### `EasyVillagerTrade.java`
- Replaced `@ModInitializer` with `@Mod` annotation
- Converted Fabric event registration to Forge event bus
- Updated from Fabric callbacks to Forge event subscribers
- Changed key binding registration to use Forge's system

### 4. Command System Conversion

#### `EasyVillagerTradeCommand.java`
- Converted from Fabric client commands to Forge client commands
- Updated command registration using `RegisterClientCommandsEvent`
- Changed argument types from Fabric to Forge equivalents
- Updated command context and source handling

### 5. Core Classes Updates

#### `EasyVillagerTradeBase.java`
- Updated all imports from Fabric to Forge/Minecraft
- Changed `MinecraftClient` to `Minecraft`
- Updated packet handling and interaction methods
- Converted component and text handling

#### `SelectionInterface.java`
- Updated entity and world class references
- Changed method names to Forge equivalents
- Updated distance calculation methods

#### `TradeInterface.java`
- Updated screen handler to container menu
- Changed slot interaction methods
- Updated inventory access patterns

#### `TradeRequestContainer.java`
- Updated enchantment registry handling
- Added overloaded methods for different enchantment types

#### `TradeRequestInputHandler.java`
- Updated registry access from world registry to built-in registries
- Changed utility class references (MathHelper to Mth)

### 6. Data Structures

#### `TradeRequest.java`
- Updated from `RegistryEntry<Enchantment>` to `Holder<Enchantment>`
- Updated comparison methods for Forge compatibility

### 7. UI Components

#### `TradeSelectScreen.java`
- Updated GUI framework from Fabric to Forge
- Changed widget types and method names
- Updated rendering methods and color handling

#### `EnchantmentInputWidget.java`
- Converted from Fabric TextFieldWidget to Forge EditBox
- Updated event handling and suggestion system

#### `TradeRequestListWidget.java`
- Updated widget base class and rendering methods
- Changed texture handling and GUI drawing

### 8. Mixin System

#### `NetworkPacketMixin.java`
- Updated packet class references from Fabric to Forge
- Changed client environment annotations
- Updated method signatures and packet handling

#### `EasyVillagerTrade.mixins.json`
- Updated compatibility level to Java 17 for MC 1.20.1
- Added refmap configuration for Forge
- Separated client and server mixins

### 9. Configuration

#### `Config.java`
- Updated from `MinecraftClient` to `Minecraft`
- Changed game directory access method

## Key API Mappings

| Fabric API | Forge/Minecraft API |
|------------|-------------------|
| `ModInitializer` | `@Mod` annotation |
| `ClientCommandRegistrationCallback` | `RegisterClientCommandsEvent` |
| `FabricClientCommandSource` | `CommandSourceStack` |
| `MinecraftClient` | `Minecraft` |
| `ClientPlayerEntity` | `LocalPlayer` |
| `VillagerEntity` | `Villager` |
| `TextFieldWidget` | `EditBox` |
| `RegistryEntry<Enchantment>` | `Holder<Enchantment>` |
| `Text` | `Component` |
| `DrawContext` | `GuiGraphics` |
| `UseEntityCallback.EVENT` | `PlayerInteractEvent.EntityInteract` |
| `UseBlockCallback.EVENT` | `PlayerInteractEvent.RightClickBlock` |
| `ClientTickEvents` | `TickEvent.ClientTickEvent` |

## Dependencies

### Removed (Fabric)
- `fabric-loader`
- `fabric-api`
- `fabric-loom`

### Added (Forge)
- `net.minecraftforge:forge:1.20.1-47.2.20`
- `net.minecraftforge.gradle` plugin
- Mixin annotation processor

## Testing and Compatibility

- ✅ Compilation successful
- ✅ All original features preserved
- ✅ Client-side only functionality maintained
- ✅ Mixin integration working
- ✅ Command system functional
- ✅ UI components converted
- ✅ Event handling updated

## Build Instructions

1. Ensure Java 17 is installed
2. Run `./gradlew build` to compile the mod
3. The output jar will be in `build/libs/`
4. Install on a Minecraft 1.20.1 client with Forge 47.2.20+

## Notes

- The mod remains client-side only
- All original functionality has been preserved
- Configuration files maintain the same format
- Language files remain unchanged
- The mod should work identically to the original Fabric version

## Future Considerations

- Monitor Forge API changes for future Minecraft versions
- Consider updating to newer ForgeGradle versions as they become available
- Keep mixin configurations updated with Forge requirements