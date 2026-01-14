# HomesPlugin Implementation Plan

## Completed Tasks
1.  **Project Setup**: Initialized Maven project (`pom.xml`) and plugin configuration (`plugin.yml`).
2.  **Data Management**: Implemented `HomeManager` to save/load homes in `homes.yml`.
3.  **Teleport Logic**: Implemented `TeleportManager` handling the 5-second delay and movement cancellation.
4.  **GUI Implementation**: Created `HomeGUI` which displays homes in the middle row (slots 9-17) of a chest inventory.
5.  **Main Class**: Implemented `HomesPlugin` to register commands (`/home`, `/sethome`) and listeners.
6.  **Verification**: Successfully compiled the project using `mvn clean package`.

## Features
-   **/sethome <name>**: Saves the current location as a home.
-   **/home**: Opens the GUI.
-   **GUI**: Shows homes in the middle row (Slots 9-17).
-   **Teleportation**:
    -   Clicking a home closes the inventory.
    -   Starts a 5-second countdown.
    -   Cancels if the player moves.

## Next Steps (User)
1.  Copy `target/HomesPlugin-1.0-SNAPSHOT.jar` to your server's `plugins` folder.
2.  Restart the server.
3.  Enjoy!
