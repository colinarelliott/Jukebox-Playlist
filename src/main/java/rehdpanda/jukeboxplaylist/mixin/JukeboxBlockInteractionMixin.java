package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JPJukeboxScreenHandler;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockInteractionMixin {

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
