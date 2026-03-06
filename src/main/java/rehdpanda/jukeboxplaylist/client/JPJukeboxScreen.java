package rehdpanda.jukeboxplaylist.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JPJukeboxScreenHandler;
import rehdpanda.jukeboxplaylist.JukeboxPlaylistHolder;

public class JPJukeboxScreen extends HandledScreen<JPJukeboxScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    private static final Identifier GENERIC_54_TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    private ButtonWidget shuffleButton;
    private ButtonWidget repeatButton;
    private ButtonWidget playStopButton;
    private ButtonWidget skipForwardButton;
    private ButtonWidget skipBackwardButton;

    public JPJukeboxScreen(JPJukeboxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        JukeboxBlockEntity jukebox = handler.getJukebox();
        int offset = 3;
        if (jukebox == null) return;

        // Skip Backward button
        this.skipBackwardButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("⏮"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 4));
        }).dimensions(this.x + 12-offset, this.y + 42, 30, 20).tooltip(Tooltip.of(Text.literal("Skip Backward"))).build());

        // Play/Stop button
        this.playStopButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("▶/■"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 0));
        }).dimensions(this.x + 44-offset, this.y + 42, 30, 20).tooltip(Tooltip.of(Text.literal("Play/Stop"))).build());

        // Skip Forward button
        this.skipForwardButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("⏭"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 3));
        }).dimensions(this.x + 76-offset, this.y + 42, 30, 20).tooltip(Tooltip.of(Text.literal("Skip Forward"))).build());

        // Shuffle button
        this.shuffleButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("🔀"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 1));
        }).dimensions(this.x + 108-offset, this.y + 42, 30, 20).tooltip(Tooltip.of(Text.literal("Shuffle"))).build());

        // Repeat button
        this.repeatButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("🔁"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 2));
        }).dimensions(this.x + 140-offset, this.y + 42, 30, 20).tooltip(Tooltip.of(Text.literal("Repeat"))).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        
        // Draw the top 35 pixels (title and 1 row of slots)
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, GENERIC_54_TEXTURE, i, j, 0.0f, 0.0f, this.backgroundWidth, 35, 256, 256);
        
        // Fill the button area (35 to 73) with a generic GUI background color
        // or a blank part of the texture (x=0, y=7 in a container texture is often a blank gray line)
        for (int k = 0; k < 38; k++) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j + 35 + k, 0.0f, 7.0f, this.backgroundWidth, 1, 256, 256);
        }

        // Draw the player inventory part (starting at 73, 73 is the "Inventory" text background)
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j + 70, 0.0f, 126.0f, this.backgroundWidth, 93, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        JukeboxBlockEntity jukebox = handler.getJukebox();
        if (jukebox != null) {
            JukeboxPlaylistHolder accessor = (JukeboxPlaylistHolder) jukebox;
            if (accessor.isPlaylistPlaying()) {
                drawButtonHighlight(context, this.playStopButton);
            }
            if (accessor.isPlaylistShuffle()) {
                drawButtonHighlight(context, this.shuffleButton);
            }
            if (accessor.isPlaylistRepeat()) {
                drawButtonHighlight(context, this.repeatButton);
            }
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawButtonHighlight(DrawContext context, ButtonWidget button) {
        if (button == null) return;
        context.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), 0x4000FFFF);
    }
}
