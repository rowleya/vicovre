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
import javax.media.format.VideoFormat;
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

    private Format inputFormat = null;

    private Format outputFormat = null;

    private Buffer outputBuffer = null;

    private Vector<Effect> insertedEffects = new Vector<Effect>();

    private Renderer renderer = null;

    private Multiplexer multiplexer = null;

    private Format requestedOutputFormat = null;

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

        private Format firstInputFormat = null;

        private Format outputFormat = null;

        private int index = -1;

        private CodecIterator(List<Codec> codecs,
                List<Format> inputFormats, List<Format> outputFormats,
                List<Buffer> buffers, Buffer firstBuffer,
                Format firstInputFormat) {
            codecIterator = codecs.listIterator();
            inputFormatIterator = inputFormats.listIterator();
            outputFormatIterator = outputFormats.listIterator();
            outputBufferIterator = buffers.listIterator();
            outputBuffer = firstBuffer;
            outputFormat = firstInputFormat;
            inputFormat = firstInputFormat;
            this.firstBuffer = firstBuffer;
            this.firstInputFormat = firstInputFormat;
        }

        private void reset(List<Codec> codecs,
                List<Format> inputFormats, List<Format> outputFormats,
                List<Buffer> buffers, int index) {
            codecIterator = codecs.listIterator(index);
            inputFormatIterator = inputFormats.listIterator(index);
            outputFormatIterator = outputFormats.listIterator(index);
            outputBufferIterator = buffers.listIterator(index);
            if (index > 0) {
                codec = codecs.get(index - 1);
                outputBuffer = buffers.get(index - 1);
                outputFormat = outputFormats.get(index - 1);
                inputFormat = inputFormats.get(index - 1);
            } else {
                codec = null;
                outputBuffer = firstBuffer;
                outputFormat = firstInputFormat;
                inputFormat = firstInputFormat;
            }
            this.index = index - 1;
        }

        private int getIndex() {
            return index;
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
            index += 1;
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
            index -=1;
        }

        private Buffer getInputBuffer() {
            return lastBuffer;
        }

        private Buffer getOutputBuffer() {
            return outputBuffer;
        }

        private Format getOutputFormat() {
            return outputFormat;
        }

        private Codec getCodec() {
            return codec;
        }

        private Format getInputFormat() {
            return inputFormat;
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
                    multiplexer.setInputFormat(getOutputFormat(),
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
                            getOutputFormat());
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
                renderer.setInputFormat(getOutputFormat());
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
        this.requestedOutputFormat = outputFormat;
        this.inputFormat = inputFormat;
        codecs = new LinkedList<Codec>();
        inputFormats = new LinkedList<Format>();
        outputFormats = new LinkedList<Format>();
        outputBuffers = new LinkedList<Buffer>();
        setupCodecs(inputFormat, codecList, codecs, inputFormats, outputFormats,
                outputBuffers);
    }

    private void setupCodecs(Format inputFormat, Codecs codecList,
            LinkedList<Codec> codecs,
            LinkedList<Format> inputFormats, LinkedList<Format> outputFormats,
            LinkedList<Buffer> outputBuffers) {
        outputFormat = inputFormat;
        outputBuffer = new Buffer();
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
            outputFormat = outFormat;
            outputBuffer = output;
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
        CodecIterator iterator = new CodecIterator(codecs,
                inputFormats, outputFormats, outputBuffers, null, null);
        while (iterator.hasNext()) {
            iterator.next();
            Format input = iterator.getInputFormat();
            System.err.println("Trying input format " + input);
            Format inputFormat = effect.setInputFormat(input);
            if (inputFormat != null) {
                System.err.println("Input format " + input + " works!");
                Format outputFormat = effect.setOutputFormat(inputFormat);
                Buffer output = createBuffer(outputFormat);
                iterator.insertBefore(effect, inputFormat, outputFormat,
                        output);
                if (!insertedEffects.contains(effect)) {
                    insertedEffects.add(effect);
                }
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
        return outputBuffer;
    }

    /**
     * Gets the output format
     * @return The output format
     */
    public Format getOutputFormat() {
        return outputFormat;
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
                    inputFormats, outputFormats, outputBuffers, inputBuffer,
                    inputFormat);
            int status = process(iterator, render);
            return status;
        } catch (Throwable t) {
            t.printStackTrace();
            return PlugIn.BUFFER_PROCESSED_FAILED;
        }
    }

    private int render() {
        int status = PlugIn.BUFFER_PROCESSED_OK;
        do {
            status = renderer.process(getOutputBuffer());
            if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                return status;
            }
        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        return status;
    }

    private int multiplex() {
        int status = PlugIn.BUFFER_PROCESSED_OK;
        do {
            status = multiplexer.process(getOutputBuffer(), track);
            if (status == PlugIn.BUFFER_PROCESSED_FAILED) {
                return status;
            }
        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        return status;
    }

    private void handleFormatChange(int index, Format newFormat)
            throws UnsupportedFormatException {
        HashMap<String, Boolean> searched = new HashMap<String, Boolean>();
        for (int i = 0; i < index; i++) {
            searched.put(codecs.get(i).getClass().getName(), true);
        }

        System.err.println("Changing format to " + newFormat + " converting to "
                + requestedOutputFormat);
        Codecs codecList = search(newFormat,
                requestedOutputFormat, searched);
        if (codecList == null) {
            throw new UnsupportedFormatException("Cannot translate from "
                    + newFormat + " to " + requestedOutputFormat,
                    newFormat);
        }

        Vector<Codec> existingCodecs = new Vector<Codec>();
        Vector<Format> existingInputFormats = new Vector<Format>();
        Vector<Format> existingOutputFormats = new Vector<Format>();
        Vector<Buffer> existingOutputBuffers = new Vector<Buffer>();
        ListIterator<Codec> codecIterator = codecs.listIterator();
        ListIterator<Format> inputFormatIterator = inputFormats.listIterator();
        ListIterator<Format> outputFormatIterator =
            outputFormats.listIterator();
        ListIterator<Buffer> outputBufferIterator =
            outputBuffers.listIterator();
        for (int i = 0; i < index; i++) {
            existingCodecs.add(codecIterator.next());
            existingInputFormats.add(inputFormatIterator.next());
            existingOutputBuffers.add(outputBufferIterator.next());
            Format outputFormat = outputFormatIterator.next();
            if (i < (index - 1)) {
                existingOutputFormats.add(outputFormat);
            } else {
                existingOutputFormats.add(newFormat);
            }
        }
        codecs.clear();
        inputFormats.clear();
        outputFormats.clear();
        outputBuffers.clear();
        codecs.addAll(existingCodecs);
        inputFormats.addAll(existingInputFormats);
        outputFormats.addAll(existingOutputFormats);
        outputBuffers.addAll(existingOutputBuffers);

        if (index == 0) {
            this.inputFormat = newFormat;
        }

        setupCodecs(newFormat, codecList, codecs, inputFormats, outputFormats,
                outputBuffers);
        for (Effect effect : insertedEffects) {
            insertEffect(effect);
        }
    }

    // Processes with a specific codec
    private synchronized int process(CodecIterator iterator, boolean render) {
        int status = PlugIn.BUFFER_PROCESSED_OK;

        Format inputFormat = iterator.getOutputFormat();
        Buffer buffer = iterator.getOutputBuffer();
        if (!buffer.getFormat().equals(inputFormat)) {
            try {
                int index = iterator.getIndex() + 1;
                System.err.println("Changing from " + inputFormat);
                handleFormatChange(index, buffer.getFormat());
                iterator.reset(codecs, inputFormats, outputFormats,
                        outputBuffers, index);
            } catch (UnsupportedFormatException e) {
                e.printStackTrace();
                iterator.previous();
                return PlugIn.BUFFER_PROCESSED_FAILED;
            }
        }

        if (!iterator.hasNext()) {
            outputFormat = iterator.getOutputFormat();
            outputBuffer = iterator.getOutputBuffer();

            if (!firstFrameProcessed) {
                synchronized (firstFrameSync) {
                    firstFrameProcessed = true;
                    firstFrameSync.notifyAll();
                }
            }
            if (render && (renderer != null)) {
                if (render() == PlugIn.BUFFER_PROCESSED_FAILED) {
                    return PlugIn.BUFFER_PROCESSED_FAILED;
                }
            } else if (render && (multiplexer != null)) {
                if (multiplex() == PlugIn.BUFFER_PROCESSED_FAILED) {
                    return PlugIn.BUFFER_PROCESSED_FAILED;
                }
            } else if (render && (thread != null)) {
                thread.finishedProcessing();
            }
            for (ProcessorListener listener : listeners) {
                listener.finishedProcessing(iterator.getOutputBuffer());
            }
            return status;
        }

        iterator.next();
        Buffer localInputBuffer = iterator.getInputBuffer();
        Codec codec = iterator.getCodec();
        Buffer outputBuffer = iterator.getOutputBuffer();
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
                iterator.previous();
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
                    int processStatus = process(iterator, render);
                    if (processStatus == PlugIn.BUFFER_PROCESSED_FAILED) {
                        return PlugIn.BUFFER_PROCESSED_FAILED;
                    } else if (processStatus == PlugIn.OUTPUT_BUFFER_NOT_FILLED
                            && status == PlugIn.BUFFER_PROCESSED_OK) {
                        status = PlugIn.OUTPUT_BUFFER_NOT_FILLED;
                    }
                }
            }

        } while (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED);
        iterator.previous();
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

    private Codecs findImmediateCodecs(Format input, Format output) {
        Vector< ? > codecsFromHere = null;
        codecsFromHere = PlugInManager.getPlugInList(input,
                output, PlugInManager.CODEC);
        System.err.println(input + " --> " + output);
        System.err.println("Trying immediate codecs " + codecsFromHere);
        if (!codecsFromHere.isEmpty()) {
            Codecs searchCodecs = new Codecs();
            for (int i = 0; i < codecsFromHere.size(); i++) {
                String codecClassName = (String) codecsFromHere.get(i);
                try {
                    Codec codec = (Codec) Misc.loadPlugin(codecClassName);
                    Format out = null;
                    Format in = codec.setInputFormat(input);
                    Format[] outs = codec.getSupportedOutputFormats(in);
                    for (int j = 0; (j < outs.length)
                            && (out == null); j++) {
                        if (output == null) {
                            if (input instanceof AudioFormat) {
                                if (outs[j].getEncoding().equals(
                                        AudioFormat.LINEAR)) {
                                    out = codec.setOutputFormat(outs[j]);
                                }
                            } else if (input instanceof VideoFormat) {
                                if ((outs[j] instanceof RGBFormat)
                                        || (outs[j] instanceof YUVFormat)) {
                                    out = codec.setOutputFormat(outs[j]);
                                }
                            }
                        } else if (output.matches(outs[j])) {
                            out = codec.setOutputFormat(output.intersects(
                                    outs[j]));
                        }
                    }

                    if (out != null) {
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
        return null;
    }

    private Codecs search(Format input, Format output,
            HashMap<String, Boolean> searched) {
        System.err.println("Finding codecs from " + input + " to " + output);
        if (((output == null)
                    && (input.matches(new RGBFormat())
                    || input.matches(new YUVFormat())))
                || input.matches(output)) {
            Codecs searchCodecs = new Codecs();
            return searchCodecs;
        }

        Codecs immediateCodecs = findImmediateCodecs(input, output);
        if (immediateCodecs != null) {
            return immediateCodecs;
        }

        Vector<?> codecsFromHere = PlugInManager.getPlugInList(input, null,
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
                outputFormats, outputBuffers, null, null);
        while (iter.hasNext()) {
            iter.next();
            out.println(iter.getInputFormat() + " -> " + iter.getCodec());
        }
        out.println(getOutputFormat());
    }

    public void addListener(ProcessorListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ProcessorListener listener) {
        listeners.remove(listener);
    }
}
