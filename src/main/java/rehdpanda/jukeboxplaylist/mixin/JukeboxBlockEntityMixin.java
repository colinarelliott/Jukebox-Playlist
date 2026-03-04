package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity {
    @Unique
    private final SimpleInventory playlistInventory = new SimpleInventory(9);
    @Unique
    private boolean playlistPlaying = false;
    @Unique
    private boolean playlistShuffle = false;
    @Unique
    private boolean playlistRepeat = false;
    @Unique
    private int currentPlaylistSlot = -1;
    @Unique
    private int playlistCooldown = 0;
    @Unique
    private final Random random = new Random();

    public JukeboxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "readNbt", at = @At("TAIL"), remap = false)
    private void readPlaylistNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        this.playlistPlaying = nbt.getBoolean("PlaylistPlaying").orElse(false);
        this.playlistShuffle = nbt.getBoolean("PlaylistShuffle").orElse(false);
        this.playlistRepeat = nbt.getBoolean("PlaylistRepeat").orElse(false);
        this.currentPlaylistSlot = nbt.getInt("CurrentPlaylistSlot").orElse(-1);
        this.playlistCooldown = nbt.getInt("PlaylistCooldown").orElse(0);
    }

    @Inject(method = "writeNbt", at = @At("TAIL"), remap = false)
    private void writePlaylistNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        nbt.putBoolean("PlaylistPlaying", playlistPlaying);
        nbt.putBoolean("PlaylistShuffle", playlistShuffle);
        nbt.putBoolean("PlaylistRepeat", playlistRepeat);
        nbt.putInt("CurrentPlaylistSlot", currentPlaylistSlot);
        nbt.putInt("PlaylistCooldown", playlistCooldown);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void tickPlaylist(World world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient()) return;

        JukeboxBlockEntityMixin mixin = (JukeboxBlockEntityMixin) (Object) blockEntity;
        if (!mixin.playlistPlaying) {
            if (mixin.playlistCooldown > 0) {
                mixin.playlistCooldown = 0;
            }
            return;
        }

        if (mixin.playlistCooldown > 0) {
            mixin.playlistCooldown--;
            if (mixin.playlistCooldown <= 0) {
                // Song finished
                mixin.playNextFromPlaylist(world, pos, blockEntity);
            }
        } else if (!blockEntity.getManager().isPlaying()) {
            mixin.playNextFromPlaylist(world, pos, blockEntity);
        }
    }

    @Unique
    private void playNextFromPlaylist(World world, BlockPos pos, JukeboxBlockEntity jukebox) {
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < playlistInventory.size(); i++) {
            if (!playlistInventory.getStack(i).isEmpty()) {
                validSlots.add(i);
            }
        }

        if (validSlots.isEmpty()) {
            this.playlistPlaying = false;
            return;
        }

        int nextSlot = -1;
        if (this.playlistShuffle) {
            nextSlot = validSlots.get(random.nextInt(validSlots.size()));
        } else {
            for (int slot : validSlots) {
                if (slot > currentPlaylistSlot) {
                    nextSlot = slot;
                    break;
                }
            }
            if (nextSlot == -1 && this.playlistRepeat) {
                nextSlot = validSlots.get(0);
            }
        }

        if (nextSlot != -1) {
            this.currentPlaylistSlot = nextSlot;
            ItemStack stack = playlistInventory.getStack(nextSlot);
            JukeboxPlayableComponent playable = stack.get(DataComponentTypes.JUKEBOX_PLAYABLE);
            if (playable != null) {
                playable.song().resolveEntry(world.getRegistryManager()).ifPresent(songEntry -> {
                    this.playlistCooldown = (int) (songEntry.value().lengthInSeconds() * 20);
                    world.syncWorldEvent(null, 1010, pos, Item.getRawId(stack.getItem()));
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), songEntry.value().soundEvent().value(), SoundCategory.RECORDS, 3.0F, 1.0F);
                    this.markDirty();
                });
            }
        } else {
            this.playlistPlaying = false;
            this.currentPlaylistSlot = -1;
            this.markDirty();
        }
    }
}
