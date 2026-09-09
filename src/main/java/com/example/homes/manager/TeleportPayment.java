package com.example.homes.manager;

import java.util.UUID;

/**
 * テレポート開始前に徴収済みの費用と、成功時に公開ホーム持ち主へ渡す分配。
 * 失敗・キャンセル時は {@code refundCostKey} を払い戻し、分配は行わない。
 */
public record TeleportPayment(String refundCostKey, UUID payoutOwner, double payoutAmount) {

    public static TeleportPayment none() {
        return new TeleportPayment(null, null, 0);
    }

    public static TeleportPayment refundable(String costKey) {
        return new TeleportPayment(costKey, null, 0);
    }

    public static TeleportPayment visitPublic(String costKey, UUID owner, double amount) {
        return new TeleportPayment(costKey, owner, amount);
    }

    public boolean hasPayout() {
        return payoutOwner != null && payoutAmount > 0;
    }
}
