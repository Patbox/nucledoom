package eu.pb4.nucledoom.othergame.rendr;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Arrays;

public class RenderOutput implements Texture {
    private final int width;
    private final int height;
    private int[] screen;
    private float[] depth;

    public RenderOutput(int width, int height) {
        this.width = width;
        this.height = height;
        var size = width * height;
        this.screen = new int[size];
        this.depth = new float[size];
        this.clear();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public int get(int x, int y) {
        return this.screen[index(x, y)];
    }

    @Override
    public void set(int x, int y, int color) {
        this.screen[index(x, y)] = color;
    }

    public boolean depthTest(int x, int y, float z) {
        return depth[index(x, y)] >= z;
    }

    public void setDepth(int x, int y, float z) {
        depth[index(x, y)] = z;
    }

    public void clear(int background) {
        Arrays.fill(this.screen, background);
        Arrays.fill(depth, Float.POSITIVE_INFINITY);
    }

    public void clear() {
        clear(0xFF000000);
    }

    public void drawQuad(Vector3f[] vec, Vector2f[] uv, @Nullable Texture image, int color) {
        drawQuad(0, vec, uv, image, color);
    }
    public void drawQuad(int offset, Vector3f[] vec, Vector2f[] uv, @Nullable Texture image, int color) {
        drawTriangle(offset, 0, vec, uv, image, color);
        drawTriangle(offset, 2, vec, uv, image, color);
    }

    private float signedTriangleArea(float ax, float ay, float bx, float by, float cx, float cy) {
        return 0.5f * ((by - ay) * (bx + ax) + (cy - by) * (cx + bx) + (ay - cy) * (ax + cx));
    }

    public void drawTriangle(Vector3f[] vec, Vector2f[] uv, @Nullable Texture image, int color) {
        drawTriangle(vec, uv, image, color);
    }
    public void drawTriangle(int offset, int index, Vector3f[] vec, Vector2f[] uv, @Nullable Texture image, int color) {
        var vec0 = vec[offset + (index % 4)];
        var vec1 = vec[offset + ((index + 1) % 4)];
        var vec2 = vec[offset + ((index + 2) % 4)];

        var uv0 = uv[offset + (index % 4)];
        var uv1 = uv[offset + ((index + 1) % 4)];
        var uv2 = uv[offset + ((index + 2) % 4)];

        var minX = Mth.clamp((int) Math.min(vec0.x, Math.min(vec1.x, vec2.x)), 0, width - 1);
        var maxX = Mth.clamp((int) Math.max(vec0.x, Math.max(vec1.x, vec2.x)), 0, width - 1);
        var minY = Mth.clamp((int) Math.min(vec0.y, Math.min(vec1.y, vec2.y)), 0, height - 1);
        var maxY = Mth.clamp((int) Math.max(vec0.y, Math.max(vec1.y, vec2.y)), 0, height - 1);

        var totalArea = signedTriangleArea(vec0.x, vec0.y, vec1.x, vec1.y, vec2.x, vec2.y);
        if (totalArea > -1) return;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var alpha = signedTriangleArea(x + 0.5f, y + 0.5f, vec1.x, vec1.y, vec2.x, vec2.y) / totalArea;
                if (alpha < 0) continue;

                var beta = signedTriangleArea(x + 0.5f, y + 0.5f, vec2.x, vec2.y, vec0.x, vec0.y) / totalArea;
                if (beta < 0) continue;

                var gamma = signedTriangleArea(x + 0.5f, y + 0.5f, vec0.x, vec0.y, vec1.x, vec1.y) / totalArea;
                if (gamma < 0) continue;
                var z = -(alpha * vec0.z + beta * vec1.z + gamma * vec2.z);
                int pos = index(x, y);

                if (depth[pos] >= z) {
                    if (image != null) {
                        var u = Mth.clamp((alpha * uv0.x + beta * uv1.x + gamma * uv2.x), 0, image.width() - 1);
                        var v = Mth.clamp((alpha * uv0.y + beta * uv1.y + gamma * uv2.y), 0, image.height() - 1);

                        var clr = ARGB.multiply(image.get(u, v), color);
                        if (ARGB.alphaFloat(clr) < 0.1) {
                            continue;
                        }
                        depth[x + y * width] = z;
                        var out = ARGB.alphaBlend(this.screen[pos], clr);
                        this.screen[pos] = out;
                        //this.canvas.set(x, y, ARGB.color(u << 4, v << 4, ((u + v) & 1) * 255, ARGB.alpha(out)));
                        //this.canvas.set(x, y, color);
                    } else {
                        if (ARGB.alphaFloat(color) < 0.1) {
                            continue;
                        }

                        depth[x + y * width] = z;
                        this.screen[pos] = ARGB.alphaBlend(this.screen[pos], color);
                    }
                }
            }

        }
    }

    public final int index(int x, int y) {
        return x + y * width;
    }
}
