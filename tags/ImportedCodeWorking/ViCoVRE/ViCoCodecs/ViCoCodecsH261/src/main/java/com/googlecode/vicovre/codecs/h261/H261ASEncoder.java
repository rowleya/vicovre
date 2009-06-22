/**
 * Copyright (c) 2009, University of Manchester
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
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
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


package com.googlecode.vicovre.codecs.h261;

import javax.media.Buffer;
import javax.media.format.YUVFormat;

import com.googlecode.vicovre.codecs.utils.BitOutputStream;
import com.googlecode.vicovre.codecs.utils.QuickArrayException;

/**
 * An encoder of H.261AS video
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class H261ASEncoder extends H261AbstractEncoder {

    // The number of the frame in this sequence
    private long sequencenumber = 0;

    /**
     * Creates a new H261AS Encoder
     * @throws QuickArrayException
     *
     */
    public H261ASEncoder() throws QuickArrayException {
        super("h261as/rtp", new YUVFormat(YUVFormat.YUV_420));
    }

    protected void finishBuffers(Buffer output, BitOutputStream outputdata,
            int startMquant, long timestamp) throws QuickArrayException {
        // Finish this packet
        output.setFormat(getOutputFormat());
        int ebit = outputdata.flush();

        // Generate the header
        BitOutputStream header = new BitOutputStream((byte[]) output.getData(),
                output.getOffset());
        header.add(ebit, H261Constants.END_PADDING_BITS); // EBIT
        header.add(startMquant, H261Constants.QUANT_BITS); // QUANT
        header.add((getWidth() >> 4) - 1, 12); // WIDTH
        header.add((getHeight() >> 4) - 1, 12); // HEIGHT

        header.flush();

        output.setSequenceNumber(sequencenumber++);
        output.setTimeStamp(timestamp);
        output.setOffset(0);
        output.setLength(outputdata.getLength());
        output.setDiscard(false);
    }

    /**
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "H.261AS RTP Encoder";
    }
}
