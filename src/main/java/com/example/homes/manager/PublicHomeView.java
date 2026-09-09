package com.example.homes.manager;

import java.util.UUID;

/** 公開ホーム一覧 GUI 用の表示データ。 */
public record PublicHomeView(
        UUID ownerUuid,
        String ownerName,
        String homeName,
        String worldName,
        double x,
        double y,
        double z,
        String memo) {
}
