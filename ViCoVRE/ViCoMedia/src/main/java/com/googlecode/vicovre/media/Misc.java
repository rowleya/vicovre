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


package com.googlecode.vicovre.media;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Vector;

import javax.media.Codec;
import javax.media.Demultiplexer;
import javax.media.Format;
import javax.media.PlugIn;
import javax.media.PlugInManager;
import javax.media.ResourceUnavailableException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.rtcp.SourceDescription;

import org.xml.sax.SAXException;

import ag3.interfaces.types.ClientProfile;

/**
 * Miscellaneous utility functions
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Misc {

    private static final Vector<String> KNOWN_CODECS = new Vector<String>();

    private static final Vector<String> KNOWN_DEMULTIPLEXERS =
        new Vector<String>();

    private static boolean codecsConfigured = false;

    private Misc() {
        // Does Nothing
    }

    /**
     * Determines if anyone has called configureCodecs yet
     * @return True if the codecs have been configured, false otherwise
     */
    public static boolean isCodecsConfigured() {
        return codecsConfigured;
    }

    /**
     * Configures the codecs from a configuration file
     * @param codecConfigFile The file listing the codecs and jars,
     *      one per line jar separated from codec by colon e.g. jar:codecClass
     * @throws IOException
     * @throws SAXException
     * @throws ResourceUnavailableException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static void configureCodecs(String codecConfigFile)
            throws IOException, SAXException {
        codecsConfigured = true;
        KNOWN_CODECS.clear();
        KNOWN_DEMULTIPLEXERS.clear();
        PlugInManager.setPlugInList(KNOWN_CODECS, PlugInManager.CODEC);
        PlugInManager.setPlugInList(KNOWN_DEMULTIPLEXERS,
                PlugInManager.DEMULTIPLEXER);
        KnownCodecsParser parser = new KnownCodecsParser(codecConfigFile);
        for (String codec : parser.getCodecs()) {
            try {
                addCodec(codec);
            } catch (Exception e) {
                System.err.println("Warning: could not load codec "
                        + codec + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        for (String demultiplexer : parser.getDemultiplexers()) {
            try {
                addDemultiplexer(demultiplexer);
            } catch (Exception e) {
                System.err.println("Warning: could not load demultiplexer "
                        + demultiplexer + ": " + e.getMessage());
            }
        }
    }

    /**
     * Adds a codec
     * @param codecClassName The class of the codec
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void addCodec(String codecClassName)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Codec codec = (Codec) loadPlugin(codecClassName);

        PlugInManager.addPlugIn(codec.getClass().getCanonicalName(),
                codec.getSupportedInputFormats(),
                codec.getSupportedOutputFormats(null),
                PlugInManager.CODEC);
    }


    /**
     * Adds a demultiplexer
     * @param demuxClassName The class of the plugin
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void addDemultiplexer(String demuxClassName)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Demultiplexer demux = (Demultiplexer) loadPlugin(demuxClassName);
        PlugInManager.addPlugIn(demux.getClass().getCanonicalName(),
                demux.getSupportedInputContentDescriptors(),
                null,
                PlugInManager.DEMULTIPLEXER);
    }

    /**
     * Adds a demultiplexer
     * @param demuxClass The class of the plugin
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void addDemultiplexer(
                Class<? extends Demultiplexer> demuxClass)
            throws InstantiationException,
            IllegalAccessException {
        Demultiplexer demux = (Demultiplexer) loadPlugin(demuxClass);
        PlugInManager.addPlugIn(demux.getClass().getCanonicalName(),
                demux.getSupportedInputContentDescriptors(),
                null,
                PlugInManager.DEMULTIPLEXER);
    }

    public static void prependDemultiplexer(
            Class<? extends Demultiplexer> demuxClass)
            throws InstantiationException,
            IllegalAccessException {
        Demultiplexer demux = (Demultiplexer) loadPlugin(demuxClass);
        Vector<String> plugins = (Vector<String>) PlugInManager.getPlugInList(
                null, null, PlugInManager.DEMULTIPLEXER);
        PlugInManager.setPlugInList(new Vector(), PlugInManager.DEMULTIPLEXER);
        PlugInManager.addPlugIn(demux.getClass().getCanonicalName(),
                demux.getSupportedInputContentDescriptors(),
                null,
                PlugInManager.DEMULTIPLEXER);
        for (String demuxerClass : plugins) {
            try {
                Demultiplexer demuxer = (Demultiplexer)
                    loadPlugin(demuxerClass);
                PlugInManager.addPlugIn(demuxerClass,
                        demuxer.getSupportedInputContentDescriptors(),
                        null, PlugInManager.DEMULTIPLEXER);
            } catch (Exception e) {
                // Do Nothing
            }

        }
    }

    /**
     * Adds a codec
     * @param codecClass The class of the codec
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void addCodec(Class< ? extends Codec> codecClass)
            throws InstantiationException, IllegalAccessException {
        Codec codec = (Codec) loadPlugin(codecClass);
        PlugInManager.addPlugIn(codec.getClass().getCanonicalName(),
                codec.getSupportedInputFormats(),
                codec.getSupportedOutputFormats(null),
                PlugInManager.CODEC);
    }



    /**
     * Loads a plugin
     * @param className The name of the plugin class
     * @return The plugin or null if there is an error
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static PlugIn loadPlugin(String className)
           throws ClassNotFoundException, InstantiationException,
           IllegalAccessException {
        Class<?> pluginClass = Class.forName(className);
        return loadPlugin(pluginClass);
    }

    /**
     * Loads a plugin
     * @param pluginClass The plugin class
     * @return The plugin or null if there is an error
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static PlugIn loadPlugin(Class<?> pluginClass)
            throws InstantiationException, IllegalAccessException {
        Object object = pluginClass.newInstance();
        if (object instanceof PlugIn) {
            PlugIn plugin = (PlugIn) object;
            return plugin;
        }
        throw new InstantiationException(
                "Class " + pluginClass + " is not a PlugIn");
    }

    /**
     * Tests a set of codecs
     * @param inputFormat The input format
     * @param codecs The codecs to test
     * @param outputFormat The output format
     * @return True if the codecs work, false otherwise
     */
    public static boolean joinCodecs(Format inputFormat,
            LinkedList<String> codecs, Format outputFormat) {
        if (codecs.isEmpty()) {
            if (inputFormat.matches(outputFormat)) {
                return true;
            }
            return false;
        }

        try {
            String codecClass = codecs.removeFirst();
            Codec codec = (Codec) Misc.loadPlugin(codecClass);
            Format input = codec.setInputFormat(inputFormat);
            if (input == null) {
                throw new Exception("Cannot set codec " + codecClass
                        + " input to " + inputFormat);
            }
            Format[] outputs = codec.getSupportedOutputFormats(input);
            for (int j = 0; j < outputs.length; j++) {
                Format output = codec.setOutputFormat(outputs[j]);
                boolean outputFound = joinCodecs(output, codecs, outputFormat);
                if (outputFound) {
                    codec.open();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SourceDescription[] createSourceDescription(
            ClientProfile profile, String note, String tool) {
        Vector<SourceDescription> sdes = new Vector<SourceDescription>();
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        String username = System.getProperty("user.name");
        sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_CNAME,
                username + "@" + hostname, 1, false));
        if (tool != null) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_TOOL,
                tool, 3, false));
        }
        if ((profile.getName() != null) && !profile.getName().equals("")) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_NAME,
                    profile.getName(), 3, false));
        }
        if ((profile.getEmail() != null) && !profile.getEmail().equals("")) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_EMAIL,
                    profile.getEmail(), 3, false));
        }
        if ((profile.getPhoneNumber() != null)
                && !profile.getPhoneNumber().equals("")) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_PHONE,
                    profile.getPhoneNumber(), 3, false));
        }
        if ((profile.getLocation() != null)
                && !profile.getLocation().equals("")) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_LOC,
                    profile.getLocation(), 3, false));
        }
        if (note != null) {
            sdes.add(new SourceDescription(SourceDescription.SOURCE_DESC_NOTE,
                    note, 3, false));
        }
        return sdes.toArray(new SourceDescription[0]);
    }

    public static Demultiplexer findDemultiplexer(DataSource ds) {
        Vector<?> demuxers = PlugInManager.getPlugInList(
                new ContentDescriptor(ds.getContentType()),
                null, PlugInManager.DEMULTIPLEXER);
        Demultiplexer demuxer = null;
        for (int i = 0; (i < demuxers.size()) && (demuxer == null); i++) {
            try {
                String demuxerClassName = (String) demuxers.get(i);
                Class<?> demuxerClass = Class.forName(demuxerClassName);
                Demultiplexer demuxerInstance =
                    (Demultiplexer) demuxerClass.newInstance();
                demuxerInstance.setSource(ds);
                demuxer = demuxerInstance;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return demuxer;
    }
}
