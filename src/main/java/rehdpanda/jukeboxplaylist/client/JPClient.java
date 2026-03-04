package rehdpanda.jukeboxplaylist.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import rehdpanda.jukeboxplaylist.JPInit;

public class JPClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(JPInit.JUKEBOX_PLAYLIST_HANDLER, JPJukeboxScreen::new);
    }
}
