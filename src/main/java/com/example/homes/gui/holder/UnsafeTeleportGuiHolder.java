package com.example.homes.gui.holder;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.example.homes.manager.TeleportPayment;

/** 危険な場所へのテレポート確認 GUI の識別と、テレポート先の座標を保持する。 */
public final class UnsafeTeleportGuiHolder implements InventoryHolder {

    private final Location target;
    /** 確認前に徴収済みの費用と、成功時の公開ホーム分配。キャンセル時は払い戻しのみ。 */
    private final TeleportPayment payment;
    /** はい/いいえで解決済みか。閉じる(Esc等)で二重に払い戻さないためのフラグ。 */
    private boolean resolved;
    private Inventory inventory;

    public UnsafeTeleportGuiHolder(Location target, TeleportPayment payment) {
        this.target = target;
        this.payment = payment == null ? TeleportPayment.none() : payment;
    }

    public Location getTarget() {
        return target;
    }

    public TeleportPayment getPayment() {
        return payment;
    }

    public String getRefundCostKey() {
        return payment.refundCostKey();
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
