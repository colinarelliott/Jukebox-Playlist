package rehdpanda.jukeboxplaylist.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rehdpanda.jukeboxplaylist.JPInit;
import rehdpanda.jukeboxplaylist.JPJukeboxScreenHandler;
import rehdpanda.jukeboxplaylist.mixin.JukeboxBlockEntityAccessor;

public class JPJukeboxScreen extends HandledScreen<JPJukeboxScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    private ButtonWidget shuffleButton;
    private ButtonWidget repeatButton;
    private ButtonWidget playStopButton;

    public JPJukeboxScreen(JPJukeboxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        JukeboxBlockEntity jukebox = handler.getJukebox();
        if (jukebox == null) return;

        // Play/Stop button
        this.playStopButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("▶/■"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 0));
        }).dimensions(this.x + 38, this.y + 42, 30, 20).build());

        // Shuffle button
        this.shuffleButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("🔀"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 1));
        }).dimensions(this.x + 73, this.y + 42, 30, 20).build());

        // Repeat button
        this.repeatButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("🔁"), button -> {
            ClientPlayNetworking.send(new JPInit.JukeboxActionPayload(jukebox.getPos(), 2));
        }).dimensions(this.x + 108, this.y + 42, 30, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0f, 0.0f, this.backgroundWidth, 35, 256, 256);
        
        for (int g = 0; g < 9; g++) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, i, g*5 + 70, 0.0f, 6.0f, this.backgroundWidth, 5, 256, 256);
        }
        
        Identifier DISPENSER_TEXTURE = Identifier.of("minecraft", "textures/gui/container/dispenser.png");
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, DISPENSER_TEXTURE, i, j + 75, 0.0f, 75.0f, this.backgroundWidth, 91, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        JukeboxBlockEntity jukebox = handler.getJukebox();
        if (jukebox != null) {
            JukeboxBlockEntityAccessor accessor = (JukeboxBlockEntityAccessor) jukebox;
            if (accessor.isPlaylistPlaying()) {
                drawButtonHighlight(context, this.playStopButton);
            }
            if (accessor.isPlaylistShuffle()) {
                drawButtonHighlight(context, this.shuffleButton);
            }
            if (accessor.isPlaylistRepeat()) {
                drawButtonHighlight(context, this.repeatButton);
            }

            context.drawText(this.textRenderer, "Play: " + (accessor.isPlaylistPlaying() ? "ON" : "OFF"), this.x + 8, this.y + 68, 0x404040, false);
            context.drawText(this.textRenderer, "Shuffle: " + (accessor.isPlaylistShuffle() ? "ON" : "OFF"), this.x + 140, this.y + 40, 0x404040, false);
            context.drawText(this.textRenderer, "Repeat: " + (accessor.isPlaylistRepeat() ? "ON" : "OFF"), this.x + 140, this.y + 55, 0x404040, false);
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawButtonHighlight(DrawContext context, ButtonWidget button) {
        if (button == null) return;
        context.fill(button.getX() - 1, button.getY() - 1, button.getX() + button.getWidth() + 1, button.getY() + button.getHeight() + 1, 0x4000FFFF);
    }
}
