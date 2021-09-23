package org.ofdrw.converter;

import org.ofdrw.converter.utils.CommonUtil;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.PageInfo;
import org.ofdrw.reader.tools.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 图片转换类
 *
 * @author qaqtutu
 * @since 2021-03-13 10:00:01
 */
public class ImageMaker extends AWTMaker {


    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     * <p>
     * 如果需要更加精确的表示请使用{@link #ImageMaker(OFDReader, double)}
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)
     */
    public ImageMaker(OFDReader reader, int ppm) {
        super(reader, ppm);
    }

    /**
     * 创建图片转换对象实例
     * <p>
     * OFD内部使用毫米作为基本单位
     *
     * @param reader OFD解析器
     * @param ppm    每毫米像素数量(Pixels per millimeter)，DPI与PPM转换可以使用{@link CommonUtil#dpiToPpm(int)}。
     * @author iandjava
     */
    public ImageMaker(OFDReader reader, double ppm) {
        super(reader, ppm);
    }

    /**
     * 渲染OFD页面为图片
     *
     * @param pageIndex 页码，从0起
     * @return 渲染完成的图片
     */
    public BufferedImage makePage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        PageInfo pageInfo = pages.get(pageIndex);
        ST_Box pageBox = pageInfo.getSize();

        // PPM 转 像素
        int pageWidthPixel = (int) Math.round(ppm * pageBox.getWidth());
        int pageHeightPixel = (int) Math.round(ppm * pageBox.getHeight());

        BufferedImage image = createImage(pageWidthPixel, pageHeightPixel);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        writePage(graphics, pageInfo, null);

        return image;
    }

    /**
     * 创建图片
     *
     * @param pageWidthPixel  图形宽度
     * @param pageHeightPixel 图像高度
     */
    private BufferedImage createImage(int pageWidthPixel, int pageHeightPixel) {
        return ImageUtils.createImage(pageWidthPixel, pageHeightPixel, isStamp);
    }

    /**
     * 渲染OFD页面为图片字节数组
     *
     * @param pageIndex 页码，从0起
     * @param type      图片类型，jpg,png,...
     * @return 渲染完成的图片字节数组
     */
    public byte[] makePage(int pageIndex, String type) throws IOException {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new GeneralConvertException(String.format("%s 不是有效索引", pageIndex));
        }
        PageInfo pageInfo = pages.get(pageIndex);
        ST_Box pageBox = pageInfo.getSize();

        // PPM 转 像素
        int pageWidthPixel = (int) Math.round(ppm * pageBox.getWidth());
        int pageHeightPixel = (int) Math.round(ppm * pageBox.getHeight());

        BufferedImage image = createImage(pageWidthPixel, pageHeightPixel);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        //消除文字锯齿
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //消除画图锯
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        writePage(graphics, pageInfo, null);

        return ImageUtils.toBytes(image, type);
    }

    /**
     * 渲染OFD页面为图片压缩包字节数组
     *
     * @param convertExt 图片转换后缀
     * @return
     * @throws IOException
     */
    public byte[] makePageZip(String convertExt) throws IOException {
        try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
            String dirPath = "dzzzlyd" + System.currentTimeMillis();
            try (//生成ZipOutputStream，会把压缩的内容全都通过这个输出流输出
                 ZipOutputStream zipOut = new ZipOutputStream(bao)) {
                //设置压缩的注释
                zipOut.setComment("comment");
                //启用压缩
                zipOut.setMethod(ZipOutputStream.DEFLATED);
                //压缩级别为最强压缩，但时间要花得多一点
                zipOut.setLevel(Deflater.BEST_COMPRESSION);
                //只是放入了空目录的名字
                zipOut.putNextEntry(new ZipEntry(dirPath + File.separator));
                zipOut.flush();
                for (int i = 0; i < this.pageSize(); i++) {
                    byte[] imageBytes = this.makePage(i, convertExt);
                    //放入一个ZipEntry
                    zipOut.putNextEntry(new ZipEntry(dirPath + File.separator + i + "." + convertExt));
                    zipOut.write(imageBytes);
                    zipOut.flush();
                }
            }
            byte[] zipBytes = bao.toByteArray();
            return zipBytes;
        }
    }
}
