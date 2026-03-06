package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;

@Mixin(value = AbstractBlock.class, priority = 500)
public abstract class JukeboxBlockMixin {
    @Inject(method = "neighborUpdate", at = @At("HEAD"))
    private void onNeighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
        if (!((Object)this instanceof JukeboxBlock) || world.isClient()) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof JukeboxPlaylistHolder accessor) {
            boolean isPowered = world.isReceivingRedstonePower(pos);
            if (isPowered && !accessor.wasPowered()) {
                accessor.togglePlayback();
            }
            accessor.setWasPowered(isPowered);
        }
    }
}
