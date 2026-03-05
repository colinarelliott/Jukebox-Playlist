package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JPJukeboxScreenHandler;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.ItemScatterer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.ArrayList;

@Mixin(JukeboxBlock.class)
public class JukeboxBlockMixin {
    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    protected void onJukeboxStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
        if (moved) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof JukeboxPlaylistHolder) {
            JPInit.LOGGER.info("onStateReplaced HEAD: capturing playlist at {} [BE: {}, pos: {}]", pos, be, be.getPos());
            // Capture all stacks at HEAD before the block entity is potentially removed
            SimpleInventory inv = ((JukeboxPlaylistHolder) be).getPlaylistInventory();
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i).copy();
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                    JPInit.LOGGER.info("Captured stack: {} from slot {}", stack, i);
                }
            }
            if (!stacks.isEmpty()) {
                CACHED_STACKS.set(stacks);
                JPInit.LOGGER.info("Successfully cached {} stacks in ThreadLocal", stacks.size());
            } else {
                CACHED_STACKS.remove();
                JPInit.LOGGER.info("No items in playlist to cache at HEAD");
            }
        } else {
            JPInit.LOGGER.info("onStateReplaced HEAD at {}: BlockEntity is NOT a JukeboxPlaylistHolder [BE: {}]", pos, be);
            CACHED_STACKS.remove();
        }
    }

    @Unique
    private static final ThreadLocal<List<ItemStack>> CACHED_STACKS = new ThreadLocal<>();

    @Inject(method = "onStateReplaced", at = @At("TAIL"))
    protected void onJukeboxStateReplacedTail(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
        if (moved) return;
        
        List<ItemStack> stacks = CACHED_STACKS.get();
        BlockState currentState = world.getBlockState(pos);
        boolean isReplaced = !currentState.isOf(state.getBlock());
        JPInit.LOGGER.info("onStateReplaced TAIL at {}: isReplaced={}, oldBlock={}, newBlock={}", pos, isReplaced, state.getBlock(), currentState.getBlock());

        if (isReplaced) {
            // Stop music logic (we don't need the BE for this, we have world and pos)
            net.minecraft.network.packet.s2c.play.StopSoundS2CPacket stopPacket = new net.minecraft.network.packet.s2c.play.StopSoundS2CPacket(null, net.minecraft.sound.SoundCategory.RECORDS);
            for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
                if (player.getBlockPos().isWithinDistance(pos, 64.0)) {
                    player.networkHandler.sendPacket(stopPacket);
                }
            }
            world.syncWorldEvent(1011, pos, 0);

            if (stacks != null && !stacks.isEmpty()) {
                JPInit.LOGGER.info("Jukebox block replaced at {}, dropping playlist items from cached data. Total stacks: {}", pos, stacks.size());
                for (ItemStack stack : stacks) {
                    JPInit.LOGGER.info("Dropping captured stack {} at {}", stack, pos);
                    net.minecraft.block.Block.dropStack(world, pos, stack);
                }
            } else {
                JPInit.LOGGER.info("onStateReplaced TAIL at {}: No cached stacks to drop.", pos);
            }
        } else {
            JPInit.LOGGER.info("onStateReplaced TAIL at {}: Jukebox NOT replaced (probably just property change), skipping drop.", pos);
        }
        CACHED_STACKS.remove();
    }

    @Inject(method = "neighborUpdate", at = @At("HEAD"))
    private void onNeighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, net.minecraft.world.block.WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
        if (world.isClient()) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof JukeboxPlaylistHolder accessor) {
            boolean isPowered = world.isReceivingRedstonePower(pos);
            if (isPowered && !accessor.wasPowered()) {
                accessor.togglePlayback();
            }
            accessor.setWasPowered(isPowered);
        }
    }

    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void onUseJukeboxWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        handleInteraction(world, pos, player, stack, cir);
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUseJukebox(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        handleInteraction(world, pos, player, player.getStackInHand(Hand.MAIN_HAND), cir);
    }

    private void handleInteraction(World world, BlockPos pos, PlayerEntity player, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof JukeboxBlockEntity jukebox) {
                if (!stack.isEmpty() && stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
                    SimpleInventory inventory = ((JukeboxPlaylistHolder) jukebox).getPlaylistInventory();
                    ItemStack remaining = inventory.addStack(stack.copy());
                    if (remaining.getCount() < stack.getCount()) {
                        stack.setCount(remaining.getCount());
                        jukebox.markDirty();
                        cir.setReturnValue(ActionResult.SUCCESS);
                        return;
                    }
                }

                player.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
                    @Override
                    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
                        return pos;
                    }

                    @Override
                    public Text getDisplayName() {
                        return Text.literal("Jukebox");
                    }

                    @Override
                    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                        return new JPJukeboxScreenHandler(syncId, playerInventory, ((JukeboxPlaylistHolder) jukebox).getPlaylistInventory(), jukebox);
                    }
                });
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
