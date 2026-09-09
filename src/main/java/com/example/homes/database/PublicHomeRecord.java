package com.example.homes.database;

import java.util.UUID;

/** DB 上の公開ホーム 1 件。World 解決前の生データ。 */
public record PublicHomeRecord(
        UUID ownerUuid,
        String homeName,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String memo) {
}
