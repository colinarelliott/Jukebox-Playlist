package rehdpanda.jukeboxplaylist;

import net.minecraft.inventory.SimpleInventory;

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
}
