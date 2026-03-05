package rehdpanda.jukeboxplaylist;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

public interface JukeboxPlaylistHolder {
    SimpleInventory getPlaylistInventory();
    boolean isPlaylistPlaying();
    void setPlaylistPlaying(boolean playing);
    boolean isPlaylistShuffle();
    void setPlaylistShuffle(boolean shuffle);
    boolean isPlaylistRepeat();
    void setPlaylistRepeat(boolean repeat);
    void skipForward();
    void skipBackward();
    void stopMusicSound(World world, BlockPos pos);
    void setDisc(ItemStack stack);
    void togglePlayback();
    boolean wasPowered();
    void setWasPowered(boolean powered);
}
