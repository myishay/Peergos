/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package peergos.shared.io.ipfs.multibase.binary;

import java.nio.charset.Charset;

import peergos.shared.io.ipfs.multibase.CharEncoding;
import peergos.shared.io.ipfs.multibase.Charsets;

/**
 * Converts String to and from bytes using the encodings required by the Java specification. These encodings are
 * specified in <a href="http://download.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">
 * Standard charsets</a>.
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @see CharEncoding
 * @see <a href="http://download.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
 * @version $Id$
 * @since 1.4
 */
public class StringUtils {

    /**
     * <p>
     * Compares two CharSequences, returning <code>true</code> if they represent equal sequences of characters.
     * </p>
     *
     * <p>
     * <code>null</code>s are handled without exceptions. Two <code>null</code> references are considered to be equal.
     * The comparison is case sensitive.
     * </p>
     *
     * <pre>
     * StringUtils.equals(null, null)   = true
     * StringUtils.equals(null, "abc")  = false
     * StringUtils.equals("abc", null)  = false
     * StringUtils.equals("abc", "abc") = true
     * StringUtils.equals("abc", "ABC") = false
     * </pre>
     *
     * <p>
     * Copied from Apache Commons Lang r1583482 on April 10, 2014 (day of 3.3.2 release).
     * </p>
     *
     * @see Object#equals(Object)
     * @param cs1
     *            the first CharSequence, may be <code>null</code>
     * @param cs2
     *            the second CharSequence, may be <code>null</code>
     * @return <code>true</code> if the CharSequences are equal (case-sensitive), or both <code>null</code>
     * @since 1.10
     */
    public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
        if (cs1 == cs2) {
            return true;
        }
        if (cs1 == null || cs2 == null) {
            return false;
        }
        if (cs1 instanceof String && cs2 instanceof String) {
            return cs1.equals(cs2);
        }
        return cs1.length() == cs2.length() && CharSequenceUtils.regionMatches(cs1, false, 0, cs2, 0, cs1.length());
    }

    /**
     * Calls {@link String#getBytes(Charset)}
     *
     * @param string
     *            The string to encode (if null, return null).
     * @param charset
     *            The {@link Charset} to encode the <code>String</code>
     * @return the encoded bytes
     */
    private static byte[] getBytes(final String string, final Charset charset) {
        if (string == null) {
            return null;
        }
        return string.getBytes(charset);
    }

    /**
     * Encodes the given string into a sequence of bytes using the UTF-8 charset, storing the result into a new byte
     * array.
     *
     * @param string
     *            the String to encode, may be <code>null</code>
     * @return encoded bytes, or <code>null</code> if the input string was <code>null</code>
     * @throws NullPointerException
     *             Thrown if {@link Charsets#UTF_8} is not initialized, which should never happen since it is
     *             required by the Java platform specification.
     * @since As of 1.7, throws {@link NullPointerException} instead of UnsupportedEncodingException
     * @see <a href="http://download.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
     */
    public static byte[] getBytesUtf8(final String string) {
        return getBytes(string, Charset.forName("UTF-8"));
    }

    /**
     * Constructs a new <code>String</code> by decoding the specified array of bytes using the given charset.
     *
     * @param bytes
     *            The bytes to be decoded into characters
     * @param charset
     *            The {@link Charset} to encode the <code>String</code>; not {@code null}
     * @return A new <code>String</code> decoded from the specified array of bytes using the given charset,
     *         or <code>null</code> if the input byte array was <code>null</code>.
     * @throws NullPointerException
     *             Thrown if charset is {@code null}
     */
    private static String newString(final byte[] bytes, final Charset charset) {
        return bytes == null ? null : new String(bytes, charset);
    }

    /**
     * Constructs a new <code>String</code> by decoding the specified array of bytes using the US-ASCII charset.
     *
     * @param bytes
     *            The bytes to be decoded into characters
     * @return A new <code>String</code> decoded from the specified array of bytes using the US-ASCII charset,
     *         or <code>null</code> if the input byte array was <code>null</code>.
     * @throws NullPointerException
     *             Thrown if {@link Charsets#US_ASCII} is not initialized, which should never happen since it is
     *             required by the Java platform specification.
     * @since As of 1.7, throws {@link NullPointerException} instead of UnsupportedEncodingException
     */
    public static String newStringUsAscii(final byte[] bytes) {
        return newString(bytes, Charset.forName("US-ASCII"));
    }

    /**
     * Constructs a new <code>String</code> by decoding the specified array of bytes using the UTF-8 charset.
     *
     * @param bytes
     *            The bytes to be decoded into characters
     * @return A new <code>String</code> decoded from the specified array of bytes using the UTF-8 charset,
     *         or <code>null</code> if the input byte array was <code>null</code>.
     * @throws NullPointerException
     *             Thrown if {@link Charsets#UTF_8} is not initialized, which should never happen since it is
     *             required by the Java platform specification.
     * @since As of 1.7, throws {@link NullPointerException} instead of UnsupportedEncodingException
     */
    public static String newStringUtf8(final byte[] bytes) {
        return newString(bytes, Charset.forName("UTF-8"));
    }

}
