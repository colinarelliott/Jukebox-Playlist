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
            JPInit.LOGGER.info("onStateReplaced HEAD: capturing playlist at {}", pos);
            // Capture all stacks at HEAD before the block entity is potentially removed
            SimpleInventory inv = ((JukeboxPlaylistHolder) be).getPlaylistInventory();
            List<Object> data = new ArrayList<>();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i).copy();
                if (!stack.isEmpty()) {
                    data.add(stack);
                    JPInit.LOGGER.info("Captured stack: {} from slot {}", stack, i);
                }
            }
            data.add(be); // Add the block entity itself at the end
            CACHED_DATA.set(data);
        }
    }

    @Unique
    private static final ThreadLocal<Object> CACHED_DATA = new ThreadLocal<>();

    @Inject(method = "onStateReplaced", at = @At("TAIL"))
    protected void onJukeboxStateReplacedTail(BlockState state, ServerWorld world, BlockPos pos, boolean moved, CallbackInfo ci) {
        if (moved) return;
        
        Object captured = CACHED_DATA.get();
        if (captured instanceof List<?> list) {
            List<Object> stacks = (List<Object>) captured;
            BlockState currentState = world.getBlockState(pos);
            boolean isReplaced = !currentState.isOf(state.getBlock());
            JPInit.LOGGER.info("onStateReplaced TAIL at {}: isReplaced={}, oldBlock={}, newBlock={}", pos, isReplaced, state.getBlock(), currentState.getBlock());

            if (isReplaced) {
                JPInit.LOGGER.info("Jukebox block replaced at {}, dropping playlist items from cached data", pos);
                
                JukeboxPlaylistHolder accessor = (JukeboxPlaylistHolder) stacks.remove(stacks.size() - 1);
                accessor.stopMusicSound(world, pos);
                world.syncWorldEvent(1011, pos, 0);

                for (Object obj : stacks) {
                    if (obj instanceof ItemStack stack && !stack.isEmpty()) {
                        JPInit.LOGGER.info("Dropping captured stack {} at {}", stack, pos);
                        net.minecraft.block.Block.dropStack(world, pos, stack);
                    }
                }
            }
        }
        CACHED_DATA.remove();
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
