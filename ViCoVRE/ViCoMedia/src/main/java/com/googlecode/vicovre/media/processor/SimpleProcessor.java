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

    private boolean closed = false;

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
     * @param outputFormat The desired output format
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
        boolean forward = false;
        if ((outputFormat instanceof RGBFormat)
                || (outputFormat instanceof YUVFormat)
                || (outputFormat instanceof AudioFormat)
                || (!(inputFormat instanceof RGBFormat))
                || (!(inputFormat instanceof YUVFormat))) {
            codecList = search(inputFormat, outputFormat, searched, true);
            forward = true;
        } else {
            codecList = search(inputFormat, outputFormat, searched, false);
            forward = false;
        }
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
            if (forward) {
                codecs.addLast(codec.next());
                inputFormats.addLast(inFormat);
                outputFormats.addLast(outFormat);
                outputBuffers.addLast(output);
            } else {
                codecs.addFirst(codec.next());
                inputFormats.addFirst(inFormat);
                outputFormats.addFirst(outFormat);
                outputBuffers.addFirst(output);
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
        do {

            long lastSequence = outputBuffer.getSequenceNumber();
            outputBuffer.setTimeStamp(0);
            outputBuffer.setOffset(0);
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
                    outputBuffer.setTimeStamp(
                            localInputBuffer.getTimeStamp());
                }
                if (!outputBuffer.isDiscard()
                        && !outputBuffer.isEOM()) {
                    if (iterator.hasNext()) {
                        iterator.next();
                        if (process(iterator, render)
                                == PlugIn.BUFFER_PROCESSED_FAILED) {
                            iterator.previous();
                            return PlugIn.BUFFER_PROCESSED_FAILED;
                        }
                        iterator.previous();
                    } else if (render && (renderer != null)
                            && ((status == PlugIn.BUFFER_PROCESSED_OK)
                            || (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                        if (render(iterator)
                                == PlugIn.BUFFER_PROCESSED_FAILED) {
                            return PlugIn.BUFFER_PROCESSED_FAILED;
                        }
                    } else if (render && (multiplexer != null)
                            && ((status == PlugIn.BUFFER_PROCESSED_OK)
                            || (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                        return multiplex();
                    } else if (render && (thread != null)
                            && ((status == PlugIn.BUFFER_PROCESSED_OK)
                            || (status == PlugIn.INPUT_BUFFER_NOT_CONSUMED))) {
                        thread.finishedProcessing();
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
            HashMap<String, Boolean> searched, boolean forward) {
        if (input.matches(output)) {
            Codecs searchCodecs = new Codecs();
            searchCodecs.codecList.addFirst(new CopyCodec());
            searchCodecs.inputFormatList.addFirst(input);
            searchCodecs.outputFormatList.addFirst(input);
            return searchCodecs;
        }
        Vector< ? > codecsFromHere = PlugInManager.getPlugInList(
                input, output, PlugInManager.CODEC);
        if (!codecsFromHere.isEmpty()) {
            System.err.println("Trying immediate codecs " + codecsFromHere);
            Codecs searchCodecs = new Codecs();
            for (int i = 0; i < codecsFromHere.size(); i++) {
                String codecClassName = (String) codecsFromHere.get(i);
                try {
                    Codec codec = (Codec) Misc.loadPlugin(codecClassName);
                    int matched = -1;
                    Format in = null;
                    Format out = null;
                    if (forward) {
                        in = codec.setInputFormat(input);
                        Format[] outs = codec.getSupportedOutputFormats(in);
                        for (int j = 0; (j < outs.length)
                                && (matched == -1); j++) {
                            if (output.matches(outs[j])) {
                                out = codec.setOutputFormat(output.intersects(
                                        outs[j]));
                                matched = j;
                            }
                        }
                    } else {
                        out = codec.setOutputFormat(output);
                        Format[] ins = codec.getSupportedInputFormats();
                        for (int j = 0; (j < ins.length)
                                && (matched == -1); j++) {
                            if (input.matches(ins[j])) {
                                Format inF = codec.setInputFormat(input);
                                if (inF != null) {
                                    Format[] outs =
                                        codec.getSupportedOutputFormats(inF);
                                    boolean ok = false;
                                    for (int k = 0; (k < outs.length)
                                            && !ok; k++) {
                                        if (out.matches(outs[k])) {
                                            ok = true;
                                        }
                                    }
                                    if (ok) {
                                        codec.setInputFormat(ins[j]);
                                        in = ins[j];
                                        matched = j;
                                    }
                                }
                            }
                        }
                    }
                    if (matched != -1) {
                        codec.open();
                        searchCodecs.codecList.addFirst(codec);
                        searchCodecs.inputFormatList.addFirst(in);
                        searchCodecs.outputFormatList.addFirst(out);
                        return searchCodecs;
                    }
                } catch (Exception e) {
                    System.err.println("Warning: " + e.getMessage());
                }
            }
        }
        if (forward) {
            codecsFromHere = PlugInManager.getPlugInList(input, null,
                    PlugInManager.CODEC);
        } else {
            codecsFromHere = PlugInManager.getPlugInList(null, output,
                    PlugInManager.CODEC);
        }
        System.err.println("Trying codecs " + codecsFromHere);
        for (int i = 0; i < codecsFromHere.size(); i++) {
            String codecClassName = (String) codecsFromHere.get(i);
            if (!searched.containsKey(codecClassName)) {
                searched.put(codecClassName, true);
                try {
                    Codec codec = (Codec) Misc.loadPlugin(codecClassName);
                    Format[] formats = null;
                    if (forward) {
                        codec.setInputFormat(input);
                        formats = codec.getSupportedOutputFormats(input);
                    } else {
                        codec.setOutputFormat(output);
                        Vector<Format> fmts = new Vector<Format>();
                        formats = codec.getSupportedInputFormats();
                        for (int j = 0; j < formats.length; j++) {
                            Format[] outs = codec.getSupportedOutputFormats(
                                    formats[j]);
                            boolean ok = false;
                            for (int k = 0; (k < outs.length) && !ok; k++) {
                                if (output.matches(outs[k])) {
                                    fmts.add(formats[j]);
                                    ok = true;
                                }
                            }
                        }
                        formats = fmts.toArray(new Format[0]);
                    }
                    for (int j = 0; j < formats.length; j++) {
                        Format fmt = null;
                        if (forward) {
                            fmt = codec.setOutputFormat(formats[j]);
                        } else {
                            fmt = codec.setInputFormat(formats[j]);
                        }
                        Codecs searchCodecs = null;
                            if (forward) {
                                searchCodecs = search(fmt, output, searched,
                                        forward);
                            } else {
                                searchCodecs = search(input, fmt, searched,
                                        forward);
                            }
                        if (searchCodecs != null) {
                            codec.open();
                            if (forward) {
                                searchCodecs.codecList.addFirst(codec);
                                searchCodecs.inputFormatList.addFirst(input);
                                searchCodecs.outputFormatList.addFirst(fmt);
                            } else {
                                searchCodecs.codecList.addFirst(codec);
                                searchCodecs.inputFormatList.addFirst(fmt);
                                searchCodecs.outputFormatList.addFirst(output);
                            }
                            return searchCodecs;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
}
