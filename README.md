# HomesPlugin

[English](README.md) | [日本語](README_JP.md)

A robust and user-friendly Home Management Plugin for Minecraft servers (Spigot/Paper).
This plugin allows players to set multiple homes, manage them via a GUI, and even share them publicly with others.

## Features

*   **GUI-Based Management**: Intuitive chest interface for managing homes.
*   **Multiple Homes**: Players can set multiple homes (configurable limit).
*   **Public/Private Toggle**: Players can make their homes public for others to visit.
*   **Economy Support**: Integration with Vault for charging costs for setting homes, teleporting, or making homes public.
*   **Database Support**: Supports H2 (local file, default) and MySQL/MariaDB.
*   **Tab Completion**: Smart tab completion for commands and player names.
*   **Asynchronous Processing**: Heavy database operations are handled asynchronously to prevent server lag.
*   **World-Specific Icons**: Customize home icons based on the world they are in.

## Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/sethome <name>` | Set a home at your current location. | `homes.use` |
| `/delhome <name>` | Delete a specific home. | `homes.use` |
| `/home <name>` | Teleport to a specific home. | `homes.use` |
| `/home <player>:<name>` | Teleport to another player's public home. | `homes.use` |
| `/homes` | Open your home management GUI. | `homes.use` |
| `/homes <player>` | Open another player's public home list. (Admins see all) | `homes.use` |
| `/homes reload` | Reload the plugin configuration. | `homes.reload` |

## Permissions

*   `homes.use`: Basic access to home commands (Default: true).
*   `homes.admin`: View and manage other players' private homes (Default: OP).
*   `homes.reload`: Allow reloading the plugin config (Default: OP).
*   `homes.limit.<number>`: Set the maximum number of homes a player can have (e.g., `homes.limit.5`).

## Configuration

### Database
By default, the plugin uses H2 (local file database). To use MySQL/MariaDB, change `database.type` in `config.yml`.

```yaml
database:
  type: "mysql" # or "mariadb"
  host: "localhost"
  port: "3306"
  name: "minecraft"
  user: "root"
  password: "password"
```

### Economy
Requires Vault and an economy plugin (e.g., EssentialsX).

```yaml
economy:
  cost:
    set-home: 100.0
    teleport: 10.0
    make-public: 500.0
```

### GUI Customization
You can customize the GUI title, icons, and messages in `config.yml`.

## Installation

1.  Download `HomesPlugin-1.5.jar`.
2.  Place it in your server's `plugins` folder.
3.  (Optional) Install Vault and an Economy plugin for economy features.
4.  Restart your server.

## Developer
Developed by naonao.
