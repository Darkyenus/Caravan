package caravan.util;

import com.badlogic.gdx.math.FloatCounter;
import com.badlogic.gdx.math.MathUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility for sampling simple and fast smoothed value noise.
 */
public final class ValueNoise {

    /** See RandomXS128.nextFloat */
    private static final double NORM_FLOAT = 1.0 / (1L << 24);

    private static float i(float f, float t, float a){
        final float a2 = a*a;
        return MathUtils.lerp(f,t,3*a2-2*a2*a);
    }

    private static long murmurHash3(long x) {
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    /** Return a float from uniform distribution of (-1, 1) for the given point. */
    private static float samplePoint(long seed, int x, int y) {
        final long base = ((long) x << 32) | (y & 0xFFFFFFFFL);

        // Modified stateless version of RandomXS128.nextFloat to give random sign as well
        final long seed0 = murmurHash3(murmurHash3(base) ^ seed);
        final long seed1 = murmurHash3(seed0);
        final long s = seed0 ^ (seed0 << 23);
        final long l = (s ^ seed1 ^ (s >>> 17) ^ (seed1 >>> 26)) + seed1;

        int floatBits = Float.floatToRawIntBits((float)((l >>> 40) * NORM_FLOAT));
        floatBits |= (l << 31); // Take LSB of l and OR it into the sign bit
        return Float.intBitsToFloat(floatBits);
    }

    /**
     * Sample the noise.
     * @param x of the sample
     * @param y of the sample
     * @param invOctavePower reciprocal of the octave. Greater octave means that the noise varies less.
     *                      Octave of 1 exactly corresponds to one sample point per coordinate unit.
     * @param magnitude determines the range of the sample
     * @return value in (-magnitude, magnitude) range
     */
    public static float sample(long seed, float x, float y, float invOctavePower, float magnitude){
        x *= invOctavePower;
        y *= invOctavePower;

        int firstX = (int) x;
        int firstY = (int) y;

        float xA = x % 1;
        if (xA < 0f) {
            xA = 1f + xA;
            firstX--;
        }
        float yA = y % 1;
        if (yA < 0f) {
            yA = 1f + yA;
            firstY--;
        }

        final int secondX = firstX + 1;
        final int secondY = firstY + 1;

        final float c00 = samplePoint(seed, firstX,firstY);
        final float c10 = samplePoint(seed, secondX, firstY);
        final float c11 = samplePoint(seed, secondX, secondY);
        final float c01 = samplePoint(seed, firstX, secondY);

        return i(i(c00, c10, xA), i(c01, c11, xA), yA) * magnitude;
    }

    public static float sample(long seed, float x, float y){
        return (1f + sample(seed, x,y,1f/256,1f/2f) + sample(seed, x,y,1f/128,1f/4f) + sample(seed, x,y,1f/64,1f/8f) + sample(seed, x,y,1f/32,1f/16f))/2f;
    }

    public static float sampleDense(long seed, float x, float y){
        return (1f + sample(seed, x,y,1f/64,1f/2f) + sample(seed, x,y,1f/32,1f/4f) + sample(seed, x,y,1f/16,1f/8f) + sample(seed, x,y,1f/8,1f/16f))/2f;
    }

    public static float sampleVeryDense(long seed, float x, float y){
        return (1f + sample(seed, x,y,1f/32,1f/2f) + sample(seed, x,y,1f/16,1f/4f) + sample(seed, x,y,1f/8,1f/8f) + sample(seed, x,y,1f/4,1f/16f))/2f;
    }

    public static void main(String[] args) throws IOException {
        final long seed = System.currentTimeMillis();
        int size = 1024;
        FloatCounter counter = new FloatCounter(0);
        BufferedImage image = new BufferedImage(size,size, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = image.createGraphics();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float value = sample(seed, x, y, 1f / 50f, 1f) * 0.5f + 0.5f;
                value = MathUtils.clamp(value, 0f, 1f);
                counter.put(value);
                g.setColor(new java.awt.Color(value, value, value));
                g.fillRect(x, y, 1, 1);
            }
        }
        ImageIO.write(image, "png", new File("Noise.png"));
    }
}
