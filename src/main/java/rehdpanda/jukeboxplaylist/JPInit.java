package rehdpanda.jukeboxplaylist;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rehdpanda.jukeboxplaylist.mixin.JukeboxBlockEntityAccessor;

public class JPInit implements ModInitializer {
    public static final String MOD_ID = "jukebox-playlist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ScreenHandlerType<JPJukeboxScreenHandler> JUKEBOX_PLAYLIST_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "jukebox_playlist"),
            new ExtendedScreenHandlerType<>(
                    (syncId, playerInventory, payload) -> {
                        BlockPos pos = payload;
                        BlockEntity be = playerInventory.player.getEntityWorld().getBlockEntity(pos);
                        if (be instanceof JukeboxBlockEntity jukebox) {
                            return new JPJukeboxScreenHandler(syncId, playerInventory, ((JukeboxBlockEntityAccessor) jukebox).getPlaylistInventory(), jukebox);
                        }
                        return new JPJukeboxScreenHandler(syncId, playerInventory);
                    }, BlockPos.PACKET_CODEC
            )
    );

    public record JukeboxActionPayload(BlockPos pos, int actionId) implements CustomPayload {
        public static final Id<JukeboxActionPayload> ID = new Id<>(Identifier.of(MOD_ID, "jukebox_action"));
        public static final PacketCodec<RegistryByteBuf, JukeboxActionPayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, JukeboxActionPayload::pos,
                PacketCodecs.INTEGER, JukeboxActionPayload::actionId,
                JukeboxActionPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Jukebox Playlist initializing...");

        JPItems.register();

        PayloadTypeRegistry.playC2S().register(JukeboxActionPayload.ID, JukeboxActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JukeboxActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                BlockEntity be = context.player().getEntityWorld().getBlockEntity(payload.pos());
                if (be instanceof JukeboxBlockEntity jukebox) {
                    JukeboxBlockEntityAccessor accessor = (JukeboxBlockEntityAccessor) jukebox;
                    switch (payload.actionId()) {
                        case 0 -> {
                            boolean playing = !accessor.isPlaylistPlaying();
                            accessor.setPlaylistPlaying(playing);
                            if (!playing) {
                                jukebox.getManager().stopPlaying(be.getWorld(), be.getCachedState());
                            }
                        }
                        case 1 -> accessor.setPlaylistShuffle(!accessor.isPlaylistShuffle());
                        case 2 -> accessor.setPlaylistRepeat(!accessor.isPlaylistRepeat());
                    }
                }
            });
        });
    }
}
