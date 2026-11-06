package com.webscreen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WebManager {
    public static final WebConfig cfg = WebConfig.load();
    public static MCEFBrowser browser;
    private static int pixelW = -1, pixelH = -1;

    private WebManager() {}

    public static boolean ready() {
        return MCEF.isInitialized();
    }

    /** Creates the Chromium browser the first time MCEF is ready. */
    public static void ensureBrowser() {
        if (browser == null && ready()) {
            browser = MCEF.createBrowser(cfg.url, false);
            pixelW = -1;
            resizeBrowser();
        }
    }

    public static double scale() {
        return MinecraftClient.getInstance().getWindow().getScaleFactor();
    }

    /** Matches Chromium's internal resolution to the on-screen size. */
    public static void resizeBrowser() {
        if (browser == null) return;
        double s = scale();
        int w = Math.max(1, (int) (cfg.contentW() * s));
        int h = Math.max(1, (int) (cfg.contentH() * s));
        if (w != pixelW || h != pixelH) {
            browser.resize(w, h);
            pixelW = w;
            pixelH = h;
        }
    }

    public static void navigate(String input) {
        String u = input == null ? "" : input.trim();
        if (u.isEmpty()) return;
        if (!u.contains("://")) {
            if (u.contains(" ") || !(u.contains(".") || u.startsWith("localhost"))) {
                u = "https://www.google.com/search?q=" + URLEncoder.encode(u, StandardCharsets.UTF_8);
            } else {
                u = "https://" + u;
            }
        }
        cfg.url = u;
        cfg.save();
        if (browser != null) browser.loadURL(u);
    }

    public static void drawBrowser(DrawContext ctx, int x, int y, int w, int h) {
        if (browser == null) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, x,     y + h, 0).texture(0f, 1f).color(255, 255, 255, 255).next();
        b.vertex(m, x + w, y + h, 0).texture(1f, 1f).color(255, 255, 255, 255).next();
        b.vertex(m, x + w, y,     0).texture(1f, 0f).color(255, 255, 255, 255).next();
        b.vertex(m, x,     y,     0).texture(0f, 0f).color(255, 255, 255, 255).next();
        Tessellator.getInstance().draw();
    }

    public static void shutdown() {
        cfg.save();
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }
}
