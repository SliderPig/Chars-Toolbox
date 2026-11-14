package com.example.chartoolbox.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

// Rounded shapes with soft edges, so nothing on the screen is a hard-cornered box. The same drawing as in Char's Config Helper and Char's Powers.
public final class Soft {
    private Soft() {}

    // ---- Smooth shapes made of real triangles: they work in any units (GUI units are fine), any pose, and their edges fade across
    // one real screen pixel, so nothing looks stepped at any GUI scale. They only add to the batch: draw them inside g.drawManaged,
    // or call g.flush() afterwards. Blits don't go through the batch, so flush before one.

    private static final int MOST_POINTS = 512;
    private static final float[] XS = new float[MOST_POINTS], YS = new float[MOST_POINTS], XS2 = new float[MOST_POINTS], YS2 = new float[MOST_POINTS];

    // How big one real screen pixel is in the units being drawn in right now.
    private static float pixel(GuiGraphics g) {
        float units = Math.abs(g.pose().last().pose().m00()) * (float) Minecraft.getInstance().getWindow().getGuiScale();
        return units > 0.0001f ? 1f / units : 1f;
    }

    private static int segments(float r, float pixel) {
        return Mth.clamp((int) Math.ceil(Math.sqrt(r / pixel) * 1.6f), 1, 24);
    }

    public static int alpha(int colour, float amount) {
        return (Mth.clamp((int) (((colour >>> 24) & 0xFF) * amount), 0, 255) << 24) | (colour & 0xFFFFFF);
    }

    // Blends two colours, alpha too.
    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        return ((int) (aa + ((b >>> 24) - aa) * t) << 24) | ((int) (ar + (((b >> 16) & 255) - ar) * t) << 16) | ((int) (ag + (((b >> 8) & 255) - ag) * t) << 8) | (int) (ab + ((b & 255) - ab) * t);
    }

    // A slow start and a gentle stop, for t from 0 to 1.
    public static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    // A quick start and a long soft stop, for things sliding into place.
    public static float easeOut(float t) {
        t = 1f - Mth.clamp(t, 0f, 1f);
        return 1f - t * t * t;
    }

    // The outline of a rounded box grown by off (shrunk if negative), clockwise on screen from the top-left corner's left end. Returns the number of points.
    private static int contour(float[] xs, float[] ys, float x0, float y0, float x1, float y1, float rtl, float rtr, float rbr, float rbl, float off, int n) {
        float l = x0 - off, t = y0 - off, e = x1 + off, b = y1 + off;
        float most = Math.max(0f, Math.min(e - l, b - t) / 2f);
        float a = Math.min(Math.max(0f, rtl + off), most), c = Math.min(Math.max(0f, rtr + off), most), d = Math.min(Math.max(0f, rbr + off), most), f = Math.min(Math.max(0f, rbl + off), most);
        int p = 0;
        p = corner(xs, ys, p, l + a, t + a, a, Mth.PI, n);
        p = corner(xs, ys, p, e - c, t + c, c, Mth.PI * 1.5f, n);
        p = corner(xs, ys, p, e - d, b - d, d, 0f, n);
        p = corner(xs, ys, p, l + f, b - f, f, Mth.PI * 0.5f, n);
        return p;
    }

    private static int corner(float[] xs, float[] ys, int p, float cx, float cy, float r, float from, int n) {
        for (int k = 0; k <= n; k++) {
            float angle = from + k * Mth.HALF_PI / n;
            xs[p] = cx + Mth.cos(angle) * r;
            ys[p] = cy + Mth.sin(angle) * r;
            p++;
        }
        return p;
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, int colour) {
        v.vertex(m, x, y, 0f).color((colour >> 16) & 255, (colour >> 8) & 255, colour & 255, colour >>> 24).endVertex();
    }

    // The colour of a top-to-bottom fade at height y.
    private static int shade(float y, float y0, float y1, int top, int bottom, float amount) {
        int c = top == bottom ? top : mix(top, bottom, (y - y0) / Math.max(0.001f, y1 - y0));
        return amount >= 1f ? c : alpha(c, amount);
    }

    // A band between two outlines.
    private static void band(VertexConsumer v, Matrix4f m, float[] ox, float[] oy, float[] ix, float[] iy, int count, float y0, float y1, int top, int bottom, float outer, float inner) {
        for (int i = 0; i < count; i++) {
            int j = i + 1 == count ? 0 : i + 1;
            vertex(v, m, ox[i], oy[i], shade(oy[i], y0, y1, top, bottom, outer));
            vertex(v, m, ix[i], iy[i], shade(iy[i], y0, y1, top, bottom, inner));
            vertex(v, m, ix[j], iy[j], shade(iy[j], y0, y1, top, bottom, inner));
            vertex(v, m, ox[j], oy[j], shade(oy[j], y0, y1, top, bottom, outer));
        }
    }

    private static void fan(VertexConsumer v, Matrix4f m, float[] xs, float[] ys, int count, float y0, float y1, int top, int bottom) {
        float cx = 0, cy = 0;
        for (int i = 0; i < count; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= count;
        cy /= count;
        int mid = shade(cy, y0, y1, top, bottom, 1f);
        for (int i = 0; i < count; i++) {
            int j = i + 1 == count ? 0 : i + 1;
            int ci = shade(ys[i], y0, y1, top, bottom, 1f);
            vertex(v, m, cx, cy, mid);
            vertex(v, m, xs[j], ys[j], shade(ys[j], y0, y1, top, bottom, 1f));
            vertex(v, m, xs[i], ys[i], ci);
            vertex(v, m, xs[i], ys[i], ci);
        }
    }

    // A rounded box, each corner its own radius, fading from top to bottom.
    public static void shape(GuiGraphics g, float x0, float y0, float x1, float y1, float rtl, float rtr, float rbr, float rbl, int top, int bottom) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0) return;
        float pixel = pixel(g), half = pixel / 2f;
        int n = segments(Math.max(Math.max(rtl, rtr), Math.max(rbr, rbl)), pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int count = contour(XS, YS, x0, y0, x1, y1, rtl, rtr, rbr, rbl, -half, n);
        contour(XS2, YS2, x0, y0, x1, y1, rtl, rtr, rbr, rbl, half, n);
        fan(v, m, XS, YS, count, y0, y1, top, bottom);
        band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 0f, 1f);
    }

    public static void round(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int top, int bottom) {
        shape(g, x0, y0, x1, y1, r, r, r, r, top, bottom);
    }

    public static void round(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int colour) {
        shape(g, x0, y0, x1, y1, r, r, r, r, colour, colour);
    }

    public static void disc(GuiGraphics g, float cx, float cy, float r, int colour) {
        round(g, cx - r, cy - r, cx + r, cy + r, r, colour);
    }

    // A thin line round a rounded box, just inside its edge. Can fade from top to bottom too.
    public static void ring(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float thick, int top, int bottom) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0) return;
        float pixel = pixel(g), half = pixel / 2f;
        thick = Math.max(thick, pixel);
        int n = segments(r, pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int count = contour(XS, YS, x0, y0, x1, y1, r, r, r, r, half, n);
        contour(XS2, YS2, x0, y0, x1, y1, r, r, r, r, -half, n);
        band(v, m, XS, YS, XS2, YS2, count, y0, y1, top, bottom, 0f, 1f);
        if (thick > pixel) {
            contour(XS, YS, x0, y0, x1, y1, r, r, r, r, half - thick, n);
            band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 1f, 1f);
            System.arraycopy(XS, 0, XS2, 0, count);
            System.arraycopy(YS, 0, YS2, 0, count);
        }
        contour(XS, YS, x0, y0, x1, y1, r, r, r, r, -thick - half, n);
        band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 1f, 0f);
    }

    public static void ring(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float thick, int colour) {
        ring(g, x0, y0, x1, y1, r, thick, colour, colour);
    }

    // A soft shadow or glow round a box: strongest just inside the edge, gone blur further out. inside = also fill under the box.
    public static void shadow(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float blur, int colour, boolean inside) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0 || (colour >>> 24) == 0) return;
        float pixel = pixel(g);
        int n = segments(r + blur, pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float start = -Math.min(blur * 0.35f, Math.min(x1 - x0, y1 - y0) / 2f);
        int count = contour(XS, YS, x0, y0, x1, y1, r, r, r, r, start, n);
        if (inside) fan(v, m, XS, YS, count, y0, y1, colour, colour);
        int steps = Mth.clamp((int) (blur / pixel / 2f), 3, 7);
        float before = 1f;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps, off = start + (blur - start) * t, fade = (1f - t) * (1f - t);
            contour(XS2, YS2, x0, y0, x1, y1, r, r, r, r, off, n);
            band(v, m, XS2, YS2, XS, YS, count, y0, y1, colour, colour, fade, before);
            System.arraycopy(XS2, 0, XS, 0, count);
            System.arraycopy(YS2, 0, YS, 0, count);
            before = fade;
        }
    }
}
