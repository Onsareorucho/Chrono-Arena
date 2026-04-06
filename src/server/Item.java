package server;

import shared.ItemType;

public class Item {
    private String itemId;
    private ItemType itemType;
    private int itemPositionX;
    private int itemPositionY;
    private boolean isAvailable;

    public Item(String itemId, ItemType itemType, int itemPositionX, int itemPositionY) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemPositionX = itemPositionX;
        this.itemPositionY = itemPositionY;
        this.isAvailable = true; // always starts available
    }

    public Item() {
    }

    public String getItemId() {
        return itemId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getItemPositionX() {
        return itemPositionX;
    }

    public int getItemPositionY() {
        return itemPositionY;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    public void setItemPositionX(int itemPositionX) {
        this.itemPositionX = itemPositionX;
    }
    public void setItemPositionY(int itemPositionY) {
        this.itemPositionY = itemPositionY;
    }
    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    

}
