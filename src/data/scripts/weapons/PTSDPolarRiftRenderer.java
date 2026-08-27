package data.scripts.weapons;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

/** Reusable polar-coordinate rift mesh: r(theta,t)=base*star(theta)*noise(theta,t). */
public final class PTSDPolarRiftRenderer {
    private PTSDPolarRiftRenderer() {}

    public static final class Shape {
        public final int segments;
        public final float[] noise;
        public final float phase;
        public final int spikeCount;
        public final float spikeStrength;
        public final float noiseStrength;

        public Shape(long seed, int segments, int spikeCount, float spikeStrength) {
            this(seed, segments, spikeCount, spikeStrength, 0.18f);
        }

        public Shape(long seed, int segments, int spikeCount, float spikeStrength, float noiseStrength) {
            this.segments = Math.max(16, segments);
            this.spikeCount = Math.max(2, spikeCount);
            this.spikeStrength = clamp(spikeStrength, 0f, 0.8f);
            this.noiseStrength = clamp(noiseStrength, 0f, 0.65f);
            noise = new float[this.segments];
            Random random = new Random(seed);
            phase = random.nextFloat() * 6.2831855f;
            for (int i = 0; i < this.segments; i++) noise[i] = random.nextFloat() * 2f - 1f;
            for (int pass = 0; pass < 2; pass++) {
                float[] copy = noise.clone();
                for (int i = 0; i < this.segments; i++) {
                    float prev = copy[(i + this.segments - 1) % this.segments];
                    float next = copy[(i + 1) % this.segments];
                    noise[i] = copy[i] * 0.58f + (prev + next) * 0.21f;
                }
            }
        }
    }

    public static void render(Shape shape, Vector2f location, float facing, float baseRadius,
                              float stretch, float elapsed, float alpha, Color fill, Color edge) {
        render(shape, location, facing, baseRadius, stretch, elapsed, alpha, fill, edge, 1f);
    }

    /** shapeAmount=0 is a perfect circle; shapeAmount=1 is the complete polar rift. */
    public static void render(Shape shape, Vector2f location, float facing, float baseRadius,
                              float stretch, float elapsed, float alpha, Color fill, Color edge,
                              float shapeAmount) {
        if (shape == null || location == null || alpha <= 0f || baseRadius <= 0f) return;
        stretch = clamp(stretch, 0f, 1f);
        shapeAmount = clamp(shapeAmount, 0f, 1f);
        float lengthScale = 1f + stretch * 2.15f;
        float widthScale = 1f - stretch * 0.48f;

        GL11.glPushMatrix();
        GL11.glTranslatef(location.x, location.y, 0f);
        GL11.glRotatef(facing, 0f, 0f, 1f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        color(fill, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(0f, 0f);
        for (int i = 0; i <= shape.segments; i++) {
            vertex(shape, i % shape.segments, baseRadius, lengthScale, widthScale,
                    elapsed, 1f, shapeAmount);
        }
        GL11.glEnd();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glLineWidth(1.35f);
        color(edge, alpha * 0.9f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < shape.segments; i++) {
            vertex(shape, i, baseRadius, lengthScale, widthScale, elapsed, 1.035f, shapeAmount);
        }
        GL11.glEnd();

        GL11.glLineWidth(1f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void vertex(Shape shape, int i, float baseRadius, float lengthScale,
                               float widthScale, float elapsed, float outerMult, float shapeAmount) {
        float theta = 6.2831855f * i / shape.segments;
        float starWave = Math.abs((float)Math.cos(theta * shape.spikeCount * 0.5f));
        float star = 1f - shape.spikeStrength
                + shape.spikeStrength * (float)Math.pow(starWave, 5.5);
        float harmonics = 1f
                + 0.075f * (float)Math.sin(theta * 7f + shape.phase + elapsed * 1.3f)
                + 0.045f * (float)Math.cos(theta * 11f - shape.phase * 0.7f - elapsed * 0.8f);
        float temporalNoise = 1f + shape.noise[i]
                * (shape.noiseStrength + 0.025f * (float)Math.sin(elapsed * 3f + i));
        float complete = star * harmonics * temporalNoise;
        float radius = baseRadius * (1f + (complete - 1f) * shapeAmount) * outerMult;
        GL11.glVertex2f((float)Math.cos(theta) * radius * lengthScale,
                (float)Math.sin(theta) * radius * widthScale);
    }

    private static void color(Color color, float alpha) {
        GL11.glColor4ub((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(),
                (byte)(color.getAlpha() * clamp(alpha, 0f, 1f)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}