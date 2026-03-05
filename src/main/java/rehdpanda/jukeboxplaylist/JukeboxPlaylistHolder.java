package rehdpanda.jukeboxplaylist;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

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
    void stopMusicSound(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos);
    void setDisc(ItemStack stack);
}
