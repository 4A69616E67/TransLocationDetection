package File.MatrixFile;

import File.AbstractItem;
import TLD.Tools;
import Unit.ChrRegion;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import sun.font.FontDesignMetrics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by 浩 on 2019/2/1.
 */

public class MatrixItem extends AbstractItem {
    public Array2DRowRealMatrix item;
    public ChrRegion Chr1;
    public ChrRegion Chr2;
    private int Fold;
    private double MinValue;
    private double MaxValue;
    private double ThresholdValue;
    private int Marginal = 160;
    public boolean Legend = true;

    public MatrixItem(ChrRegion chr1, ChrRegion chr2, double[][] matrix) {
        this(matrix);
        Chr1 = chr1;
        Chr2 = chr2;
    }

    public MatrixItem(int rowDimension, int columnDimension) throws NotStrictlyPositiveException {
        item = new Array2DRowRealMatrix(rowDimension, columnDimension);
    }

    public MatrixItem(double[][] matrix) {
        item = new Array2DRowRealMatrix(matrix);
    }

    public static class MatrixComparator implements Comparator<MatrixItem> {

        @Override
        public int compare(MatrixItem o1, MatrixItem o2) {
            return 0;
        }
    }

    public BufferedImage DrawMatrix(float threshold) {
        int MatrixHeight = item.getRowDimension();
        int MatrixWidth = item.getColumnDimension();
        ArrayList<Double> list = new ArrayList<>();
        for (int i = 0; i < MatrixHeight; i++) {
            for (int j = 0; j < MatrixWidth; j++) {
                list.add(item.getEntry(i, j));
            }
        }
        Collections.sort(list);
        MinValue = list.get(0);
        MaxValue = list.get(list.size() - 1);
        ThresholdValue = list.get((int) ((list.size() - 1) * threshold));
        //generate heatmap
        BufferedImage matrix_image = new BufferedImage(MatrixWidth, MatrixHeight, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < MatrixHeight; i++) {
            for (int j = 0; j < MatrixWidth; j++) {
                double value = item.getEntry(i, j);
                Color c;
                if (value >= MinValue && value <= MaxValue) {
                    value = Math.min(value, ThresholdValue);
                    double percent = 1 - value / (ThresholdValue - MinValue);
                    c = new Color(255, (int) (255 * percent), (int) (255 * percent));
                } else {
                    c = new Color(255, 255, 255, 0);
                }
                matrix_image.setRGB(j, MatrixHeight - 1 - i, c.getRGB());
            }
        }
        return matrix_image;
    }

    //    public static BufferedImage PlotHeatMap(String Chr1, int StartSite1, String Chr2, int StartSite2, RealMatrix Matrix, int Resolution, float threshold) throws IOException {
//
////        ImageIO.write(image, OutFile.getName().substring(OutFile.getName().lastIndexOf('.') + 1), OutFile);
//    }
    public BufferedImage DrawHeatMap(int resolution, float threshold) {
        return DrawHeatMap(Chr1.Chr, Chr1.region.Start, Chr2.Chr, Chr2.region.Start, resolution, threshold);
    }

    public BufferedImage DrawHeatMap(String Chr1, int StartSite1, String Chr2, int StartSite2, int resolution, float threshold) {
        int MatrixHeight = item.getRowDimension();
        int MatrixWidth = item.getColumnDimension();
        int StandardImageSize = 2000;
//        int Marginal = 160;
        int LegendWidth = 20;
        int interval = 200;
        int extend_len = 15;
//        Double MinValue = null, MaxValue = null;
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(-Math.PI / 2, 0, 0);
        Font t;
        BufferedImage matrix_image = DrawMatrix(threshold);
        //zoom on if the original graphic size is too small
        Fold = Math.max(StandardImageSize / MatrixHeight, StandardImageSize / MatrixWidth);
        Fold = Math.max(Fold, 1);
        //calculate new figure size
        MatrixHeight = MatrixHeight * Fold;
        MatrixWidth = MatrixWidth * Fold;
        resolution = resolution / Fold;//correct resolution,
        BufferedImage image = new BufferedImage(MatrixWidth + Marginal * 2, MatrixHeight + Marginal * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        //set transparent background
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.drawImage(matrix_image, Marginal, Marginal, MatrixWidth, MatrixHeight, null);
        //draw x y label
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        graphics.drawLine(Marginal, Marginal + MatrixHeight, Marginal + MatrixWidth, Marginal + MatrixHeight);//draw x label line
        graphics.drawLine(Marginal, Marginal + MatrixHeight, Marginal, Marginal);//draw y label line
        t = new Font("Times New Roman", Font.PLAIN, 30);
        //draw sub x interval
        for (int i = 0; i <= MatrixWidth * 10 / interval; i++) {
            graphics.drawLine(Marginal + i * interval / 10, Marginal + MatrixHeight, Marginal + i * interval / 10, Marginal + MatrixHeight + extend_len / 2);
        }
        //draw x interval
        for (int i = 0; i <= MatrixWidth / interval; i++) {
            graphics.drawLine(Marginal + i * interval, Marginal + MatrixHeight, Marginal + i * interval, Marginal + MatrixHeight + extend_len);
            float value = (float) (StartSite2 + i * interval * resolution) / 1000000;
            String value_str;
            if (value == (int) value) {
                value_str = String.format("%d", (int) (value));
            } else {
                value_str = String.format("%.2f", value);
            }
            int h = FontDesignMetrics.getMetrics(t).getHeight();
            Tools.DrawStringCenter(graphics, value_str, t, Marginal + i * interval, Marginal + MatrixHeight + extend_len + h / 2 + 2, 0);
        }
        //draw sub y interval
        for (int i = 0; i <= MatrixHeight * 10 / interval; i++) {
            int y1 = Marginal + MatrixHeight - i * interval / 10;
            graphics.drawLine(Marginal, y1, Marginal - extend_len / 2, y1);
        }
        //draw y interval
        for (int i = 0; i <= MatrixHeight / interval; i++) {
            graphics.drawLine(Marginal, Marginal + MatrixHeight - i * interval, Marginal - extend_len, Marginal + MatrixHeight - i * interval);
            float value = (float) (StartSite1 + i * interval * resolution) / 1000000;
            String value_str;
            if (value == (int) value) {
                value_str = String.format("%d", (int) (value));
            } else {
                value_str = String.format("%.2f", value);
            }
            int h = FontDesignMetrics.getMetrics(t).getHeight();
            Tools.DrawStringCenter(graphics, value_str, t, Marginal - extend_len - h / 2 - 2, Marginal + MatrixHeight - i * interval, -Math.PI / 2);
        }
//        graphics.setStroke(new BasicStroke(3.0f));
        //draw legend
        if (Legend) {
            interval = 40;
            graphics.setPaint(new GradientPaint(Marginal + MatrixWidth + interval, Marginal + MatrixHeight, Color.WHITE, Marginal + MatrixWidth + interval, Marginal, Color.RED));
            graphics.fillRect(Marginal + MatrixWidth + interval, Marginal, LegendWidth, MatrixHeight);
            graphics.setColor(Color.BLACK);
            for (int i = 0; i <= 10; i++) {
                graphics.drawLine(Marginal + MatrixWidth + interval + LegendWidth, Math.round(Marginal + MatrixHeight - (float) (i) / 10 * MatrixHeight), Marginal + MatrixWidth + interval + LegendWidth + extend_len, Math.round(Marginal + MatrixHeight - (float) (i) / 10 * MatrixHeight));
                String value_str = String.format("%.1f", MinValue + (ThresholdValue - MinValue) * (float) (i) / 10);
                Tools.DrawStringCenter(graphics, value_str, t, Marginal + MatrixWidth + interval + LegendWidth + extend_len + 2 + FontDesignMetrics.getMetrics(t).stringWidth(value_str) / 2, Math.round(Marginal + MatrixHeight - (float) (i) / 10 * MatrixHeight), 0);
            }
        }
        //draw x,y title
        t = new Font("Times New Roman", Font.BOLD, 80);
        Tools.DrawStringCenter(graphics, Chr1, t, FontDesignMetrics.getMetrics(t).getHeight() / 2 + 5, Marginal + MatrixHeight / 2, -Math.PI / 2);//draw y title,rotation pi/2 anticlockwise
        Tools.DrawStringCenter(graphics, Chr2, t, Marginal + MatrixWidth / 2, 2 * Marginal + MatrixHeight - 5 - FontDesignMetrics.getMetrics(t).getHeight() / 2, 0);//draw x title
        return image;
//        return PlotHeatMap(Chr1, StartSite1, Chr2, StartSite2, item, resolution, threshold);
    }

    public int getFold() {
        return Fold;
    }

    public int getMarginal() {
        return Marginal;
    }

    //    @Override
//    public int compareTo(MatrixItem o) {
//        return 0;
//    }
}
