package rehdpanda.jukeboxplaylist;

import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class JPJukeboxScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final JukeboxBlockEntity jukebox;

    public JPJukeboxScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(9), null);
    }

    public JPJukeboxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, JukeboxBlockEntity jukebox) {
        super(JPInit.JUKEBOX_PLAYLIST_HANDLER, syncId);
        checkSize(inventory, 9);
        this.inventory = inventory;
        this.jukebox = jukebox;

        inventory.onOpen(playerInventory.player);

        // Jukebox Playlist (1x9)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 18) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE);
                }
            });
        }

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public JukeboxBlockEntity getJukebox() {
        return jukebox;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player) && (jukebox == null || (jukebox.getWorld().getBlockEntity(jukebox.getPos()) == jukebox && player.squaredDistanceTo(jukebox.getPos().toCenterPos()) <= 64.0));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < 9) {
                if (!this.insertItem(stack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(stack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
