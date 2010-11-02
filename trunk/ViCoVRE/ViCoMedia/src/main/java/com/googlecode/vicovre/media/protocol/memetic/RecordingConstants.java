/**
 * Copyright (c) 2008-2009, University of Bristol
 * Copyright (c) 2008-2009, University of Manchester
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
package com.googlecode.vicovre.media.protocol.memetic;

import java.text.SimpleDateFormat;

/**
 * Recording constants
 * @author Andrew G D Rowley
 * @version 1.0
 */
public interface RecordingConstants {

    /**
     * The date and time format
     */
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss");

    /**
     * The name of the SAX Parser class
     */
    String SAX_PARSER = "org.apache.xerces.parsers.SAXParser";

    /**
     * The extension of the harvest data file
     */
    String HARVEST_SOURCE = ".harvest";

    /**
     * The extension of the unfinished recording
     */
    String UNFINISHED_RECORDING_INDEX = ".unfinished_rec_index";

    /**
     * The name of the index of the recording
     */
    String RECORDING_INDEX = ".rec_index";

    String RECORDING_INPROGRESS = ".rec_inprogress";

    /**
     * The name of the recording metadata
     */
    String METADATA = ".rec_metadata2";

    String OLD_METADATA = ".rec_metadata";

    String FOLDER_METADATA = ".folder_metadata";

    /**
     * The name of the recording lifetime
     */
    String LIFETIME = ".rec_lifetime";

    /**
     * The extension of the recording layout file
     */
    String LAYOUT = ".rec_layout";

    /**
     * The name of the description of a folder
     */
    String DESCRIPTION = ".description";

    /**
     * The name of a folder
     */
    String NAME = ".name";

    /**
     * The normalization factor to multiply random numbers by in ids
     */
    int ID_NORMALIZATION = 100000;

    /**
     * The postfix for the metadata of a stream file
     */
    String STREAM_METADATA = ".metadata";

    /**
     * The postfix for the index of a stream file
     */
    String STREAM_INDEX = ".index";

    /**
     * The alternative stream index postfix
     */
    String STREAM_INDEX2 = "_index";

    String ANNOTATIONS = ".annotations";

    /**
     * The id of an RTP Packet within a recording
     */
    int RTP_PACKET = 0;

    /**
     * The id of an RTCP packet within a recording
     */
    int RTCP_PACKET = 1;
}
