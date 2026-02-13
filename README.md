# HomesPlugin

[English](#english) | [日本語](#japanese)

<a name="english"></a>
## English

**HomesPlugin** is a robust and user-friendly Home Management Plugin for Minecraft servers (Spigot/Paper).
This plugin allows players to set multiple homes, manage them via a GUI, share them publicly with others, and includes a full-featured TPA system.

### ✨ Features

*   **Home Management**:
    *   Set homes with `/sethome <name>`.
    *   Teleport to homes with `/home <name>`.
    *   Delete homes with `/delhome <name>`.
*   **GUI Interface**:
    *   Manage homes intuitively using a chest GUI via `/homes`.
    *   Visit other players' public homes using `/vhome <player>`.
*   **TPA System (Teleport Request)**:
    *   Send teleport requests to other players.
    *   Supports `/tpa` (Teleport to player) and `/tpahere` (Teleport player to you).
    *   Interactive chat buttons for **[Accept]** and **[Deny]**.
    *   Features include cooldowns, warmup time (5s), and movement cancellation.
*   **Back Command**:
    *   Return to your previous location or death point using `/back`.
*   **Economy Support**:
    *   Integration with Vault to charge for setting homes, teleporting, etc.
*   **Fully Configurable**:
    *   All messages and settings can be customized in `config.yml`.

### 📖 Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/sethome <name>` | Set a home at your current location. | `homes.use` |
| `/delhome <name>` | Delete a specific home. | `homes.use` |
| `/home <name>` | Teleport to a specific home. | `homes.use` |
| `/home <player>:<name>` | Teleport to another player's public home. | `homes.use` |
| `/homes` | Open your home management GUI. | `homes.use` |
| `/homes reload` | Reload the plugin configuration. | `homes.reload` |
| `/vhome <player>` | Open another player's public home list. | `homes.use` |
| `/tpa <player>` | Request to teleport to another player. | `homes.use` |
| `/tpahere <player>` | Request another player to teleport to you. | `homes.use` |
| `/tpaccept` | Accept a teleport request. | `homes.use` |
| `/tpdeny` | Deny a teleport request. | `homes.use` |
| `/tpcancel <player>` | Cancel a sent teleport request. | `homes.use` |
| `/tpatoggle` | Toggle receiving teleport requests. | `homes.use` |
| `/tpaignore <player>` | Ignore teleport requests from a specific player. | `homes.use` |
| `/back` | Teleport to your previous location or death point. | `homes.use` |

### ⚙️ Configuration

You can configure settings in `config.yml`.

```yaml
settings:
  default-home-limit: 1  # Maximum homes per player
  teleport-delay: 3      # Teleport warmup in seconds
  
  # TPA Settings
  tpa:
    enabled: true        # Enable TPA system
    back-on-teleport: true # Save location to /back when using TPA
    cooldown: 60         # TPA cooldown in seconds

  # Back Settings
  back:
    enabled: true        # Enable /back command
    save-death-location: true # Save death location to /back
```

### 📥 Installation

1.  Download the `HomesPlugin.jar`.
2.  Place it in your server's `plugins` folder.
3.  (Optional) Install Vault and an Economy plugin (like EssentialsX) for economy features.
4.  Restart your server.

### 👤 Developer

Developed by **naonao**.

---

<a name="japanese"></a>
## 日本語

**HomesPlugin** は、Minecraft Spigot/Paper サーバー向けの多機能ホーム管理 & テレポートプラグインです。
ホームの設定、GUIによる管理、経済連携、そしてプレイヤー間のテレポート（TPA）機能をこれ1つで提供します。

### ✨ 主な機能

*   **ホーム管理**:
    *   `/sethome <名前>` で現在地をホームに設定。
    *   `/home <名前>` で設定したホームへテレポート。
    *   `/delhome <名前>` でホームを削除。
*   **GUI操作**:
    *   `/homes` でGUIを開き、クリック操作でホーム一覧を確認・テレポート可能。
    *   他人の公開ホームへの訪問機能 (`/vhome`)。
*   **TPA (テレポートリクエスト)**:
    *   プレイヤー間でのテレポート申請・承認機能。
    *   `/tpa` (相手の場所へ行く) と `/tpahere` (相手を呼ぶ) に対応。
    *   チャットの【承認】【拒否】ボタンで簡単操作。
    *   クールダウン設定、テレポート前の待機時間（5秒）、移動キャンセル機能付き。
*   **Back機能**:
    *   `/back` でテレポート前の場所や死亡地点に戻ることが可能。
*   **経済連携**:
    *   Vaultプラグインと連携し、ホーム設定やテレポートにコストを設定可能。
*   **完全な日本語対応**:
    *   メッセージは `config.yml` ですべてカスタマイズ可能。

### 📖 コマンド一覧

| コマンド | 説明 | 権限 |
| --- | --- | --- |
| `/sethome <名前>` | 現在地をホームとして設定します。 | `homes.use` |
| `/delhome <名前>` | 特定のホームを削除します。 | `homes.use` |
| `/home <名前>` | 特定のホームにテレポートします。 | `homes.use` |
| `/home <プレイヤー>:<名前>` | 他のプレイヤーの公開ホームにテレポートします。 | `homes.use` |
| `/homes` | ホーム管理 GUI を開きます。 | `homes.use` |
| `/homes reload` | プラグインの設定を再読み込みします。 | `homes.reload` |
| `/vhome <プレイヤー>` | 他のプレイヤーの公開ホームリストを開きます。 | `homes.use` |
| `/tpa <プレイヤー>` | 相手に自分のテレポートリクエストを送ります（相手の場所へ行く）。 | `homes.use` |
| `/tpahere <プレイヤー>` | 相手を自分の場所に呼ぶリクエストを送ります（カモン）。 | `homes.use` |
| `/tpaccept` | 届いているリクエストを承認します。 | `homes.use` |
| `/tpdeny` | 届いているリクエストを拒否します。 | `homes.use` |
| `/tpcancel <プレイヤー>` | 送ったリクエストをキャンセルします。 | `homes.use` |
| `/tpatoggle` | TPAの受信拒否設定を切り替えます。 | `homes.use` |
| `/tpaignore <プレイヤー>` | 特定のプレイヤーからのTPAを無視します。 | `homes.use` |
| `/back` | 直前の場所（または死亡地点）に戻ります。 | `homes.use` |

### ⚙️ 設定 (config.yml)

```yaml
settings:
  default-home-limit: 1  # デフォルトのホーム作成上限数
  teleport-delay: 3      # テレポート待機時間（秒）
  
  # TPA設定
  tpa:
    enabled: true        # TPA機能を有効にするか
    back-on-teleport: true # TPA使用時に移動元を/backに保存するか
    cooldown: 60         # TPAのクールダウン（秒）

  # Back設定
  back:
    enabled: true        # Back機能を有効にするか
    save-death-location: true # 死亡地点を/backに保存するか
```

### 📥 インストール

1.  `HomesPlugin.jar` をサーバーの `plugins` フォルダに配置します。
2.  サーバーを再起動します。
3.  必要に応じて `plugins/HomesPlugin/config.yml` を編集してください。

### 📦 ビルド方法

Mavenを使用しています。

```bash
mvn clean package
```

### 👤 開発者

**naonao** によって開発されました。

## 📜 ライセンス

MIT License
