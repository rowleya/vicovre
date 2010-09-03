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

package com.googlecode.vicovre.media.ui;

import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.protocol.DataSource;

public class VideoCaptureDevice {

    private String name = null;

    private Class<? extends DataSource> dataSourceClass = null;

    private DataSource[] dataSources = null;

    private String[] inputNames = null;

    private MediaLocator[] inputLocators = null;

    public VideoCaptureDevice(String name,
            Class<? extends DataSource> dataSourceClass,
            MediaLocator locator) {
        this(name, dataSourceClass, new String[0], new MediaLocator[]{locator});
    }

    public VideoCaptureDevice(String name,
            Class<? extends DataSource> dataSourceClass,
            String[] inputNames, MediaLocator[] inputLocators) {
        this.name = name;
        this.dataSourceClass = dataSourceClass;
        this.inputNames = inputNames;
        this.inputLocators = inputLocators;
        dataSources = new DataSource[inputLocators.length];
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DataSource getDataSource(int input) throws NoDataSourceException {
        if (input >= inputLocators.length) {
            throw new ArrayIndexOutOfBoundsException(input);
        }
        if (dataSources[input] == null) {
            try {
                dataSources[input] = dataSourceClass.newInstance();
                dataSources[input].setLocator(inputLocators[input]);
            } catch (Exception e) {
                throw new NoDataSourceException("Could not instantiate class "
                        + dataSourceClass);
            }
        }
        return dataSources[input];
    }

    public MediaLocator getLocator() {
        return inputLocators[0];
    }

    public String[] getInputs() {
        return inputNames;
    }
}
