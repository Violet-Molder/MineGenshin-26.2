package com.linweiyun.genshin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 生成伤害飘字的垂直渐变遮罩（不透明灰度图）。
 * 输出：src/main/resources/assets/minegenshin/gui/damage_indicator_mask.png
 *
 * 顶部纯白（亮度 255）→ 底部纯黑（亮度 0），alpha 恒为 255。
 */
public class MaskGenerator {

    public static void main(String[] args) throws IOException {
        File out = new File("src/main/resources/assets/minegenshin/gui/damage_indicator_mask1.png");
        out.getParentFile().mkdirs();

        int width  = 1;
        int height = 64;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            float t = y / (float) (height - 1); // 0 = 顶部, 1 = 底部
            int gray = Math.round(255 * (1.0f - t)); // 白 → 黑
            int argb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
            img.setRGB(0, y, argb);
        }

        ImageIO.write(img, "PNG", out);
        System.out.println("Mask written to: " + out.getAbsolutePath());
    }
}
