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
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxNBTMixin extends BlockEntity implements JukeboxPlaylistHolder {
    @Shadow
    public abstract void setDisc(ItemStack stack);
    public JukeboxNBTMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

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

    @Inject(method = "readData", at = @At("TAIL"))
    private void readPlaylistData(net.minecraft.storage.ReadView view, CallbackInfo ci) {
        JPInit.LOGGER.info("readPlaylistData called for jukebox at {}", this.pos);
        if (view.getReadView("PlaylistInventory") != null) {
            this.playlistInventory.clear();
            net.minecraft.inventory.Inventories.readData(view.getReadView("PlaylistInventory"), this.playlistInventory.getHeldStacks());
            JPInit.LOGGER.info("Read playlist inventory at {}", this.pos);
        } else {
            JPInit.LOGGER.info("No PlaylistInventory found in ReadView for jukebox at {}", this.pos);
        }
        this.playlistPlaying = view.getBoolean("PlaylistPlaying", false);
        this.playlistShuffle = view.getBoolean("PlaylistShuffle", false);
        this.playlistRepeat = view.getBoolean("PlaylistRepeat", false);
        this.currentPlaylistSlot = view.getInt("CurrentPlaylistSlot", -1);
        this.playlistCooldown = view.getInt("PlaylistCooldown", 0);
    }

    @Inject(method = "writeData", at = @At("TAIL"))
    private void writePlaylistData(net.minecraft.storage.WriteView view, CallbackInfo ci) {
        JPInit.LOGGER.info("writePlaylistData called for jukebox at {}", this.pos);
        net.minecraft.inventory.Inventories.writeData(view.get("PlaylistInventory"), this.playlistInventory.getHeldStacks());
        view.putBoolean("PlaylistPlaying", this.playlistPlaying);
        view.putBoolean("PlaylistShuffle", this.playlistShuffle);
        view.putBoolean("PlaylistRepeat", this.playlistRepeat);
        view.putInt("CurrentPlaylistSlot", this.currentPlaylistSlot);
        view.putInt("PlaylistCooldown", this.playlistCooldown);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void tickPlaylist(World world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient()) return;

        JukeboxPlaylistHolder mixin = (JukeboxPlaylistHolder) blockEntity;
        if (!mixin.isPlaylistPlaying()) {
            return;
        }

        JukeboxNBTMixin mixinImpl = (JukeboxNBTMixin) (Object) blockEntity;

        if (!blockEntity.getManager().isPlaying()) {
            JPInit.LOGGER.info("Jukebox not playing at {} (manager state: {}), starting next", pos, blockEntity.getManager().isPlaying());
            // Mark dirty to ensure it persists if it was about to stop
            blockEntity.markDirty();
            mixinImpl.playNextFromPlaylist(world, pos, blockEntity);
        }
    }

    @Override
    public void stopMusicSound(World world, BlockPos pos) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            net.minecraft.network.packet.s2c.play.StopSoundS2CPacket stopPacket = new net.minecraft.network.packet.s2c.play.StopSoundS2CPacket(null, net.minecraft.sound.SoundCategory.RECORDS);
            for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player.getBlockPos().isWithinDistance(pos, 64.0)) {
                    player.networkHandler.sendPacket(stopPacket);
                }
            }
        }
    }

    @Unique
    private void playNextFromPlaylist(World world, BlockPos pos, JukeboxBlockEntity jukebox) {
        JPInit.LOGGER.info("playNextFromPlaylist called at {}", pos);
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < playlistInventory.size(); i++) {
            if (!playlistInventory.getStack(i).isEmpty()) {
                validSlots.add(i);
            }
        }
        JPInit.LOGGER.info("Valid playlist slots: {}", validSlots);

        if (validSlots.isEmpty()) {
            JPInit.LOGGER.info("Playlist is empty, stopping playback at {}", pos);
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
            JPInit.LOGGER.info("Selected slot {} with item {} to play at {}", nextSlot, stack, pos);
            
            net.minecraft.block.jukebox.JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), stack).ifPresentOrElse(songEntry -> {
                // Stop any current music first to avoid "starting new copy"
                jukebox.getManager().stopPlaying(world, jukebox.getCachedState());
                world.syncWorldEvent(1011, pos, 0);
                this.stopMusicSound(world, pos);
                this.setDisc(ItemStack.EMPTY);
                world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, false), 3);
                
                JPInit.LOGGER.info("Starting song {} at {}", songEntry.getKey().map(k -> k.getValue().toString()).orElse("unknown"), pos);
                
                // Set the disc in the jukebox manager to actually start playing
                this.setDisc(stack.copy());
                world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, true), 3);
                if (world instanceof ServerWorld serverWorld) {
                    jukebox.getManager().startPlaying(serverWorld, songEntry);
                }
                
                this.markDirty();
            }, () -> {
                JPInit.LOGGER.info("Could not resolve song from stack {}, stopping playback at {}", stack, pos);
                this.playlistPlaying = false;
                this.markDirty();
            });
        } else {
            JPInit.LOGGER.info("No next slot found, stopping playback at {}", pos);
            this.playlistPlaying = false;
            this.currentPlaylistSlot = -1;
            this.markDirty();
        }
    }

    @Override
    public SimpleInventory getPlaylistInventory() {
        return playlistInventory;
    }

    @Override
    public boolean isPlaylistPlaying() {
        return playlistPlaying;
    }

    @Override
    public void setPlaylistPlaying(boolean playing) {
        this.playlistPlaying = playing;
    }

    @Override
    public boolean isPlaylistShuffle() {
        return playlistShuffle;
    }

    @Override
    public void setPlaylistShuffle(boolean shuffle) {
        this.playlistShuffle = shuffle;
    }

    @Override
    public boolean isPlaylistRepeat() {
        return playlistRepeat;
    }

    @Override
    public void setPlaylistRepeat(boolean repeat) {
        this.playlistRepeat = repeat;
    }

    @Override
    public void skipForward() {
        if (this.getWorld() != null && !this.getWorld().isClient()) {
            this.playNextFromPlaylist(this.getWorld(), this.getPos(), (JukeboxBlockEntity) (Object) this);
        }
    }

    @Override
    public void skipBackward() {
        if (this.getWorld() != null && !this.getWorld().isClient()) {
            this.playPreviousFromPlaylist(this.getWorld(), this.getPos(), (JukeboxBlockEntity) (Object) this);
        }
    }

    @Unique
    private void playPreviousFromPlaylist(World world, BlockPos pos, JukeboxBlockEntity jukebox) {
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

        int prevSlot = -1;
        if (this.playlistShuffle) {
            prevSlot = validSlots.get(random.nextInt(validSlots.size()));
        } else {
            for (int i = validSlots.size() - 1; i >= 0; i--) {
                int slot = validSlots.get(i);
                if (slot < currentPlaylistSlot) {
                    prevSlot = slot;
                    break;
                }
            }
            if (prevSlot == -1 && this.playlistRepeat) {
                prevSlot = validSlots.get(validSlots.size() - 1);
            }
        }

        if (prevSlot != -1) {
            this.currentPlaylistSlot = prevSlot;
            ItemStack stack = playlistInventory.getStack(prevSlot);
            net.minecraft.block.jukebox.JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), stack).ifPresentOrElse(songEntry -> {
                // Stop any current music first to avoid "starting new copy"
                jukebox.getManager().stopPlaying(world, jukebox.getCachedState());
                world.syncWorldEvent(1011, pos, 0);
                this.stopMusicSound(world, pos);
                this.setDisc(ItemStack.EMPTY);
                world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, false), 3);
                
                // Set the disc in the jukebox manager to actually start playing
                this.setDisc(stack.copy());
                world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, true), 3);
                if (world instanceof ServerWorld serverWorld) {
                    jukebox.getManager().startPlaying(serverWorld, songEntry);
                }
                
                this.markDirty();
            }, () -> {
                JPInit.LOGGER.info("Could not resolve previous song from stack {}, stopping playback at {}", stack, pos);
                this.playlistPlaying = false;
                this.markDirty();
            });
        } else {
            // If no previous and no repeat, we might want to stop or just stay on current?
            // Original behavior for next if not found is stop.
            this.playlistPlaying = false;
            this.currentPlaylistSlot = -1;
            this.markDirty();
        }
    }
}
