package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rehdpanda.jukeboxplaylist.JPItems;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JPJukeboxScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(JukeboxBlock.class)
public class JukeboxBlockMixin {
    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void onUseWrench(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (stack.isOf(JPItems.WRENCH_ITEM)) {
            if (!world.isClient()) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox) {
                    player.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
                        @Override
                        public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
                            return pos;
                        }

                        @Override
                        public Text getDisplayName() {
                            return Text.translatable("container.jukebox_playlist");
                        }

                        @Override
                        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                            return new JPJukeboxScreenHandler(syncId, playerInventory, ((JukeboxBlockEntityAccessor) jukebox).getPlaylistInventory(), jukebox);
                        }
                    });
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
