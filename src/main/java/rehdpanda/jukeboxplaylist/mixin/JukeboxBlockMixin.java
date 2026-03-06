package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.AbstractBlock;
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
import net.minecraft.world.block.WireOrientation;
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

@Mixin(value = AbstractBlock.class, priority = 500)
public abstract class JukeboxBlockMixin {
    @Inject(method = "neighborUpdate", at = @At("HEAD"))
    private void onNeighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
        if (!((Object)this instanceof JukeboxBlock)) return;
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
        if (!((Object)this instanceof JukeboxBlock)) return;
        // Logic handled by JukeboxBlockInteractionMixin
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUseJukebox(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!((Object)this instanceof JukeboxBlock)) return;
        // Logic handled by JukeboxBlockInteractionMixin
    }

    @Unique
    private void handleInteraction(World world, BlockPos pos, PlayerEntity player, ItemStack stack, CallbackInfoReturnable<ActionResult> cir) {
        // No-op, implementation moved to JukeboxBlockInteractionMixin
    }
}
