package com.example.homes.gui.holder;

import java.util.List;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import com.example.homes.manager.PublicHomeView;

/** 公開ホーム一覧 GUI の識別と、ページ・表示中ホームのスナップショットを保持する。 */
public final class PublicHomeGuiHolder implements InventoryHolder {

    private final int page;
    private final List<PublicHomeView> homes;
    private Inventory inventory;

    public PublicHomeGuiHolder(int page, List<PublicHomeView> homes) {
        this.page = page;
        this.homes = List.copyOf(homes);
    }

    public int getPage() {
        return page;
    }

    /** ヘッドスロット順に並んだ、このページの公開ホーム。 */
    public List<PublicHomeView> getHomes() {
        return homes;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
