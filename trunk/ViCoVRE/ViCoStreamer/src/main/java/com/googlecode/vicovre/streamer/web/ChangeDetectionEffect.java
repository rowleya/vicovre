package com.googlecode.vicovre.streamer.web;

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
import javax.media.format.RGBFormat;

import com.googlecode.vicovre.media.Bits;

public class ChangeDetectionEffect implements Effect {

    // The size of the block to be considered in change detection
    private static final int BLOCK_SIZE = 8;

    // The height to resize the incoming image to
    private static final int IMAGE_HEIGHT = 288;

    // The width to resize the incoming image to
    private static final int IMAGE_WIDTH = 352;

    // The threshold of the difference between two blocks before one is sent
    private static final int THRESHOLD = 10;

    // The constant for the change detection post threshold
    private static final int POST_THRESH_CONSTANT = 8;

    // The multiplier for the change detection post threshold
    private static final double POST_THRESH_MULTIPLIER = 0.104;

    // The constant for the change detection pre threshold
    private static final int PRE_THRESH_CONSTANT = 15;

    // The multiplier for the change detection pre threshold
    private static final double PRE_THRESH_MUTLIPLIER = -0.08;

    // The threshold for the change detection
    private static final int BLOCK_AVG_THRESHOLD = 125;

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

    // The number of bits per pixel to consider
    private static final int BITS_PER_PIXEL = 8;

    // The name of the effect
    private static final String NAME = "Change Detection Effect";

    // The input/output format
    private RGBFormat format = null;

    // The allowed input formats
    private Format[] inputFormats;

    // The allowed output formats
    private Format[] outputFormats;

    // The last buffer stored
    private Buffer lastBuffer = null;

    // The type of the input
    private Class<?> type = null;

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

    // The number of red bits
    private int redBits = 0;

    // The number of green bits
    private int greenBits = 0;

    // The number of blue bits
    private int blueBits = 0;

    // The horizontal resize ratio of the input
    private float horRatio = 0;

    // The vertical resize ratio of the input
    private float verRatio = 0;

    // The lock status
    private boolean locked = false;

    // The listeners to screen change events
    private Vector <CaptureChangeListener> screenListeners =
        new Vector<CaptureChangeListener>();

    /**
     * Creates a new ChangeDetectionEffect
     *
     */
    public ChangeDetectionEffect() {
        inputFormats = new Format[] {
            new RGBFormat()
        };

        outputFormats = new Format[] {
            new RGBFormat()
        };
    }

    /**
     * @see javax.media.Codec#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    /**
     * @see javax.media.Codec#getSupportedOutputFormats(javax.media.Format)
     */
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return outputFormats;
        }

        if (matches(input, inputFormats) != null) {
            return new Format[] {outputFormats[0].intersects(input)};
        }
        return new Format[0];
    }

    /**
     * @see javax.media.Codec#process(javax.media.Buffer, javax.media.Buffer)
     */
    public int process(Buffer bufIn, Buffer bufOut) {
        lock();
        if (format == null) {
            format = (RGBFormat) bufIn.getFormat();
            type = format.getDataType();
            pixelStride = format.getPixelStride();
            lineStride = format.getLineStride();
            redMask = format.getRedMask();
            blueMask = format.getBlueMask();
            greenMask = format.getGreenMask();
            redShift = Bits.getShift(redMask);
            blueShift = Bits.getShift(blueMask);
            greenShift = Bits.getShift(greenMask);
            redBits = Bits.getBitCount(redMask, redShift);
            greenBits = Bits.getBitCount(greenMask, greenShift);
            blueBits = Bits.getBitCount(blueMask, blueShift);
            Dimension size = format.getSize();
            horRatio = (float) size.getWidth() / IMAGE_WIDTH;
            verRatio = (float) size.getHeight() / IMAGE_HEIGHT;
        }
        if ((lastBuffer == null) || hasChanged(lastBuffer, bufIn)) {
            lastBuffer = (Buffer) bufIn.clone();
            for (int i = 0; i < screenListeners.size(); i++) {
                CaptureChangeListener listener = screenListeners.get(i);
                listener.captureDone(getImage());
            }
        }
        release();
        bufOut.copy(bufIn);
        return BUFFER_PROCESSED_OK;
    }

    /**
     * @see javax.media.Codec#setInputFormat(javax.media.Format)
     */
    public Format setInputFormat(Format input) {
        if (input instanceof RGBFormat) {
            return input;
        }
        return null;
    }

    /**
     * @see javax.media.Codec#setOutputFormat(javax.media.Format)
     */
    public Format setOutputFormat(Format output) {
        return output;

    }

    /**
     * @see javax.media.PlugIn#close()
     */
    public void close() {

        // no action required
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

    // Utility methods.
    private Format matches(Format in, Format outs[]) {
        for (int i = 0; i < outs.length; i++) {
            if (in.matches(outs[i])) {
                return outs[i];
            }
        }

        return null;
    }

    private boolean hasChanged(Buffer lastBuffer, Buffer thisBuffer) {
        int lastOffset = lastBuffer.getOffset();
        int thisOffset = thisBuffer.getOffset();
        Object lastIn = lastBuffer.getData();
        Object thisIn = thisBuffer.getData();

        if (thisIn == null) {
            return false;
        }

        for (int i = 0; i < (IMAGE_WIDTH - BLOCK_SIZE); i += BLOCK_SIZE) {
            for (int j = 0; j < (IMAGE_HEIGHT - BLOCK_SIZE); j += BLOCK_SIZE) {
                int[][] diffY = new int[BLOCK_SIZE][BLOCK_SIZE];
                int sumY = 0;
                for (int y = j; y < (j + BLOCK_SIZE); y++) {
                    int lineIn = (int) (y * verRatio);
                    for (int x = i; x < (i + BLOCK_SIZE); x++) {
                        int pixelIn = (int) (x * horRatio);
                        int lastPos = (lineIn * lineStride)
                            + (pixelIn * pixelStride) + lastOffset;
                        int thisPos = (lineIn * lineStride)
                            + (pixelIn * pixelStride) + thisOffset;
                        int lastRed = 0;
                        int lastGreen = 0;
                        int lastBlue = 0;
                        int thisRed = 0;
                        int thisGreen = 0;
                        int thisBlue = 0;
                        if (type.equals(Format.byteArray)) {
                            byte[] lastData = (byte []) lastIn;
                            byte[] thisData = (byte []) thisIn;
                            lastRed = lastData[lastPos + redMask - 1]
                                       & BYTE_TO_INT_MASK;
                            lastGreen = lastData[lastPos + greenMask - 1]
                                       & BYTE_TO_INT_MASK;
                            lastBlue = lastData[lastPos + blueMask - 1]
                                       & BYTE_TO_INT_MASK;
                            thisRed = thisData[thisPos + redMask - 1]
                                               & BYTE_TO_INT_MASK;
                            thisGreen = thisData[thisPos + greenMask - 1]
                                       & BYTE_TO_INT_MASK;
                            thisBlue = thisData[thisPos + blueMask - 1]
                                       & BYTE_TO_INT_MASK;
                        } else if (type.equals(Format.shortArray)) {
                            short[] lastData = (short []) lastIn;
                            short[] thisData = (short []) thisIn;
                            lastRed = ((lastData[lastPos] & redMask)
                                >> redShift) << (BITS_PER_PIXEL - redBits);
                            lastGreen = ((lastData[lastPos] & greenMask)
                                >> greenShift) << (BITS_PER_PIXEL - greenBits);
                            lastBlue = ((lastData[lastPos] & blueMask)
                                >> blueShift) << (BITS_PER_PIXEL - blueBits);
                            thisRed = ((thisData[thisPos] & redMask)
                                >> redShift) << (BITS_PER_PIXEL - redBits);
                            thisGreen = ((thisData[thisPos] & greenMask)
                                >> greenShift) << (BITS_PER_PIXEL - greenBits);
                            thisBlue = ((thisData[thisPos] & blueMask)
                                >> blueShift) << (BITS_PER_PIXEL - blueBits);

                        } else if (type.equals(Format.intArray)) {
                            int[] lastData = (int []) lastIn;
                            int[] thisData = (int []) thisIn;
                            lastRed = ((lastData[lastPos] & redMask)
                                    >> redShift) << (BITS_PER_PIXEL - redBits);
                            lastGreen = ((lastData[lastPos] & greenMask)
                                >> greenShift) << (BITS_PER_PIXEL - greenBits);
                            lastBlue = ((lastData[lastPos] & blueMask)
                                >> blueShift) << (BITS_PER_PIXEL - blueBits);
                            thisRed = ((thisData[thisPos] & redMask)
                                >> redShift) << (BITS_PER_PIXEL - redBits);
                            thisGreen = ((thisData[thisPos] & greenMask)
                                >> greenShift) << (BITS_PER_PIXEL - greenBits);
                            thisBlue = ((thisData[thisPos] & blueMask)
                                >> blueShift) << (BITS_PER_PIXEL - blueBits);
                        }

                        // Values based on CCITT RGB to YUV conversion
                        int lastY = (int) (CCITT_Y_RED_MULTIPLIER * lastRed)
                                + (int) (CCITT_Y_GREEN_MULTIPLIER * lastGreen)
                                + (int) (CCITT_Y_BLUE_MULTIPLIER * lastBlue)
                                + CCITT_Y_CONSTANT;
                        int thisY = (int) (CCITT_Y_RED_MULTIPLIER * thisRed)
                            + (int) (CCITT_Y_GREEN_MULTIPLIER * thisGreen)
                            + (int) (CCITT_Y_BLUE_MULTIPLIER * thisBlue)
                            + CCITT_Y_CONSTANT;
                        sumY += thisY;
                        diffY[x - i][y - j] = Math.abs(thisY - lastY);
                    }
                }
                int blockAvg = sumY / (BLOCK_SIZE * BLOCK_SIZE);
                int yThresh = 0;
                if (blockAvg <= BLOCK_AVG_THRESHOLD) {
                    yThresh = (int) ((blockAvg * PRE_THRESH_MUTLIPLIER)
                            + PRE_THRESH_CONSTANT);
                } else {
                    yThresh = (int) ((blockAvg * POST_THRESH_MULTIPLIER)
                            - POST_THRESH_CONSTANT);
                }

                int diffSum = 0;
                for (int x = 0; x < BLOCK_SIZE; x++) {
                    for (int y = 0; y < BLOCK_SIZE; y++) {
                        if (diffY[x][y] > yThresh) {
                            diffSum += diffY[x][y] / yThresh;
                        }
                    }
                }
                if (diffSum > THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
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
            int pixelStride = format.getPixelStride();
            int scanlineStride = format.getLineStride();
            int[] bandOffsets = {format.getRedMask() - 1,
                    format.getGreenMask() - 1, format.getBlueMask() - 1};
            DataBuffer buffer = new DataBufferByte(
                    (byte[]) lastBuffer.getData(), w * h);
            WritableRaster raster = Raster.createInterleavedRaster(
                    buffer, w, h, scanlineStride, pixelStride, bandOffsets,
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
        }
    }

    /**
     * Removes a listener of screen capture events
     * @param listener The listener to remove
     */
    public void removeScreenListener(CaptureChangeListener listener) {
        screenListeners.remove(listener);
    }
}
