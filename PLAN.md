# CFHCore Visual Recipe Editor Plan

## Architecture Overview
The system will follow a layered architecture: Presentation (CFHGui screens/hotkey listener), Logic (Recipe Editor state management), Service Layer (JEI Bridge for discovery/sync, SQLite Manager for persistence), and Data Layer (SQLite DB). Communication between layers will utilize CFHCore's existing event bus/system hooks.

## Module Breakdown
1. **Hotkey Listener:** NeoForge event registration for the hotkey to open the editor screen.
2. **Editor Screen:** New CFHGui screen implementing dynamic UI based on discovered recipe types.
3. **JEI Bridge:** Module responsible for querying JEI (mezz.jei) for `RecipeCategories` and parsing JSON recipe data.
4. **Storage:** SQLite database management for saving and loading recipes in the new format.
5. **Sync:** Logic to push saved recipes back to the JEI registry/data pack format, requiring minimal JEI service restart capability.

## File List to Create/Modify
- `src/main/java/com/onehumanawa/cfhcore/gui/VisualRecipeEditorScreen.java` (New)
- `src/main/java/com/onehumanawa/cfhcore/events/RecipeEditorListener.java` (New/Modify)
- `src/main/java/com/onehumanawa/cfhcore/jei/JeiRecipeBridge.java` (New)
- `src/main/java/com/onehumanawa/cfhcore/storage/RecipeDatabaseManager.java` (New)
- `build.gradle` (Modify dependencies)
- `META-INF/mods.toml` (Verify/Update dependencies)
- `resources/config/cfhcore.json` (Modify for new settings/DB paths)

## Dependency Additions Needed
1. **JEI API:** `mezz.jei` dependency compatible with Minecraft 1.21.1. (UNVERIFIED: requires web_search)
2. **SQLite JDBC:** A compatible JDBC driver (e.g., `sqlite-jdbc`) bundled or added as a compileOnly dependency. (UNVERIFIED: requires web_search)

## Risk Areas and Mitigation
1. **JEI API Version:** Incorrect API usage due to version mismatch. Mitigation: Verify exact 1.21.1 binding via `web_search`.
2. **Minimal JEI Restart:** JEI's internal restart mechanism might require a full mod reload. Mitigation: Test minimal restart capability on a staging setup first.
3. **Recipe Data Structure:** JSON schema variance across JEI categories. Mitigation: Implement robust, dynamic JSON parsing with fallback error handling, not hardcoding types.

## Implementation Order
1. Web search for JEI API 1.21.1 and SQLite driver.
2. Update `build.gradle` and verify build success.
3. Implement `RecipeDatabaseManager` (Storage).
4. Implement `JeiRecipeBridge` (Discovery/Reading).
5. Implement `VisualRecipeEditorScreen` (Presentation, using CFHGui).
6. Implement Save/Export logic (Sync) and test minimal JEI reload.
7. Finalize Hotkey registration.
