package rehdpanda.jukeboxplaylist.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;

public class JPClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(JPInit.JUKEBOX_PLAYLIST_HANDLER, JPJukeboxScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(JPInit.JukeboxStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BlockEntity be = context.client().world.getBlockEntity(payload.pos());
                if (be instanceof JukeboxBlockEntity jukebox) {
                    JukeboxPlaylistHolder accessor = (JukeboxPlaylistHolder) jukebox;
                    accessor.setPlaylistPlaying(payload.playing());
                    accessor.setPlaylistShuffle(payload.shuffle());
                    accessor.setPlaylistRepeat(payload.repeat());
                }
            });
        });
    }
}
