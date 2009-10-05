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

package com.googlecode.vicovre.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Represents a password
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Password {

    private static final byte[] NO_PASSWORD = new byte[]{
        (byte) 0xda, (byte) 0x39, (byte) 0xa3, (byte) 0xee, (byte) 0x5e,
        (byte) 0x6b, (byte) 0x4b, (byte) 0x0d, (byte) 0x32, (byte) 0x55,
        (byte) 0xbf, (byte) 0xef, (byte) 0x95, (byte) 0x60, (byte) 0x18,
        (byte) 0x90, (byte) 0xaf, (byte) 0xd8, (byte) 0x07, (byte) 0x09};

    private MessageDigest digest = null;

    private byte[] hash = NO_PASSWORD;

    /**
     * Creates a new empty Password
     * @throws NoSuchAlgorithmException
     */
    public Password() {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new Password
     * @param password the password to use to create the hash
     * @throws NoSuchAlgorithmException
     */
    public Password(String password) {
        this();
        digest.reset();
        hash = digest.digest(password.getBytes());
    }

    /**
     * Determines if this password is the same as the given string password
     * @param password The password to check
     * @return True if the hash of the password equals the stored hash
     */
    public boolean equals(String password) {
        digest.reset();
        return equals(digest.digest(password.getBytes()));
    }

    /**
     * Determines if this hash is the same as the stored hash
     * @param hash The hash to check
     * @return True if the hash is the same as the stored hash
     */
    public boolean equals(byte[] hash) {
        return Arrays.equals(this.hash, hash);
    }

    /**
     * Determines if this password is the same
     * @param password The password to check
     * @return True if the password hashes are the same
     */
    public boolean equals(Password password) {
        return equals(password.hash);
    }

    private String toHexString(byte[] bytes) {
        String hexString = "";
        for (int i = 0; i < bytes.length; i++) {
            hexString += Integer.toHexString(bytes[i] & 0xFF);
        }
        return hexString;
    }

    /**
     * Gets the hex string of the password
     * @return The hex string
     */
    public String getHashHexString() {
        return toHexString(hash);
    }

    /**
     * Sets the password hash to the given hex string
     * @param hexString The hex string to set
     */
    public void setHexStringHash(String hexString) {
        hash = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            String hexValue = "0x" + hexString.substring(i, i + 2);
            short value = Short.decode(hexValue);
            hash[i / 2] = (byte) value;
        }
    }

    /**
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getHashHexString().hashCode();
    }
}
