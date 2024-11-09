package utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class ImageUtils {

    private static final int BLOCK_SIZE = 8;
    private static final int SIZE = 512;

    public static void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;
            File file = new File(imgPath);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(0);

                byte[] bytes = new byte[frameLength];
                raf.read(bytes);

                int ind = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        byte r = bytes[ind];
                        byte g = bytes[ind + height * width];
                        byte b = bytes[ind + height * width * 2];

                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        img.setRGB(x, y, pix);
                        ind++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Image couldn't be read.");
            e.printStackTrace();
        }
    }

    public static void showImages(BufferedImage image1, BufferedImage image2, String title) {
        JFrame resultImageFrame = new JFrame(title);
        resultImageFrame.setLayout(new GridBagLayout());
        JLabel label1 = new JLabel("DCT", JLabel.CENTER);
        JLabel label2 = new JLabel("DWT", JLabel.CENTER);
        JLabel imageLabel1 = new JLabel(new ImageIcon(image1));
        JLabel imageLabel2 = new JLabel(new ImageIcon(image2));
    
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0;
        c.gridy = 0;
        resultImageFrame.add(imageLabel1, c);
        c.gridy = 1;
        resultImageFrame.add(label1, c);
        c.gridx = 1;
        c.gridy = 0;
        resultImageFrame.add(imageLabel2, c);
        c.gridy = 1;
        resultImageFrame.add(label2, c);
        resultImageFrame.pack();
        resultImageFrame.setVisible(true);
        resultImageFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    // DCT Encoding
    public static List<double[][][]> encodeDCT(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        List<double[][][]> dctBlocks = new ArrayList<>();

        for (int channel = 0; channel < 3; channel++) {
            double[][][] channelBlocks = new double[width / BLOCK_SIZE][height / BLOCK_SIZE][BLOCK_SIZE * BLOCK_SIZE];
            for (int i = 0; i < width; i += BLOCK_SIZE) {
                for (int j = 0; j < height; j += BLOCK_SIZE) {
                    double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];
                    // Extract 8x8 block
                    for (int x = 0; x < BLOCK_SIZE; x++) {
                        for (int y = 0; y < BLOCK_SIZE; y++) {
                            int pixel = inputImage.getRGB(i + x, j + y);
                            int color = (channel == 0) ? (pixel >> 16) & 0xFF
                                    : (channel == 1) ? (pixel >> 8) & 0xFF : pixel & 0xFF;
                            block[x][y] = color;
                        }
                    }
                    // Apply DCT
                    double[][] dctBlock = dctTransform(block);
                    for (int x = 0; x < BLOCK_SIZE; x++) {
                        for (int y = 0; y < BLOCK_SIZE; y++) {
                            channelBlocks[i / BLOCK_SIZE][j / BLOCK_SIZE][x * BLOCK_SIZE + y] = dctBlock[x][y];
                        }
                    }
                }
            }
            dctBlocks.add(channelBlocks);
        }
        return dctBlocks;
    }

    // DCT Decoding
    public static BufferedImage decodeDCT(List<double[][][]> dctBlocks, int width, int height, int n) {
        int m = Math.round(n / 4096.0f);
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int channel = 0; channel < 3; channel++) {
            double[][][] channelBlocks = dctBlocks.get(channel);
            for (int i = 0; i < width; i += BLOCK_SIZE) {
                for (int j = 0; j < height; j += BLOCK_SIZE) {
                    double[][] dctBlock = new double[BLOCK_SIZE][BLOCK_SIZE];
                    for (int x = 0; x < BLOCK_SIZE; x++) {
                        for (int y = 0; y < BLOCK_SIZE; y++) {
                            dctBlock[x][y] = channelBlocks[i / BLOCK_SIZE][j / BLOCK_SIZE][x * BLOCK_SIZE + y];
                        }
                    }
                    applyZigzagThreshold(dctBlock, m);
                    double[][] decodedBlock = inverseDCTTransform(dctBlock);
                    for (int x = 0; x < BLOCK_SIZE; x++) {
                        for (int y = 0; y < BLOCK_SIZE; y++) {
                            int pixel = outputImage.getRGB(i + x, j + y);
                            int color = Math.min(255, Math.max(0, (int) Math.round(decodedBlock[x][y])));
                            if (channel == 0) {
                                pixel = (pixel & 0xFF00FFFF) | (color << 16);
                            } else if (channel == 1) {
                                pixel = (pixel & 0xFFFF00FF) | (color << 8);
                            } else {
                                pixel = (pixel & 0xFFFFFF00) | color;
                            }
                            outputImage.setRGB(i + x, j + y, pixel);
                        }
                    }
                }
            }
        }
        return outputImage;
    }

    private static void applyZigzagThreshold(double[][] dctBlock, int m) {
        int[][] zigzagOrder = getZigzagOrder();
        for (int i = m; i < BLOCK_SIZE * BLOCK_SIZE; i++) {
            int x = zigzagOrder[i][0];
            int y = zigzagOrder[i][1];
            dctBlock[x][y] = 0;
        }
    }

    // Zigzag Traversal
    private static int[][] getZigzagOrder() {
        int[][] order = new int[BLOCK_SIZE * BLOCK_SIZE][2];
        int i = 0;
        for (int s = 0; s < BLOCK_SIZE * 2 - 1; s++) {
            int z = (s < BLOCK_SIZE) ? 0 : s - BLOCK_SIZE + 1;
            for (int j = z; j <= s - z; j++) {
                order[i][0] = (s % 2 == 0) ? j : s - j;
                order[i][1] = (s % 2 == 0) ? s - j : j;
                i++;
            }
        }
        return order;
    }

    // DCT Transformation
    private static double[][] dctTransform(double[][] block) {
        int N = BLOCK_SIZE;
        double[][] dct = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int x = 0; x < N; x++) {
                    for (int y = 0; y < N; y++) {
                        sum += block[x][y] *
                                Math.cos((2 * x + 1) * u * Math.PI / (2 * N)) *
                                Math.cos((2 * y + 1) * v * Math.PI / (2 * N));
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1.0;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1.0;
                dct[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return dct;
    }

    // Inverse DCT Transformation
    private static double[][] inverseDCTTransform(double[][] dctBlock) {
        int N = BLOCK_SIZE;
        double[][] block = new double[N][N];
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                double sum = 0.0;
                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {
                        double cu = (u == 0) ? 1 / Math.sqrt(2) : 1.0;
                        double cv = (v == 0) ? 1 / Math.sqrt(2) : 1.0;
                        sum += cu * cv * dctBlock[u][v] *
                                Math.cos((2 * x + 1) * u * Math.PI / (2 * N)) *
                                Math.cos((2 * y + 1) * v * Math.PI / (2 * N));
                    }
                }
                block[x][y] = 0.25 * sum;
            }
        }
        return block;
    }

    // DWT Encoding
    public static List<double[][]> encodeDWT(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        List<double[][]> dwtChannels = new ArrayList<>();
        for (int channel = 0; channel < 3; channel++) {
            double[][] channelData = new double[height][width];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int pixel = inputImage.getRGB(i, j);
                    int color = (channel == 0) ? (pixel >> 16) & 0xFF :
                              (channel == 1) ? (pixel >> 8) & 0xFF : pixel & 0xFF;
                    channelData[j][i] = color;
                }
            }
            forwardDWT(channelData);
            dwtChannels.add(channelData);
        }
        return dwtChannels;
    }

    // DWT Decoding
    public static BufferedImage decodeDWT(List<double[][]> dwtChannels, int width, int height, int n) {
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int channel = 0; channel < 3; channel++) {
            double[][] channelData = dwtChannels.get(channel);
            double[][] coefficients = new double[height][width];
            for (int i = 0; i < height; i++) {
                System.arraycopy(channelData[i], 0, coefficients[i], 0, width);
            }
            if (n < width * height) {
                applyWaveletThreshold(coefficients, n);
            }
            inverseDWTTransform(coefficients);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int pixel = outputImage.getRGB(i, j);
                    int color = Math.min(255, Math.max(0, (int) Math.round(coefficients[j][i])));
                    if (channel == 0) {
                        pixel = (pixel & 0xFF00FFFF) | (color << 16);
                    } else if (channel == 1) {
                        pixel = (pixel & 0xFFFF00FF) | (color << 8);
                    } else {
                        pixel = (pixel & 0xFFFFFF00) | color;
                    }
                    outputImage.setRGB(i, j, pixel);
                }
            }
        }
        return outputImage;
    }

    private static void forwardDWT(double[][] data) {
        int rows = data.length;
        int cols = data[0].length;
        while (rows >= 2 && cols >= 2) {
            for (int i = 0; i < rows; i++) {
                double[] row = new double[cols];
                System.arraycopy(data[i], 0, row, 0, cols);
                transformRow(row, cols);
                System.arraycopy(row, 0, data[i], 0, cols);
            }
            for (int j = 0; j < cols; j++) {
                double[] col = new double[rows];
                for (int i = 0; i < rows; i++) {
                    col[i] = data[i][j];
                }
                transformRow(col, rows);
                for (int i = 0; i < rows; i++) {
                    data[i][j] = col[i];
                }
            }
            rows /= 2;
            cols /= 2;
        }
    }

    // Inverse DWT
    private static void inverseDWTTransform(double[][] data) {
        int rows = 2;
        int cols = 2;
        while (rows <= data.length && cols <= data[0].length) {
            for (int j = 0; j < cols; j++) {
                double[] col = new double[rows];
                for (int i = 0; i < rows; i++) {
                    col[i] = data[i][j];
                }
                inverseTransformRow(col, rows);
                for (int i = 0; i < rows; i++) {
                    data[i][j] = col[i];
                }
            }
            for (int i = 0; i < rows; i++) {
                double[] row = new double[cols];
                System.arraycopy(data[i], 0, row, 0, cols);
                inverseTransformRow(row, cols);
                System.arraycopy(row, 0, data[i], 0, cols);
            }
            rows *= 2;
            cols *= 2;
        }
    }

    private static void transformRow(double[] data, int length) {
        double[] temp = new double[length];
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            int idx = i * 2;
            temp[i] = (data[idx] + data[idx + 1]) * 0.7071067811865476;
            temp[half + i] = (data[idx] - data[idx + 1]) * 0.7071067811865476;
        }
        System.arraycopy(temp, 0, data, 0, length);
    }

    private static void inverseTransformRow(double[] data, int length) {
        double[] temp = new double[length];
        int half = length / 2;
        for (int i = 0; i < half; i++) {
            double avg = data[i] * 0.7071067811865476;
            double diff = data[half + i] * 0.7071067811865476;
            temp[i * 2] = avg + diff;
            temp[i * 2 + 1] = avg - diff;
        }
        System.arraycopy(temp, 0, data, 0, length);
    }

    private static void applyWaveletThreshold(double[][] data, int n) {
        int size = (int) Math.sqrt(n);
        int levels = (int) (Math.log(512 / size) / Math.log(2));
        int currentSize = 512;
        for (int level = 0; level < levels; level++) {
            currentSize /= 2;
            for (int i = 0; i < currentSize; i++) {
                for (int j = currentSize; j < currentSize * 2; j++) {
                    data[i][j] = 0;
                }
            }
            for (int i = currentSize; i < currentSize * 2; i++) {
                for (int j = 0; j < currentSize * 2; j++) {
                    data[i][j] = 0;
                }
            }
        }
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                if (i >= currentSize || j >= currentSize) {
                    data[i][j] = 0;
                }
            }
        }
    }

    // Part A
    // Progressive DCT
    public static void progressiveDCT(List<double[][][]> dctBlocks, int width, int height) {
        JFrame frame = createDisplayFrame("DCT Progressive Decoding");
        JLabel label = new JLabel();
        frame.add(label);
        frame.setVisible(true);

        for (int m = 1; m <= 64; m++) {
            System.out.println("DCT Decoding Iteration: " + m);
            BufferedImage decodedImage = decodeDCT(dctBlocks, width, height, m * 4096);
            label.setIcon(new ImageIcon(decodedImage));
            frame.repaint();
            sleep(200);
        }
    }

    // Progressive DWT
    public static void progressiveDWT(List<double[][]> dwtBlocks, int width, int height) {
        JFrame frame = createDisplayFrame("DWT Progressive Decoding");
        JLabel label = new JLabel();
        frame.add(label);
        frame.setVisible(true);

        for (int k = 1; k <= 10; k++) {
            System.out.println("DWT Decoding Iteration: " + k);
            int n = (int) Math.pow(4, k-1);
            BufferedImage decodedImage = decodeDWT(dwtBlocks, width, height, n);
            label.setIcon(new ImageIcon(decodedImage));
            frame.repaint();
            sleep(200);
        }
    }

    // Part B
    // Balanced Progressive DCT and DWT
    public static void balancedProgressiveDCTandDWT(List<double[][][]> dctBlocks, List<double[][]> dwtBlocks, int width, int height) {
        JFrame frame = createDisplayFrame("Balanced DCT and DWT Progressive Decoding");
        JLabel label = new JLabel();
        frame.add(label);
        frame.setVisible(true);

        for (int iter = 1; iter <= 64; iter++) {
            System.out.println("Balanced DCT and DWT Iteration: " + iter);
            BufferedImage dctImage = decodeDCT(dctBlocks, width, height, iter * 4096);
            BufferedImage dwtImage = decodeDWT(dwtBlocks, width, height, iter * 4096 / 64);
            label.setIcon(new ImageIcon(combineImages(dctImage, dwtImage)));
            frame.repaint();
            sleep(200);
        }
    }

    private static JFrame createDisplayFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setLayout(new GridBagLayout());
        frame.setSize(SIZE, SIZE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static BufferedImage combineImages(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth() + img2.getWidth();
        int height = Math.max(img1.getHeight(), img2.getHeight());
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        combined.getGraphics().drawImage(img1, 0, 0, null);
        combined.getGraphics().drawImage(img2, img1.getWidth(), 0, null);
        return combined;
    }
}
