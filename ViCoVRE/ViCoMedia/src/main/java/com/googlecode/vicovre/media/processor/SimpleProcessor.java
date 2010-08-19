/**
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.googlecode.vicovre.media.processor;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Effect;
import javax.media.Format;
import javax.media.Multiplexer;
import javax.media.PlugIn;
import javax.media.PlugInManager;
import javax.media.Renderer;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.YUVFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;

import com.googlecode.vicovre.media.Misc;

/**
 * Performs simple processing operations
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class SimpleProcessor {

    private LinkedList<Codec> codecs = null;

    private LinkedList<Format> inputFormats = null;

    private LinkedList<Format> outputFormats = null;

    private LinkedList<Buffer> outputBuffers = null;

    private Renderer renderer = null;

    private Multiplexer multiplexer = null;

    private int track = 0;

    private ProcessingThread thread = null;

    private LinkedList<ProcessorListener> listeners =
        new LinkedList<ProcessorListener>();

    private boolean closed = false;

    private boolean firstFrameProcessed = false;

    private Integer firstFrameSync = new Integer(0);

    private class CodecIterator {

        private ListIterator<Codec> codecIterator = null;

        private ListIterator<Format> inputFormatIterator = null;

        private ListIterator<Format> outputFormatIterator = null;

        private ListIterator<Buffer> outputBufferIterator = null;

        private Codec codec = null;

        private Format inputFormat = null;

        private Buffer outputBuffer = null;

        private Buffer lastBuffer = null;

        private Buffer firstBuffer = null;

        private Format outputFormat = null;

        private CodecIterator(List<Codec> codecs,
                List<Format> inputFormats, List<Format> outputFormats,
                List<Buffer> buffers, Buffer firstBuffer) {
            codecIterator = codecs.listIterator();
            inputFormatIterator = inputFormats.listIterator();
            outputFormatIterator = outputFormats.listIterator();
            outputBufferIterator = buffers.listIterator();
            outputBuffer = firstBuffer;
            this.firstBuffer = firstBuffer;
        }

        private boolean hasNext() {
            return codecIterator.hasNext();
        }

        private void next() {
            lastBuffer = outputBuffer;
            codec = codecIterator.next();
            inputFormat = inputFormatIterator.next();
            outputFormat = outputFormatIterator.next();
            outputBuffer = outputBufferIterator.next();
        }

        private void previous() {
            outputBuffer = outputBufferIterator.previous();
            outputFormat = outputFormatIterator.previous();
            inputFormat = inputFormatIterator.previous();
            codec = codecIterator.previous();
            if (outputBufferIterator.hasPrevious()) {
                lastBuffer = outputBufferIterator.previous();
                outputBufferIterator.next();
            } else {
                lastBuffer = firstBuffer;
            }
        }

        private Buffer getInputBuffer() {
            return lastBuffer;
        }

        private Buffer getOutputBuffer() {
            return outputBuffer;
        }

        private Codec getCodec() {
            return codec;
        }

        private Format getInputFormat() {
            return inputFormat;
        }

        private void setInputFormat(Format inputFormat) {
            inputFormatIterator.set(inputFormat);
        }

        private Format getOutputFormat() {
            return outputFormat;
        }

        private void setOutputFormat(Format outputFormat) {
            outputFormatIterator.set(outputFormat);
        }

        private void insertBefore(Codec codec, Format inputFormat,
                Format outputFormat, Buffer outputBuffer) {
            codecIterator.previous();
            codecIterator.add(codec);
            codecIterator.next();

            inputFormatIterator.previous();
            inputFormatIterator.add(inputFormat);
            inputFormatIterator.next();

            outputFormatIterator.previous();
            outputFormatIterator.add(outputFormat);
            outputFormatIterator.next();

            outputBufferIterator.previous();
            outputBufferIterator.add(outputBuffer);
            outputBufferIterator.next();
        }
    }

    /**
     * Creates a new Processor for a multiplexer
     *
     * @param inputFormat The input format to start with
     * @param multiplexer The multiplexer to finish with
     * @param track the track to set in the multiplexer
     * @throws UnsupportedFormatException
     * @throws ResourceUnavailableException
     */
    public SimpleProcessor(Format inputFormat, Multiplexer multiplexer,
            int track) throws UnsupportedFormatException,
            ResourceUnavailableException {
        init(inputFormat, multiplexer, track);
    }

    /**
     * Creates a new Processor for a renderer
     *
     * @param inputFormat The input format to start with
     * @param renderer The renderer to finish with
     * @throws UnsupportedFormatException
     * @throws ResourceUnavailableException
     */
    public SimpleProcessor(Format inputFormat, Renderer renderer)
            throws UnsupportedFormatException, ResourceUnavailableException {
        init(inputFormat, renderer);
    }

    /**
     * Creates a new SimpleProcessor
     * @param inputFormat The input format
     * @param outputFormat The desired output format or null to specify that the
     *                     output should be LINEAR Audio, or YUV or RGB Video
     * @throws UnsupportedFormatException
     */
    public SimpleProcessor(Format inputFormat, Format outputFormat)
            throws UnsupportedFormatException {
        init(inputFormat, outputFormat);
    }

    private void init(Format inputFormat, Multiplexer multiplexer, int track)
            throws UnsupportedFormatException, ResourceUnavailableException {
        this.multiplexer = multiplexer;
        this.track = track;

        Format[] formats = multiplexer.getSupportedInputFormats();
        boolean inited = false;
        for (int i = 0; (i < formats.length) && !inited; i++) {
            if (formats[i].getClass().isInstance(inputFormat)) {
                try {
                    init(inputFormat, formats[i]);
                    multiplexer.setInputFormat(outputFormats.getLast(),
                            track);
                    multiplexer.open();
                    inited = true;
                } catch (UnsupportedFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!inited) {
            throw new UnsupportedFormatException(inputFormat);
        }
    }

    private void init(Format inputFormat, Renderer renderer)
            throws UnsupportedFormatException, ResourceUnavailableException {
        this.renderer = renderer;
        Format[] formats = renderer.getSupportedInputFormats();
        boolean inited = false;
        for (int i = 0; (formats != null) && (i < formats.length)
                && !inited; i++) {
            if (formats[i].matches(inputFormat)
                    && inputFormat.matches(formats[i])) {
                try {
                    init(inputFormat, formats[i]);
                    Format f = renderer.setInputFormat(
                            outputFormats.getLast());
                    if (f != null) {
                        renderer.open();
                        inited = true;
                    }
                } catch (UnsupportedFormatException e) {
                    // Do Nothing
                }
            }
        }
        for (int i = 0; (formats != null) && (i < formats.length)
                && !inited; i++) {
            try {
                init(inputFormat, formats[i]);
                renderer.setInputFormat(outputFormats.getLast());
                renderer.open();
                inited = true;
            } catch (UnsupportedFormatException e) {
                // Do Nothing
            }
        }
        if (!inited) {
            throw new UnsupportedFormatException(inputFormat);
        }
    }

    private Buffer createBuffer(Format outFormat) {
        Buffer output = new Buffer();
        output.setFormat(outFormat);
        output.setOffset(0);
        output.setLength(0);
        output.setFlags(0);
        output.setSequenceNumber(0);
        output.setTimeStamp(0);
        return output;
    }

    private void init(Format inputFormat, Format outputFormat)
            throws UnsupportedFormatException {
        HashMap<String, Boolean> searched = new HashMap<String, Boolean>();
        Codecs codecList = null;

        System.err.println("Finding codecs from " + inputFormat + " to "
                + outputFormat);

        codecList = search(inputFormat, outputFormat, searched);
        if (codecList == null) {
            throw new UnsupportedFormatException("Cannot translate from "
                    + inputFormat + " to " + outputFormat, inputFormat);
        }
        codecs = new LinkedList<Codec>();
        inputFormats = new LinkedList<Format>();
        outputFormats = new LinkedList<Format>();
        outputBuffers = new LinkedList<Buffer>();
        Iterator<Codec> codec = codecList.codecList.iterator();
        Iterator<Format> iFormat = codecList.inputFormatList.iterator();
        Iterator<Format> oFormat = codecList.outputFormatList.iterator();
        while(codec.hasNext()) {
            Format outFormat = oFormat.next();
            Format inFormat = iFormat.next();
            Buffer output = createBuffer(outFormat);
            codecs.addLast(codec.next());
            inputFormats.addLast(inFormat);
            outputFormats.addLast(outFormat);
            outputBuffers.addLast(output);
        }
    }

    public void waitForFirstFrame() {
        synchronized (firstFrameSync) {
            while (!firstFrameProcessed) {
                try {
                    firstFrameSync.wait();
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
        }
    }

    public boolean insertEffect(Effect effect) {
        CodecIterator iterator = new CodecIterator(codecs, inputFormats,
                outputFormats, outputBuffers, null);
        while (iterator.hasNext()) {
            iterator.next();
            Format input = iterator.getInputFormat();
            Format inputFormat = effect.setInputFormat(input);
            if (inputFormat != null) {
                Format outputFormat = effect.setOutputFormat(inputFormat);
                Buffer output = createBuffer(outputFormat);
                iterator.insertBefore(effect, inputFormat, outputFormat,
                        output);
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the output buffer
     * @return The output buffer
     */
    public Buffer getOutputBuffer() {
        return outputBuffers.getLast();
    }

    /**
     * Gets the output format
     * @return The output format
     */
    public Format getOutputFormat() {
        return outputFormats.getLast();
    }

    /**
     * Processes an input buffer rendering it if necessary
     * @param inputBuffer The buffer to process
     * @return The status of the processing
     */
    public int process(Buffer inputBuffer) {
        return process(inputBuffer, true);
    }

    /**
     * Processes an input buffer
     * @param inputBuffer The buffer to process
     * @param render True if the rendering should be done
     * @return The status of the processing
     */
    public int process(Buffer inputBuffer, boolean render) {
        if (closed) {
            return PlugIn.BUFFER_PROCESSED_FAILED;
        }
        try {
            CodecIterator iterator = new CodecIterator(codecs,
                    inputFormats, outputFormats, outputBuffers, inputBuffer);
            iterator.next();
            int status = process(iterator, render);
            return status;
        } catch (Throwable t) {
            t.printStackTrace();
            return PlugIn.BUFFER_PROCESSED_FAILED;
        }
    }

    private int render(CodecIterator iterator) {
        int status = PlugIn.BUFFER_PROCESSED_OK;
        do {
            Buffer outputBuffer = iterator.getOutputBuffer();
            Format outputFormat = iterator.getOutputFormat();
            if (!outputBuffer.getFormat().equals(outputFormat)) {
                Format format = renderer.setInputFormat(
                        outputBuffer.getFormat());
                iterator.setOutputFormat(format);
            }
            status = renderer.process(outputBuffer);
            if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                return status;
            }
        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        return status;
    }

    private int multiplex() {
        int status = PlugIn.BUFFER_PROCESSED_OK;
        do {
            status = multiplexer.process(outputBuffers.getLast(),
                    track);
            if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                return status;
            }
        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        return status;
    }

    // Processes with a specific codec
    private synchronized int process(CodecIterator iterator, boolean render) {
        int status = PlugIn.BUFFER_PROCESSED_OK;
        Buffer localInputBuffer = iterator.getInputBuffer();
        Codec codec = iterator.getCodec();
        Buffer outputBuffer = iterator.getOutputBuffer();
        if (!localInputBuffer.getFormat().equals(iterator.getInputFormat())) {
            Format format = codec.setInputFormat(
                    localInputBuffer.getFormat());
            iterator.setInputFormat(format);
        }
        long firstInputBufferTimestamp = -1;
        do {

            long lastSequence = outputBuffer.getSequenceNumber();
            outputBuffer.setTimeStamp(0);
            outputBuffer.setOffset(0);
            if (firstInputBufferTimestamp == -1) {
                firstInputBufferTimestamp = localInputBuffer.getTimeStamp();
            }
            if (outputBuffer.getData() != null) {
                outputBuffer.setLength(Array.getLength(
                        outputBuffer.getData()));
            } else {
                outputBuffer.setLength(0);
            }
            outputBuffer.setFlags(0);
            status = codec.process(localInputBuffer,
                    outputBuffer);
            if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                return status;
            }

            if (status != PlugIn.OUTPUT_BUFFER_NOT_FILLED) {
                if (lastSequence == outputBuffer.getSequenceNumber()) {
                    lastSequence += 1;
                    if (lastSequence < 0) {
                        lastSequence = 0;
                    }
                    outputBuffer.setSequenceNumber(lastSequence);
                }
                if (outputBuffer.getTimeStamp() == 0) {
                    outputBuffer.setTimeStamp(firstInputBufferTimestamp);
                }
                firstInputBufferTimestamp = -1;
                if (!outputBuffer.isDiscard()) {
                    if (iterator.hasNext()) {
                        iterator.next();
                        status = process(iterator, render);
                        if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                            iterator.previous();
                            return PlugIn.BUFFER_PROCESSED_FAILED;
                        }
                        iterator.previous();
                    } else {
                        if (!firstFrameProcessed) {
                            synchronized (firstFrameSync) {
                                firstFrameProcessed = true;
                                firstFrameSync.notifyAll();
                            }
                        }
                        if (render && (renderer != null)
                                && ((status == PlugIn.BUFFER_PROCESSED_OK)
                                || (status ==
                                    PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                            if (render(iterator)
                                    == PlugIn.BUFFER_PROCESSED_FAILED) {
                                return PlugIn.BUFFER_PROCESSED_FAILED;
                            }
                        } else if (render && (multiplexer != null)
                                && ((status == PlugIn.BUFFER_PROCESSED_OK)
                                || (status ==
                                    PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                            if (multiplex() == PlugIn.BUFFER_PROCESSED_FAILED) {
                                return PlugIn.BUFFER_PROCESSED_FAILED;
                            }
                        } else if (render && (thread != null)
                                && ((status == PlugIn.BUFFER_PROCESSED_OK)
                                || (status ==
                                    PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                            thread.finishedProcessing();
                        }
                        for (ProcessorListener listener : listeners) {
                            listener.finishedProcessing(outputBuffer);
                        }
                    }
                }
            }

        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        return status;
    }

    private class Codecs {

        private Codecs() {
            // Does Nothing
        };

        private LinkedList<Codec> codecList = new LinkedList<Codec>();

        private LinkedList<Format> inputFormatList = new LinkedList<Format>();

        private LinkedList<Format> outputFormatList = new LinkedList<Format>();
    }

    private Codecs search(Format input, Format output,
            HashMap<String, Boolean> searched) {
        if (((output == null)
                    && (input.matches(new RGBFormat())
                    || input.matches(new YUVFormat())))
                || input.matches(output)) {
            Codecs searchCodecs = new Codecs();
            searchCodecs.codecList.addFirst(new CopyCodec());
            searchCodecs.inputFormatList.addFirst(input);
            searchCodecs.outputFormatList.addFirst(input);
            return searchCodecs;
        }
        Vector< ? > codecsFromHere = null;
        if (output == null) {
            if (input instanceof AudioFormat) {
                codecsFromHere = PlugInManager.getPlugInList(input,
                        new AudioFormat(AudioFormat.LINEAR),
                        PlugInManager.CODEC);
                if (!codecsFromHere.isEmpty()) {
                    output = new AudioFormat(AudioFormat.LINEAR);
                }
            } else {
                codecsFromHere = PlugInManager.getPlugInList(input,
                        new RGBFormat(), PlugInManager.CODEC);
                if (!codecsFromHere.isEmpty()) {
                    output = new RGBFormat();
                } else {
                    codecsFromHere = PlugInManager.getPlugInList(input,
                            new YUVFormat(), PlugInManager.CODEC);
                    if (!codecsFromHere.isEmpty()) {
                        output = new YUVFormat();
                    }
                }
            }
        } else {
            codecsFromHere = PlugInManager.getPlugInList(
                    input, output, PlugInManager.CODEC);
        }
        System.err.println(input + " --> " + output);
        System.err.println("Trying immediate codecs " + codecsFromHere);
        if (!codecsFromHere.isEmpty()) {
            Codecs searchCodecs = new Codecs();
            for (int i = 0; i < codecsFromHere.size(); i++) {
                String codecClassName = (String) codecsFromHere.get(i);
                try {
                    Codec codec = (Codec) Misc.loadPlugin(codecClassName);
                    int matched = -1;
                    Format out = null;
                    Format in = codec.setInputFormat(input);
                    Format[] outs = codec.getSupportedOutputFormats(in);
                    for (int j = 0; (j < outs.length)
                            && (matched == -1); j++) {
                        if (output.matches(outs[j])) {
                            out = codec.setOutputFormat(output.intersects(
                                    outs[j]));
                            matched = j;
                        }
                    }

                    if (matched != -1) {
                        codec.open();
                        searchCodecs.codecList.addFirst(codec);
                        searchCodecs.inputFormatList.addFirst(in);
                        searchCodecs.outputFormatList.addFirst(out);
                        return searchCodecs;
                    }
                    System.err.println("Immediate codec " + codecClassName
                            + " failed");
                } catch (Exception e) {
                    System.err.println("Warning: " + e.getMessage());
                }
            }
        }

        codecsFromHere = PlugInManager.getPlugInList(input, null,
                PlugInManager.CODEC);
        System.err.println("Trying codecs " + codecsFromHere);
        Vector<PathElement> paths = new Vector<PathElement>();
        for (int i = 0; i < codecsFromHere.size(); i++) {
            String codecClassName = (String) codecsFromHere.get(i);
            if (!searched.containsKey(codecClassName)) {
                try {
                    Codec codec = (Codec) Misc.loadPlugin(codecClassName);
                    codec.setInputFormat(input);
                    Format[] formats = codec.getSupportedOutputFormats(input);
                    for (int j = 0; j < formats.length; j++) {
                        paths.add(new PathElement(codec, formats[j]));
                    }
                } catch (Exception e) {
                    System.err.println("Warning: " + e.getMessage());
                }
            }
        }
        Collections.sort(paths, new ClosestCodecComparator(output));

        for (int i = 0; i < paths.size(); i++) {
            PathElement element = paths.get(i);
            Codec codec = element.getCodec();
            String codecClassName = codec.getClass().getName();
            searched.put(codecClassName, true);
            Format format = element.getFormat();
            try {
                Format next = codec.setOutputFormat(format);
                System.err.println("Trying codec " + codecClassName);
                Codecs searchCodecs = search(next, output, searched);
                if (searchCodecs != null) {
                    codec.open();
                    searchCodecs.codecList.addFirst(codec);
                    searchCodecs.inputFormatList.addFirst(input);
                    searchCodecs.outputFormatList.addFirst(next);
                    return searchCodecs;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            searched.remove(codecClassName);
        }
        return null;
    }

    /**
     * Closes the codecs
     *
     */
    public synchronized void close() {
        if (!closed) {
            closed = true;
            stop();
            for (Codec codec : codecs) {
                codec.close();
            }
            codecs.clear();
            for (Buffer buffer : outputBuffers) {
                buffer.setData(null);
            }
            outputBuffers.clear();
            inputFormats.clear();
            outputFormats.clear();
            inputFormats = null;
            outputFormats = null;
            outputBuffers = null;
            codecs = null;
            System.gc();
        }
    }

    /**
     * Starts the processing of a track of a datasource
     * @param ds The datasource
     * @param track The track to process
     */
    public void start(DataSource ds, int track) {
        if (thread == null) {
            thread = new ProcessingThread(ds, track, this);
        }
        thread.start();
        thread.waitForStart();
        System.err.println("Starting processing for " + ds.getClass());
    }

    /**
     * Stops the processing of a datasource
     *
     */
    public void stop() {
        if (thread != null) {
            thread.close();
            thread = null;
        }
    }

    /**
     * Gets a datasouce output of the processor
     * @param ds The datasource input
     * @param track The track of the input to process
     * @return The output data source
     */
    public PushBufferDataSource getDataOutput(DataSource ds, int track) {
        if (thread == null) {
            thread = new ProcessingThread(ds, track, this);
        }
        return new ProcessorSource(thread);
    }

    /**
     * Gets a control
     * @param className The class of the control
     * @return The control or null if none
     */
    public Object getControl(String className) {
        for (Codec codec : codecs) {
            Object control = codec.getControl(className);
            if (control != null) {
                return control;
            }
        }
        if (renderer != null) {
            Object control = renderer.getControl(className);
            if (control != null) {
                return control;
            }
        }
        return null;
    }

    /**
     * Gets the most recent buffer in a particular format
     * @param format The format to get the buffer in
     * @return The buffer or null if none found
     */
    public Buffer getBuffer(Format format) {
        Iterator<Format> formatIterator = outputFormats.iterator();
        Iterator<Buffer> bufferIterator = outputBuffers.iterator();
        while (formatIterator.hasNext()) {
            Buffer buffer = bufferIterator.next();
            if (format.matches(formatIterator.next())) {
                return buffer;
            }
        }
        return null;
    }

    public void printChain(PrintStream out) {
        CodecIterator iter = new CodecIterator(codecs, inputFormats,
                outputFormats, outputBuffers, null);
        while (iter.hasNext()) {
            iter.next();
            out.println(iter.getInputFormat() + " -> " + iter.getCodec());
        }
        out.println(outputFormats.getLast());
    }

    public void addListener(ProcessorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProcessorListener listener) {
        listeners.remove(listener);
    }
}
