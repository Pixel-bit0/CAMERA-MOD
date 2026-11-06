package com.webscreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WebScreen extends Screen {
    private TextFieldWidget urlField;
    private boolean dragging, resizing, contentPressed;
    private int dragOffX, dragOffY;

    public WebScreen() {
        super(Text.literal("Web Screen"));
    }

    private WebConfig c() { return WebManager.cfg; }

    @Override
    protected void init() {
        WebConfig c = c();
        clampWindow();
        urlField = new TextFieldWidget(textRenderer, c.x + 52, c.y + WebConfig.TITLE_H + 2, c.w - 56, 14, Text.empty());
        urlField.setMaxLength(2048);
        urlField.setText(c.url);
        addDrawableChild(urlField);
        WebManager.ensureBrowser();
        WebManager.resizeBrowser();
        if (WebManager.browser != null) WebManager.browser.setFocus(true);
    }

    private void clampWindow() {
        WebConfig c = c();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        c.w = Math.max(WebConfig.MIN_W, Math.min(c.w, sw - 2));
        c.h = Math.max(WebConfig.MIN_H, Math.min(c.h, sh - 2));
        c.x = Math.max(0, Math.min(c.x, sw - c.w));
        c.y = Math.max(0, Math.min(c.y, sh - c.h));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean inContent(double mx, double my) {
        WebConfig c = c();
        return in(mx, my, c.contentX(), c.contentY(), c.contentW(), c.contentH());
    }

    private int bx(double mx) { return (int) ((mx - c().contentX()) * WebManager.scale()); }
    private int by(double my) { return (int) ((my - c().contentY()) * WebManager.scale()); }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        WebConfig c = c();
        WebManager.ensureBrowser();

        // window frame + title bar
        ctx.fill(c.x - 1, c.y - 1, c.x + c.w + 1, c.y + c.h + 1, 0xFF000000);
        ctx.fill(c.x, c.y, c.x + c.w, c.y + WebConfig.TITLE_H, 0xFF2B2B2B);
        ctx.drawText(textRenderer, "Web Screen  (drag here to move)", c.x + 4, c.y + 3, 0xFFFFFF, false);

        // title buttons
        boolean overClose = in(mx, my, c.x + c.w - 14, c.y, 14, 14);
        ctx.fill(c.x + c.w - 14, c.y, c.x + c.w, c.y + 14, overClose ? 0xFFCC3333 : 0xFF444444);
        ctx.drawText(textRenderer, "x", c.x + c.w - 9, c.y + 3, 0xFFFFFF, false);
        ctx.fill(c.x + c.w - 50, c.y, c.x + c.w - 15, c.y + 14, c.pinned ? 0xFF2E7D32 : 0xFF444444);
        ctx.drawText(textRenderer, c.pinned ? "Pinned" : "Pin", c.x + c.w - 47, c.y + 3, 0xFFFFFF, false);

        // toolbar
        int ty = c.y + WebConfig.TITLE_H;
        ctx.fill(c.x, ty, c.x + c.w, ty + WebConfig.TOOLBAR_H, 0xFF3C3C3C);
        drawBtn(ctx, c.x + 2, ty + 2, "<", mx, my);
        drawBtn(ctx, c.x + 18, ty + 2, ">", mx, my);
        drawBtn(ctx, c.x + 34, ty + 2, "R", mx, my);
        urlField.setX(c.x + 52);
        urlField.setY(ty + 2);
        urlField.setWidth(c.w - 56);
        if (!urlField.isFocused() && WebManager.browser != null) {
            String u = WebManager.browser.getURL();
            if (u != null && !u.isEmpty() && !u.equals(urlField.getText())) urlField.setText(u);
        }

        // page content
        ctx.fill(c.contentX(), c.contentY(), c.contentX() + c.contentW(), c.contentY() + c.contentH(), 0xFF101010);
        if (WebManager.browser != null) {
            if (!resizing) WebManager.resizeBrowser();
            WebManager.drawBrowser(ctx, c.contentX(), c.contentY(), c.contentW(), c.contentH());
        } else {
            String msg = WebManager.ready() ? "Starting browser..." : "Chromium is still loading (first launch downloads files)...";
            ctx.drawText(textRenderer, msg, c.contentX() + 8, c.contentY() + 8, 0xAAAAAA, false);
        }

        // resize handle
        ctx.fill(c.x + c.w - 10, c.y + c.h - 10, c.x + c.w, c.y + c.h, 0xCC888888);
        ctx.fill(c.x + c.w - 7, c.y + c.h - 4, c.x + c.w - 2, c.y + c.h - 3, 0xFF000000);
        ctx.fill(c.x + c.w - 4, c.y + c.h - 7, c.x + c.w - 3, c.y + c.h - 2, 0xFF000000);

        super.render(ctx, mx, my, delta); // draws the URL field on top
    }

    private void drawBtn(DrawContext ctx, int x, int y, String label, int mx, int my) {
        boolean over = in(mx, my, x, y, 14, 14);
        ctx.fill(x, y, x + 14, y + 14, over ? 0xFF666666 : 0xFF505050);
        ctx.drawText(textRenderer, label, x + 4, y + 3, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        WebConfig c = c();
        int ty = c.y + WebConfig.TITLE_H;

        if (in(mx, my, c.x + c.w - 14, c.y, 14, 14)) { close(); return true; }
        if (in(mx, my, c.x + c.w - 50, c.y, 35, 14)) { c.pinned = !c.pinned; c.save(); return true; }
        if (in(mx, my, c.x + c.w - 10, c.y + c.h - 10, 10, 10)) { resizing = true; return true; }
        if (in(mx, my, c.x, c.y, c.w - 50, WebConfig.TITLE_H)) {
            dragging = true;
            dragOffX = (int) mx - c.x;
            dragOffY = (int) my - c.y;
            return true;
        }
        if (in(mx, my, c.x + 2, ty + 2, 14, 14)) { if (WebManager.browser != null) WebManager.browser.goBack(); return true; }
        if (in(mx, my, c.x + 18, ty + 2, 14, 14)) { if (WebManager.browser != null) WebManager.browser.goForward(); return true; }
        if (in(mx, my, c.x + 34, ty + 2, 14, 14)) { if (WebManager.browser != null) WebManager.browser.reload(); return true; }

        if (urlField.isMouseOver(mx, my)) {
            if (WebManager.browser != null) WebManager.browser.setFocus(false);
            return super.mouseClicked(mx, my, button);
        }

        setFocused(null);
        if (inContent(mx, my) && WebManager.browser != null) {
            WebManager.browser.setFocus(true);
            WebManager.browser.sendMousePress(bx(mx), by(my), button);
            contentPressed = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        WebConfig c = c();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        if (dragging) {
            c.x = Math.max(0, Math.min((int) mx - dragOffX, sw - c.w));
            c.y = Math.max(0, Math.min((int) my - dragOffY, sh - c.h));
            return true;
        }
        if (resizing) {
            c.w = Math.max(WebConfig.MIN_W, Math.min((int) mx - c.x, sw - c.x));
            c.h = Math.max(WebConfig.MIN_H, Math.min((int) my - c.y, sh - c.y));
            return true;
        }
        if (contentPressed && WebManager.browser != null) {
            WebManager.browser.sendMouseMove(bx(mx), by(my));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            WebManager.resizeBrowser();
            c().save();
            return true;
        }
        if (contentPressed && WebManager.browser != null) {
            WebManager.browser.sendMouseRelease(bx(mx), by(my), button);
            contentPressed = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (inContent(mx, my) && WebManager.browser != null) {
            WebManager.browser.sendMouseMove(bx(mx), by(my));
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (inContent(mx, my) && WebManager.browser != null) {
            WebManager.browser.sendMouseWheel(bx(mx), by(my), amount, 0);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (urlField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                WebManager.navigate(urlField.getText());
                setFocused(null);
                if (WebManager.browser != null) WebManager.browser.setFocus(true);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (WebManager.browser != null) {
            WebManager.browser.sendKeyPress(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!urlField.isFocused() && WebManager.browser != null) {
            WebManager.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (urlField.isFocused()) return super.charTyped(chr, modifiers);
        if (WebManager.browser != null) {
            WebManager.browser.sendKeyTyped(chr, modifiers);
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        if (WebManager.browser != null) WebManager.browser.setFocus(false);
        c().save();
    }
}
