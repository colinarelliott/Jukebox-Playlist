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
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;

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
                            return new JPJukeboxScreenHandler(syncId, playerInventory, ((JukeboxPlaylistHolder) jukebox).getPlaylistInventory(), jukebox);
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

    public record JukeboxStatePayload(BlockPos pos, boolean playing, boolean shuffle, boolean repeat) implements CustomPayload {
        public static final Id<JukeboxStatePayload> ID = new Id<>(Identifier.of(MOD_ID, "jukebox_state"));
        public static final PacketCodec<RegistryByteBuf, JukeboxStatePayload> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, JukeboxStatePayload::pos,
                PacketCodecs.BOOLEAN, JukeboxStatePayload::playing,
                PacketCodecs.BOOLEAN, JukeboxStatePayload::shuffle,
                PacketCodecs.BOOLEAN, JukeboxStatePayload::repeat,
                JukeboxStatePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Jukebox Playlist initializing...");

        PayloadTypeRegistry.playC2S().register(JukeboxActionPayload.ID, JukeboxActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JukeboxStatePayload.ID, JukeboxStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JukeboxActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                BlockEntity be = context.player().getEntityWorld().getBlockEntity(payload.pos());
                if (be instanceof JukeboxBlockEntity jukebox) {
                    JukeboxPlaylistHolder accessor = (JukeboxPlaylistHolder) jukebox;
                    LOGGER.info("Handling action {} for jukebox at {}", payload.actionId(), payload.pos());
                    switch (payload.actionId()) {
                        case 0 -> {
                            boolean playing = !accessor.isPlaylistPlaying();
                            accessor.setPlaylistPlaying(playing);
                            LOGGER.info("Toggled playing to {} for jukebox at {}", playing, payload.pos());
                            if (!playing) {
                                // STOP sequence
                                accessor.setPlaylistPlaying(false); // Explicitly set to false before anything else
                                jukebox.getManager().stopPlaying(be.getWorld(), be.getCachedState());
                                be.getWorld().syncWorldEvent(1011, payload.pos(), 0);
                                accessor.stopMusicSound(be.getWorld(), payload.pos());
                                accessor.setDisc(net.minecraft.item.ItemStack.EMPTY);
                                be.getWorld().setBlockState(payload.pos(), be.getCachedState().with(net.minecraft.block.JukeboxBlock.HAS_RECORD, false), 3);
                                be.markDirty();
                            } else {
                                // START sequence (if not already playing)
                                if (!jukebox.getManager().isPlaying()) {
                                    LOGGER.info("Jukebox not currently playing, skipping forward to start playlist at {}", payload.pos());
                                    accessor.skipForward();
                                } else {
                                    LOGGER.info("Jukebox already playing, will continue with playlist at {}", payload.pos());
                                }
                            }
                        }
                        case 1 -> {
                            accessor.setPlaylistShuffle(!accessor.isPlaylistShuffle());
                            LOGGER.info("Toggled shuffle to {} for jukebox at {}", accessor.isPlaylistShuffle(), payload.pos());
                        }
                        case 2 -> {
                            accessor.setPlaylistRepeat(!accessor.isPlaylistRepeat());
                            LOGGER.info("Toggled repeat to {} for jukebox at {}", accessor.isPlaylistRepeat(), payload.pos());
                        }
                        case 3 -> {
                            LOGGER.info("Manual skip forward for jukebox at {}", payload.pos());
                            accessor.skipForward();
                        }
                        case 4 -> {
                            LOGGER.info("Manual skip backward for jukebox at {}", payload.pos());
                            accessor.skipBackward();
                        }
                    }
                    // Notify clients about the state change
                    JukeboxStatePayload statePayload = new JukeboxStatePayload(payload.pos(), accessor.isPlaylistPlaying(), accessor.isPlaylistShuffle(), accessor.isPlaylistRepeat());
                    if (be.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                        for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
                            if (player.getBlockPos().isWithinDistance(payload.pos(), 64.0)) { // Sync to nearby players
                                ServerPlayNetworking.send(player, statePayload);
                            }
                        }
                    } else {
                        ServerPlayNetworking.send(context.player(), statePayload);
                    }
                    be.markDirty();
                }
            });
        });
    }
}
