package com.webscreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WebScreenMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.webscreen.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.webscreen"));
        KeyBinding pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.webscreen.pin", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.webscreen"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new WebScreen());
            }
            while (pinKey.wasPressed()) {
                WebManager.cfg.pinned = !WebManager.cfg.pinned;
                WebManager.cfg.save();
            }
            // keep Chromium alive/created even while only the pinned screen is showing
            if (WebManager.cfg.pinned) WebManager.ensureBrowser();
        });

        // Pinned screen: stays on your HUD while you play (not clickable).
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            WebConfig c = WebManager.cfg;
            if (!c.pinned || mc.currentScreen != null || WebManager.browser == null) return;
            WebManager.resizeBrowser();
            ctx.fill(c.contentX() - 1, c.contentY() - 1,
                    c.contentX() + c.contentW() + 1, c.contentY() + c.contentH() + 1, 0xFF000000);
            WebManager.drawBrowser(ctx, c.contentX(), c.contentY(), c.contentW(), c.contentH());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WebManager.shutdown());
    }
}
