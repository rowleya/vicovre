package com.googlecode.vicovre.media.screencapture;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.ComponentColorModel;
import java.awt.image.WritableRaster;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Effect;
import javax.media.Format;
import javax.media.Renderer;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.utils.QuickArray;
import com.googlecode.vicovre.codecs.utils.QuickArrayAbstract;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;
import com.googlecode.vicovre.codecs.utils.QuickArrayWrapper;
import com.googlecode.vicovre.media.Bits;
import com.googlecode.vicovre.media.processor.SimpleProcessor;

/**
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ChangeDetection implements Renderer, Effect {

    // The CCITT RGB to YUV Y Blue multiplier
    private static final double CCITT_Y_BLUE_MULTIPLIER = 0.114;

    // The CCITT RGB to YUV Y Green multiplier
    private static final double CCITT_Y_GREEN_MULTIPLIER = 0.587;

    // The CCITT RGB to YUV Y Red multiplier
    private static final double CCITT_Y_RED_MULTIPLIER = 0.299;

    // The CCITT RGB to YUV Y Constant
    private static final int CCITT_Y_CONSTANT = 16;

    // The mask to convert a byte to an int
    private static final int BYTE_TO_INT_MASK = 0xFF;

    // The name of the effect
    private static final String NAME = "Change Detection Effect";

    // The input/output format
    private RGBFormat format = null;

    // The allowed input formats
    private Format[] inputFormats;

    // The last buffer stored
    private Buffer lastBuffer = null;

    // The data type of the input
    private Class<?> dataType = null;

    // The pixel stride of the input
    private int pixelStride = 0;

    // The line stride of the input
    private int lineStride = 0;

    // The red mask of the input
    private int redMask = 0;

    // The blue mask of the input
    private int blueMask = 0;

    // The green mask of the input
    private int greenMask = 0;

    // The red shift of the input
    private int redShift = 0;

    // The blue shift of the input
    private int blueShift = 0;

    // The green shift of the input
    private int greenShift = 0;

    // The yuv format
    private YUVFormat yuvFormat = null;

    // The lock status
    private boolean locked = false;

    private int[] crvec = null;

    private QuickArray refbuf = null;

    private QuickArrayAbstract devbuf = null;

    private int scan = 0;

    private int blkw = 0;

    private int blkh = 0;

    private int nblk = 0;

    private int width = 0;

    private int threshold = 400;

    private boolean firstScene = true;

    private double firstSceneChangeThreshold = 0.90;

    private double sceneChangeThreshold = 0.05;

    private long lastUpdateTime = -1;

    private SimpleProcessor toRGB = null;

    private Dimension size = null;

    private boolean immediatelyNotifyChange = false;

    // The listeners to screen change events
    private Vector<CaptureChangeListener> screenListeners =
        new Vector<CaptureChangeListener>();

    /**
     * Creates a new ChangeDetectionEffect
     *
     */
    public ChangeDetection() {
        inputFormats = new Format[] {
            new YUVFormat(),
            new RGBFormat(null, Format.NOT_SPECIFIED, Format.byteArray,
                    Format.NOT_SPECIFIED, 24, Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    3, Format.NOT_SPECIFIED, Format.FALSE,
                    Format.NOT_SPECIFIED),
            new RGBFormat(null, Format.NOT_SPECIFIED, Format.intArray,
                    Format.NOT_SPECIFIED, 32, Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                    1, Format.NOT_SPECIFIED, Format.FALSE,
                    Format.NOT_SPECIFIED)
        };
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setFirstSceneChangeThreshold(double firstSceneChangeThreshold) {
        this.firstSceneChangeThreshold = firstSceneChangeThreshold;
    }

    public void setSceneChangeThreshold(double sceneChangeThreshold) {
        this.sceneChangeThreshold = sceneChangeThreshold;
    }

    public int getNBlocks() {
        return nblk;
    }

    public void setImmediatlyNotifyChange(boolean immediatelyNotifyChange) {
        this.immediatelyNotifyChange = immediatelyNotifyChange;
    }

    /**
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return inputFormats;
        }
        return new Format[]{input};
    }

    private void save(QuickArrayAbstract lum, int pos, int stride) {
        for (int i = 16; --i >= 0;) {
            refbuf.copy(lum, pos, pos, 16);
            pos += stride;
        }
    }

    /*
     * Default save routine -- stuff new luma blocks into cache.
     */
    private void saveblks(QuickArrayAbstract lum) {
        int crv = 0;
        int pos = 0;
        int stride = width;
        for (int y = 0; y < blkh; y++) {
            int lineStart = pos;
            for (int x = 0; x < blkw; x++) {
                if (crvec[crv++] == 1) {
                    if (yuvFormat == null) {
                        convertBlock(lum, devbuf, x, y);
                        save(devbuf, pos, width);
                    } else {
                        save(lum, pos, width);
                    }
                }
                pos += 16;
            }
            pos = lineStart + (stride * 16);
        }
    }

    private void convertBlockLine(QuickArrayAbstract inBuf,
            QuickArrayAbstract devBuf, int line, int blkx, int blky) {
        int x = blkx * 16;
        int y = (blky * 16) + line;
        int posOut = (y * width) + x;
        int posIn = (y * lineStride) + (x * pixelStride);

        for (int i = 0; i < 16; i++) {
            int r = 0;
            int g = 0;
            int b = 0;
            if (dataType.equals(Format.byteArray)) {
                r = inBuf.getByte(posIn + redMask - 1) & BYTE_TO_INT_MASK;
                g = inBuf.getByte(posIn + greenMask - 1) & BYTE_TO_INT_MASK;
                b = inBuf.getByte(posIn + blueMask - 1) & BYTE_TO_INT_MASK;
            } else if (dataType.equals(Format.intArray)) {
                int rgb = inBuf.getInt(posIn);
                r = (rgb & redMask) >> redShift;
                g = (rgb & greenMask) >> greenShift;
                b = (rgb & blueMask) >> blueShift;
            }

            int yVal = (int) (CCITT_Y_RED_MULTIPLIER * r)
                  + (int) (CCITT_Y_GREEN_MULTIPLIER * g)
                  + (int) (CCITT_Y_BLUE_MULTIPLIER * b)
                  + CCITT_Y_CONSTANT;
            devBuf.setByte(posOut, (byte) (yVal & BYTE_TO_INT_MASK));

            posOut += 1;
            posIn += pixelStride;
        }
    }

    private void convertBlock(QuickArrayAbstract inBuf,
            QuickArrayAbstract devBuf, int blkx, int blky) {
        for (int i = 0; i < 16; i++) {
            convertBlockLine(inBuf, devBuf, i, blkx, blky);
        }
    }



    public int process(Buffer input, Buffer output) {
        int result = process(input);
        output.copy(input);
        return result;
    }

    /**
     *
     * @see javax.media.Renderer#process(javax.media.Buffer)
     */
    public int process(Buffer bufIn) {
        if (crvec == null) {
            setInputFormat(bufIn.getFormat());

            try {
                refbuf = new QuickArray(byte[].class, size.width * size.height);
                refbuf.clear();
                if (yuvFormat == null) {
                    devbuf = new QuickArray(byte[].class,
                            size.width * size.height);
                    ((QuickArray) devbuf).clear();
                }
            } catch (QuickArrayException e) {
                e.printStackTrace();
                return BUFFER_PROCESSED_FAILED;
            }
            crvec = new int[nblk];
            lastBuffer = null;
        }
        scan = (scan + 3) & 7;
        Object inObject = bufIn.getData();
        if (inObject == null) {
            return BUFFER_PROCESSED_OK;
        }

        int ds = width;
        int rs = width;
        QuickArrayWrapper in = null;
        try {
            if (yuvFormat == null) {
                in = new QuickArrayWrapper(inObject, bufIn.getOffset(),
                        bufIn.getLength());
            } else {
                in = new QuickArrayWrapper(inObject, bufIn.getOffset()
                        + yuvFormat.getOffsetY(), bufIn.getLength());
            }
        } catch (QuickArrayException e) {
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        }

        lock();
        int db = scan * ds;
        int rb = scan * rs;
        int w = blkw;
        int crv = 0;
        for (int y = 0; y < blkh; ++y) {
            int ndb = db;
            int nrb = rb;
            int ncrv = crv;
            for (int x = 0; x < blkw; x++) {
                if (yuvFormat == null) {
                    convertBlockLine(in, devbuf, scan, x, y);
                } else {
                    devbuf = in;
                }
                int left = 0;
                int right = 0;
                int top = 0;
                int bottom = 0;
                left += (devbuf.getByte(db + 0) & 0xFF)
                      - (refbuf.getByte(rb + 0) & 0xFF);
                left += (devbuf.getByte(db + 1) & 0xFF)
                      - (refbuf.getByte(rb + 1) & 0xFF);
                left += (devbuf.getByte(db + 2) & 0xFF)
                      - (refbuf.getByte(rb + 2) & 0xFF);
                left += (devbuf.getByte(db + 3) & 0xFF)
                      - (refbuf.getByte(rb + 3) & 0xFF);
                top += (devbuf.getByte(db + 0 + 1 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 0 + 1 * 4) & 0xFF);
                top += (devbuf.getByte(db + 1 + 1 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 1 + 1 * 4) & 0xFF);
                top += (devbuf.getByte(db + 2 + 1 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 2 + 1 * 4) & 0xFF);
                top += (devbuf.getByte(db + 3 + 1 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 3 + 1 * 4) & 0xFF);
                top += (devbuf.getByte(db + 0 + 2 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 0 + 2 * 4) & 0xFF);
                top += (devbuf.getByte(db + 1 + 2 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 1 + 2 * 4) & 0xFF);
                top += (devbuf.getByte(db + 2 + 2 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 2 + 2 * 4) & 0xFF);
                top += (devbuf.getByte(db + 3 + 2 * 4) & 0xFF)
                     - (refbuf.getByte(rb + 3 + 2 * 4) & 0xFF);
                right += (devbuf.getByte(db + 0 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 0 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 1 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 1 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 2 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 2 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 3 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 3 + 3 * 4) & 0xFF);
                right = Math.abs(right);
                left = Math.abs(left);
                top = Math.abs(top);
                db += ds << 3;
                rb += rs << 3;
                left += (devbuf.getByte(db + 0) & 0xFF)
                      - (refbuf.getByte(rb + 0) & 0xFF);
                left += (devbuf.getByte(db + 1) & 0xFF)
                      - (refbuf.getByte(rb + 1) & 0xFF);
                left += (devbuf.getByte(db + 2) & 0xFF)
                      - (refbuf.getByte(rb + 2) & 0xFF);
                left += (devbuf.getByte(db + 3) & 0xFF)
                      - (refbuf.getByte(rb + 3) & 0xFF);
                bottom += (devbuf.getByte(db + 0 + 1 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 0 + 1 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 1 + 1 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 1 + 1 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 2 + 1 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 2 + 1 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 3 + 1 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 3 + 1 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 0 + 2 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 0 + 2 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 1 + 2 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 1 + 2 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 2 + 2 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 2 + 2 * 4) & 0xFF);
                bottom += (devbuf.getByte(db + 3 + 2 * 4) & 0xFF)
                        - (refbuf.getByte(rb + 3 + 2 * 4) & 0xFF);
                right += (devbuf.getByte(db + 0 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 0 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 1 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 1 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 2 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 2 + 3 * 4) & 0xFF);
                right += (devbuf.getByte(db + 3 + 3 * 4) & 0xFF)
                       - (refbuf.getByte(rb + 3 + 3 * 4) & 0xFF);
                right = Math.abs(right);
                left = Math.abs(left);
                bottom = Math.abs(bottom);
                db -= ds << 3;
                rb -= rs << 3;

                int center = 0;
                if (left >= threshold && x > 0) {
                    crvec[crv - 1] = 1;
                    center = 1;
                }
                if (right >= threshold && x < w - 1) {
                    crvec[crv + 1] = 1;
                    center = 1;
                }
                if (bottom >= threshold && y < blkh - 1) {
                    crvec[crv + w] = 1;
                    center = 1;
                }
                if (top >= threshold && y > 0) {
                    crvec[crv - w] = 1;
                    center = 1;
                }
                if (center > 0) {
                    crvec[crv + 0] = 1;
                }

                db += 16;
                rb += 16;
                ++crv;
            }
            db = ndb + (ds << 4);
            rb = nrb + (rs << 4);
            crv = ncrv + w;
        }
        double diffCount = 0;
        for (int i = 0; i < nblk; i++) {
            diffCount += crvec[i];
        }
        double fDiff = (diffCount / nblk);
        if ((firstScene && (fDiff >= firstSceneChangeThreshold))
                || (!firstScene && (fDiff >= sceneChangeThreshold))) {
            firstScene = false;
            lastUpdateTime = bufIn.getTimeStamp();
            saveblks(in);
            for (int i = 0; i < nblk; i++) {
                crvec[i] = 0;
            }
            if (immediatelyNotifyChange) {
                notifyChange(bufIn);
            }
        } else if (lastUpdateTime != -1) {
            notifyChange(bufIn);
        }

        release();
        return BUFFER_PROCESSED_OK;
    }

    private void notifyChange(Buffer bufIn) {
        if (yuvFormat != null) {
            if (toRGB == null) {
                Dimension size = yuvFormat.getSize();
                format = new RGBFormat(size, size.width * size.height,
                        Format.intArray,
                        Format.NOT_SPECIFIED, 32, 0x00FF0000,
                        0x0000FF00, 0x000000FF,
                        1, size.width, Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
                try {
                    toRGB = new SimpleProcessor(yuvFormat, format);
                } catch (UnsupportedFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        if (toRGB != null) {
            toRGB.process(bufIn);
            lastBuffer = toRGB.getOutputBuffer();
        } else {
            lastBuffer = bufIn;
        }
        for (int i = 0; i < screenListeners.size(); i++) {
            final CaptureChangeListener listener = screenListeners.get(i);
            listener.captureDone(lastUpdateTime);
        }
        lastUpdateTime = -1;
    }

    private void setSize() {
        if (format != null) {
            size = format.getSize();
        } else {
            size = yuvFormat.getSize();
        }
        if (size != null) {
            this.width = size.width;
            blkw = size.width >> 4;
            blkh = size.height >> 4;
            nblk = blkw * blkh;
        }
    }

    /**
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format input) {
        if (input instanceof RGBFormat) {
            format = (RGBFormat) input;
            pixelStride = format.getPixelStride();
            lineStride = format.getLineStride();
            redMask = format.getRedMask();
            blueMask = format.getBlueMask();
            greenMask = format.getGreenMask();
            dataType = format.getDataType();
            if (dataType.equals(Format.intArray)) {
                redShift = Bits.getShift(redMask);
                blueShift = Bits.getShift(blueMask);
                greenShift = Bits.getShift(greenMask);
            }
            setSize();
            return input;
        } else if (input instanceof YUVFormat) {
            yuvFormat = (YUVFormat) input;
            setSize();
            return input;
        }

        return null;
    }

    public Format setOutputFormat(Format format) {
        return format;
    }

    /**
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        nblk = -1;
        if (refbuf != null) {
            refbuf.free();
        }
        if ((devbuf != null) && (devbuf instanceof QuickArray)) {
            ((QuickArray) devbuf).free();
        }
    }

    /**
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return NAME;
    }

    /**
     * @see javax.media.PlugIn#open()
     */
    public void open() {
        // no action required
    }

    /**
     * @see javax.media.PlugIn#reset()
     */
    public void reset() {

        // no action required
    }

    /**
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String arg0) {
        return null;
    }

    /**
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return null;
    }

    /**
     * Gets the latest captured image
     * @return the latest captured image
     */
    public BufferedImage getImage() {
        BufferedImage image = null;
        if (format.getDataType() == Format.intArray) {
            image = new BufferedImage(
                    format.getSize().width,
                    format.getSize().height,
                    BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                    (int[]) lastBuffer.getData(), 0, image.getWidth());
        } else if (format.getDataType() == Format.byteArray) {
            int w = format.getSize().width;
            int h = format.getSize().height;
            int pixStride = format.getPixelStride();
            int scanlineStride = format.getLineStride();
            int[] bandOffsets = {format.getRedMask() - 1,
                    format.getGreenMask() - 1, format.getBlueMask() - 1};
            byte[] data = new byte[lastBuffer.getLength()];
            System.arraycopy(lastBuffer.getData(), lastBuffer.getOffset(),
                    data, 0, lastBuffer.getLength());
            DataBuffer buffer = new DataBufferByte(
                    data, w * h);
            WritableRaster raster = Raster.createInterleavedRaster(
                    buffer, w, h, scanlineStride, pixStride, bandOffsets,
                    null);

            ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            boolean hasAlpha = false;
            boolean isAlphaPremultiplied = false;
            int transparency = ComponentColorModel.OPAQUE;
            int transferType = DataBuffer.TYPE_BYTE;
            ColorModel colorModel = new ComponentColorModel(colorSpace,
                    hasAlpha, isAlphaPremultiplied, transparency, transferType);

            image = new BufferedImage(colorModel, raster, isAlphaPremultiplied,
                    null);
            if (format.getFlipped() == RGBFormat.TRUE) {
                AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                tx.translate(0, -image.getHeight(null));
                AffineTransformOp op = new AffineTransformOp(tx,
                        AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                image = op.filter(image, null);
            }
        }
        return image;
    }

    /**
     * Locks the thread against updates
     */
    public synchronized void lock() {
        while (locked) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        locked = true;
    }
    /**
     * Allows updates to proceed
     */
    public synchronized void release() {
        locked = false;
        notifyAll();
    }

    /**
     * Adds a new listener to screen capture events
     * @param listener The listener to add
     */
    public void addScreenListener(CaptureChangeListener listener) {
        if (!screenListeners.contains(listener)) {
            screenListeners.add(listener);
            if (hasImage()) {
                listener.captureDone(lastUpdateTime);
            }
        }
    }

    /**
     * Removes a listener of screen capture events
     * @param listener The listener to remove
     */
    public void removeScreenListener(CaptureChangeListener listener) {
        screenListeners.remove(listener);
    }

    /**
     *
     * @see javax.media.Renderer#start()
     */
    public void start() {
        // Does Nothing
    }

    /**
     *
     * @see javax.media.Renderer#stop()
     */
    public void stop() {
        // Does Nothing
    }

    public boolean hasImage() {
        return lastBuffer != null;
    }
}
