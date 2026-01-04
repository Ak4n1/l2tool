/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2tool.img;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * Generic texture handler for formats not specifically implemented.
 * Supports: RGB8, L8, RGBA7, RGB16, RRRGGGBBB
 */
public class GenericTexture extends Img {

    private GenericTexture() {
    }

    public static GenericTexture createFromData(byte[] data, MipMapInfo info) {
        GenericTexture tex = new GenericTexture();
        tex.setName(info.name);
        tex.setFormat(info.format);

        BufferedImage[] mipMaps = new BufferedImage[info.offsets.length];
        byte[][] ds = new byte[info.offsets.length][];

        for (int i = 0; i < info.offsets.length; i++) {
            int width = Math.max(info.width / (1 << i), 1);
            int height = Math.max(info.height / (1 << i), 1);

            ds[i] = Arrays.copyOfRange(data, info.offsets[i], info.offsets[i] + info.sizes[i]);

            BufferedImage image = null;

            switch (info.format) {
                case RGB8:
                    image = createRGB8Image(ds[i], width, height);
                    break;
                case L8:
                    image = createL8Image(ds[i], width, height);
                    break;
                case RGBA7:
                    image = createRGBA7Image(ds[i], width, height);
                    break;
                case RGB16:
                    image = createRGB16Image(ds[i], width, height);
                    break;
                case RRRGGGBBB:
                    image = createRRRGGGBBBImage(ds[i], width, height);
                    break;
                default:
                    // Fallback: create a placeholder image
                    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    break;
            }

            mipMaps[i] = image;
        }

        tex.setMipMaps(mipMaps);
        tex.setData(ds);
        return tex;
    }

    /**
     * RGB8 - 24 bits per pixel (3 bytes: R, G, B)
     */
    private static BufferedImage createRGB8Image(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int pixelCount = width * height;
        int dataSize = data.length;
        int bytesPerPixel = dataSize / pixelCount;

        // Handle case where data might be padded or different format
        if (bytesPerPixel < 3) {
            bytesPerPixel = 3;
        }

        for (int y = 0; y < height && buffer.remaining() >= 3; y++) {
            for (int x = 0; x < width && buffer.remaining() >= 3; x++) {
                int b = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int r = buffer.get() & 0xFF;
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    /**
     * L8 - 8 bits per pixel grayscale (1 byte: luminance)
     */
    private static BufferedImage createL8Image(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (index < data.length) {
                    int l = data[index] & 0xFF;
                    int rgb = (l << 16) | (l << 8) | l;
                    image.setRGB(x, y, rgb);
                }
            }
        }

        return image;
    }

    /**
     * RGBA7 - 7 bits per channel (stored as 4 bytes but with 7-bit precision)
     */
    private static BufferedImage createRGBA7Image(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int y = 0; y < height && buffer.remaining() >= 4; y++) {
            for (int x = 0; x < width && buffer.remaining() >= 4; x++) {
                int b = (buffer.get() & 0x7F) << 1;
                int g = (buffer.get() & 0x7F) << 1;
                int r = (buffer.get() & 0x7F) << 1;
                int a = (buffer.get() & 0x7F) << 1;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }

        return image;
    }

    /**
     * RGB16 - 16 bits per pixel (5-6-5 format: 5 red, 6 green, 5 blue)
     */
    private static BufferedImage createRGB16Image(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int y = 0; y < height && buffer.remaining() >= 2; y++) {
            for (int x = 0; x < width && buffer.remaining() >= 2; x++) {
                int pixel = buffer.getShort() & 0xFFFF;
                // 5-6-5 format
                int r = ((pixel >> 11) & 0x1F) << 3;
                int g = ((pixel >> 5) & 0x3F) << 2;
                int b = (pixel & 0x1F) << 3;
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    /**
     * RRRGGGBBB - Planar format where R, G, B channels are stored separately
     */
    private static BufferedImage createRRRGGGBBBImage(byte[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int planeSize = width * height;

        // Check if we have enough data for all 3 planes
        if (data.length < planeSize * 3) {
            // Not enough data, return what we can
            return image;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int r = data[index] & 0xFF;
                int g = data[planeSize + index] & 0xFF;
                int b = data[planeSize * 2 + index] & 0xFF;
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    @Override
    public void write(File file) throws IOException {
        // Write as PNG by default
        ImageIO.write(getMipMaps()[0], "png", file);
    }
}
