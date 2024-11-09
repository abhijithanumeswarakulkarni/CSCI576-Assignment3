package main;

import java.awt.image.BufferedImage;
import java.util.List;

import utils.ImageUtils;

public class Assignment {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid input");
            return;
        }

        String imagePath = args[0];
        int n = Integer.parseInt(args[1]);

        if (n != -1 && n != -2 && (n < 4096 || n > 262144)) {
            System.out.println("Invalid coefficient");
            return;
        }

        try {
            int width = 512;
            int height = 512;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ImageUtils.readImageRGB(width, height, imagePath, image);
            List<double[][][]> encodedDCTBlocks = ImageUtils.encodeDCT(image);
            List<double[][]> encodedDWTBlocks = ImageUtils.encodeDWT(image);
            if (n > 0) {
                BufferedImage decodedDCTImage = ImageUtils.decodeDCT(encodedDCTBlocks, width, height, n);
                BufferedImage decodedDWTImage = ImageUtils.decodeDWT(encodedDWTBlocks, width, height, n);
                ImageUtils.showImages(decodedDCTImage, decodedDWTImage, "DCT vs DWT Compression");
            }
            if (n == -1) {
                ImageUtils.progressiveDCT(encodedDCTBlocks, width, height);
                ImageUtils.progressiveDWT(encodedDWTBlocks, width, height);
            }
            if (n == -2) {
                ImageUtils.balancedProgressiveDCTandDWT(encodedDCTBlocks, encodedDWTBlocks, width, height);
            }
        } catch (Exception e) {
            System.out.println("Something Went Wrong!");
            e.printStackTrace();
        }
    }
}
