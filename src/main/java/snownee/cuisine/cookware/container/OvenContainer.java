package snownee.cuisine.cookware.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IntReferenceHolder;
import snownee.cuisine.base.item.RecipeItem;
import snownee.cuisine.cookware.CookwareModule;
import snownee.cuisine.cookware.tile.AbstractCookwareTile;
import snownee.cuisine.cookware.tile.OvenTile;
import snownee.cuisine.data.RecordData;
import snownee.cuisine.util.ModSlot;

public class OvenContainer extends Container {

    protected AbstractCookwareTile tile;
    public final PlayerEntity player;
    protected int cookTime;
    protected boolean cycle;

    public static final int COOK_TIME = 20;

    public OvenContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, CookwareModule.OVEN_TILE.create().getInventory());
    }

    public OvenContainer(int id, PlayerInventory playerInventory, OvenTile tile) {
        this(id, playerInventory, tile.getInventory());
        this.tile = tile;
    }

    public OvenContainer(int id, PlayerInventory playerInventory, IInventory inventory) {
        super(CookwareModule.OVEN_CONTAINER, id);
        assertInventorySize(inventory, 12);
        this.player = playerInventory.player;

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new ModSlot(inventory, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }

        this.addSlot(new ModSlot(inventory, 9, 124, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new ModSlot(inventory, 10, 114, 5));
        this.addSlot(new ModSlot(inventory, 11, 134, 5));

        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(playerInventory, i1 + k * 9 + 9, 8 + i1 * 18, 84 + k * 18));
            }
        }

        for (int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 142));
        }

        trackInt(new IntReferenceHolder() {
            @Override
            public void set(int value) {
                cookTime = value;
            }

            @Override
            public int get() {
                return cookTime;
            }
        });
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int size = this.inventorySlots.size() - playerIn.inventory.mainInventory.size();
            if (index < size) {
                if (!this.mergeItemStack(itemstack1, size, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, size, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return !tile.isRemoved() && !(player.getDistanceSq(tile.getPos().getX() + 0.5D, tile.getPos().getY() + 0.5D, tile.getPos().getZ() + 0.5D) > 64.0D);
    }

    public void startCooking(boolean cycle) {
        if (tile == null || tile.isRemoved() || !cycle && !tile.canCook()) {
            return;
        }
        if (tile.getCookingPlayer() != null && tile.getCookingPlayer() != player) {
            return;
        }
        tile.startCooking(this);
        this.cycle = cycle;
        cookTime = COOK_TIME;
    }

    public void tick() {
        if (cookTime == 0) {
            return;
        }
        if (--cookTime == 0) {
            updateProgressBar(0, cookTime);
            ItemStack recipe = tile.getRecipeHandler().getStackInSlot(0);
            if (!recipe.isEmpty()) {
                RecordData data = RecipeItem.getData(recipe);
                if (data != null && data.getCookware() == tile.getCookware()) {
                    //TODO
                }
            }
            if (tile.cookAsItem(player, cycle) && cycle) {
                startCooking(true);
            } else {
                tile.stopCooking();
            }
        }
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        if (tile != null) {
            tile.stopCooking();
        }
    }

    public int getCookProgressionScaled() {
        int i = COOK_TIME - cookTime;
        return COOK_TIME != 0 && cookTime != 0 ? i * 24 / COOK_TIME : 0;
    }

}
