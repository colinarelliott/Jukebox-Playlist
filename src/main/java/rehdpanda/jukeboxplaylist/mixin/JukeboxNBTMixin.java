package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
public abstract class JukeboxNBTMixin extends BlockEntity implements JukeboxPlaylistHolder, SidedInventory {
    @Shadow public abstract void setDisc(ItemStack stack);
    @Shadow public abstract ItemStack getStack();

    public JukeboxNBTMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Unique private final SimpleInventory playlistInventory = new SimpleInventory(9);
    @Unique private boolean playlistPlaying = false;
    @Unique private boolean playlistShuffle = false;
    @Unique private boolean playlistRepeat = false;
    @Unique private int currentPlaylistSlot = -1;
    @Unique private boolean wasPowered = false;
    @Unique private final Random random = new Random();

    @Inject(method = "readData", at = @At("TAIL"))
    private void readPlaylistData(net.minecraft.storage.ReadView view, CallbackInfo ci) {
        if (view.getReadView("PlaylistInventory") != null) {
            this.playlistInventory.clear();
            net.minecraft.inventory.Inventories.readData(view.getReadView("PlaylistInventory"), this.playlistInventory.getHeldStacks());
        }
        this.playlistPlaying = view.getBoolean("PlaylistPlaying", false);
        this.playlistShuffle = view.getBoolean("PlaylistShuffle", false);
        this.playlistRepeat = view.getBoolean("PlaylistRepeat", false);
        this.currentPlaylistSlot = view.getInt("CurrentPlaylistSlot", -1);
        this.wasPowered = view.getBoolean("WasPowered", false);
    }

    @Inject(method = "writeData", at = @At("TAIL"))
    private void writePlaylistData(net.minecraft.storage.WriteView view, CallbackInfo ci) {
        net.minecraft.inventory.Inventories.writeData(view.get("PlaylistInventory"), this.playlistInventory.getHeldStacks());
        view.putBoolean("PlaylistPlaying", this.playlistPlaying);
        view.putBoolean("PlaylistShuffle", this.playlistShuffle);
        view.putBoolean("PlaylistRepeat", this.playlistRepeat);
        view.putInt("CurrentPlaylistSlot", this.currentPlaylistSlot);
        view.putBoolean("WasPowered", this.wasPowered);
    }

    @Override
    public void markRemoved() {
        // Do not drop here, it's too early for some removal types (like moving blocks or chunk unloading)
        super.markRemoved();
    }

    @Inject(method = "dropRecord", at = @At("HEAD"))
    private void dropPlaylistItems(CallbackInfo ci) {
        if (this.world != null && !this.world.isClient() && !this.playlistInventory.isEmpty()) {
            for (int i = 0; i < this.playlistInventory.size(); i++) {
                if (this.playlistPlaying && i == this.currentPlaylistSlot) {
                    continue;
                }
                
                ItemStack stack = this.playlistInventory.getStack(i);
                if (!stack.isEmpty()) {
                    net.minecraft.block.Block.dropStack(this.world, this.pos, stack.copy());
                }
            }
            this.playlistInventory.clear();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void tickPlaylist(World world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient()) return;

        JukeboxPlaylistHolder mixin = (JukeboxPlaylistHolder) blockEntity;
        if (!mixin.isPlaylistPlaying()) return;

        JukeboxNBTMixin mixinImpl = (JukeboxNBTMixin) (Object) blockEntity;

        if (!blockEntity.getManager().isPlaying() || blockEntity.getStack().isEmpty()) {
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
        int nextSlot = getNextSlot();
        if (nextSlot != -1) {
            playFromSlot(world, pos, jukebox, nextSlot);
        } else {
            this.playlistPlaying = false;
            this.currentPlaylistSlot = -1;
            this.markDirty();
        }
    }

    @Unique
    private int getNextSlot() {
        List<Integer> validSlots = getValidSlots();
        if (validSlots.isEmpty()) return -1;

        if (this.playlistShuffle) return validSlots.get(random.nextInt(validSlots.size()));

        for (int slot : validSlots) {
            if (slot > currentPlaylistSlot) return slot;
        }
        return this.playlistRepeat ? validSlots.get(0) : -1;
    }

    @Unique
    private List<Integer> getValidSlots() {
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < playlistInventory.size(); i++) {
            if (!playlistInventory.getStack(i).isEmpty()) validSlots.add(i);
        }
        return validSlots;
    }

    @Unique
    private void playFromSlot(World world, BlockPos pos, JukeboxBlockEntity jukebox, int slot) {
        this.currentPlaylistSlot = slot;
        ItemStack stack = playlistInventory.getStack(slot);
        net.minecraft.block.jukebox.JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), stack).ifPresentOrElse(songEntry -> {
            jukebox.getManager().stopPlaying(world, jukebox.getCachedState());
            world.syncWorldEvent(1011, pos, 0);
            this.stopMusicSound(world, pos);
            this.setDisc(ItemStack.EMPTY);
            world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, false), 3);

            this.setDisc(stack.copy());
            world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, true), 3);
            if (world instanceof ServerWorld serverWorld) jukebox.getManager().startPlaying(serverWorld, songEntry);
            this.markDirty();
        }, () -> {
            this.playlistPlaying = false;
            this.markDirty();
        });
    }

    @Override public SimpleInventory getPlaylistInventory() { return playlistInventory; }
    @Override public boolean isPlaylistPlaying() { return playlistPlaying; }
    @Override public void setPlaylistPlaying(boolean playing) { this.playlistPlaying = playing; }
    @Override public boolean isPlaylistShuffle() { return playlistShuffle; }
    @Override public void setPlaylistShuffle(boolean shuffle) { this.playlistShuffle = shuffle; }
    @Override public boolean isPlaylistRepeat() { return playlistRepeat; }
    @Override public void setPlaylistRepeat(boolean repeat) { this.playlistRepeat = repeat; }

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
        int prevSlot = getPrevSlot();
        if (prevSlot != -1) {
            playFromSlot(world, pos, jukebox, prevSlot);
        } else {
            this.playlistPlaying = false;
            this.currentPlaylistSlot = -1;
            this.markDirty();
        }
    }

    @Unique
    private int getPrevSlot() {
        List<Integer> validSlots = getValidSlots();
        if (validSlots.isEmpty()) return -1;

        if (this.playlistShuffle) return validSlots.get(random.nextInt(validSlots.size()));

        for (int i = validSlots.size() - 1; i >= 0; i--) {
            int slot = validSlots.get(i);
            if (slot < currentPlaylistSlot) return slot;
        }
        return this.playlistRepeat ? validSlots.get(validSlots.size() - 1) : -1;
    }

    @Override
    public boolean wasPowered() {
        return wasPowered;
    }

    @Override
    public void setWasPowered(boolean powered) {
        this.wasPowered = powered;
    }

    @Override
    public void togglePlayback() {
        World world = this.getWorld();
        BlockPos pos = this.getPos();
        if (world == null || world.isClient()) return;
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity) (Object) this;

        this.playlistPlaying = !this.playlistPlaying;
        if (!this.playlistPlaying) {
            jukebox.getManager().stopPlaying(world, jukebox.getCachedState());
            world.syncWorldEvent(1011, pos, 0);
            this.stopMusicSound(world, pos);
            this.setDisc(ItemStack.EMPTY);
            world.setBlockState(pos, jukebox.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, false), 3);
        } else if (!jukebox.getManager().isPlaying()) {
            this.skipForward();
        }
        this.markDirty();
        if (world instanceof ServerWorld serverWorld) {
            JPInit.JukeboxStatePayload statePayload = new JPInit.JukeboxStatePayload(pos, this.playlistPlaying, this.playlistShuffle, this.playlistRepeat);
            for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player.getBlockPos().isWithinDistance(pos, 64.0)) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, statePayload);
                }
            }
        }
    }

    // SidedInventory implementation
    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.contains(net.minecraft.component.DataComponentTypes.JUKEBOX_PLAYABLE);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    // Inventory delegation to playlistInventory
    @Override
    public int size() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        return playlistInventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return playlistInventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = playlistInventory.removeStack(slot, amount);
        if (!stack.isEmpty()) this.markDirty();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = playlistInventory.removeStack(slot);
        if (!stack.isEmpty()) this.markDirty();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        playlistInventory.setStack(slot, stack);
        this.markDirty();
    }

    @Override
    public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        return playlistInventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        playlistInventory.clear();
        this.markDirty();
    }
}
