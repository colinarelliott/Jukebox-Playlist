package rehdpanda.jukeboxplaylist.mixin;

import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(JukeboxBlockEntity.class)
public interface JukeboxBlockEntityAccessor {
    @Accessor(value = "playlistInventory", remap = false)
    SimpleInventory getPlaylistInventory();

    @Accessor(value = "playlistPlaying", remap = false)
    boolean isPlaylistPlaying();

    @Accessor(value = "playlistPlaying", remap = false)
    void setPlaylistPlaying(boolean playing);

    @Accessor(value = "playlistShuffle", remap = false)
    boolean isPlaylistShuffle();

    @Accessor(value = "playlistShuffle", remap = false)
    void setPlaylistShuffle(boolean shuffle);

    @Accessor(value = "playlistRepeat", remap = false)
    boolean isPlaylistRepeat();

    @Accessor(value = "playlistRepeat", remap = false)
    void setPlaylistRepeat(boolean repeat);
}
