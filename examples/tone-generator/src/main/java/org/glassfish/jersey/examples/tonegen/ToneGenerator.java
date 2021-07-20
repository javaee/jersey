/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.examples.tonegen;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generate DTMF tone waves and create a wave file.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class ToneGenerator {

    private static final float SAMPLE_RATE = 44100;
    private static final int MAX_AMPLITUDE = 60;

    /**
     * Create a temporary file containing a DTMF tone in a .wav format.
     *
     * @param toneSequence    {@link String} representing a desired DTMF sequence. Valid characters are digits (0-9),
     *                        the hash sign (#) and star (*). Invalid characters are ignored.
     * @param toneDuration    duration of a single generated DTMF tone (represented by one character in toneSequence) in
     *                        milliseconds
     * @param silenceDuration duration of silence between two generated tones in milliseconds
     * @return name of the generated temporary file on the server-side
     * @throws LineUnavailableException
     * @throws IOException
     */
    public static String generate(String toneSequence, int toneDuration, int silenceDuration) throws LineUnavailableException,
            IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        for (char toneValue : toneSequence.toCharArray()) {
            DTMFTone tone = DTMFTone.forValue(toneValue);
            if (tone == null) {
                continue;
            }
            int loFreq = tone.getLoFreq();
            int hiFreq = tone.getHiFreq();
            byte[] bufLo = generateWaveForm(loFreq, MAX_AMPLITUDE, toneDuration);
            byte[] bufHi = generateWaveForm(hiFreq, MAX_AMPLITUDE, toneDuration);
            for (int i = 0; i < bufLo.length; i++) {
                bufLo[i] = Integer.valueOf(bufLo[i] + bufHi[i]).byteValue();
            }
            outStream.write(bufLo);
            outStream.write(generateSilence(silenceDuration));
        }
        outStream.flush();

        byte[] output = outStream.toByteArray();
        InputStream b_in = new ByteArrayInputStream(output);
        return writeWav(b_in, output.length);

    }

    /**
     * Generate the actual sinus wave of one simple tone.
     *
     * @param frequency frequency in Herz [Hz]
     * @param volume    maximum amplitude (note, that clipping/overflow may occur during mixing, if amplitude is too high)
     * @param duration  duration of the tone in milliseconds
     * @return byte array representing the tone waveform
     */
    private static byte[] generateWaveForm(int frequency, int volume, int duration) {
        byte[] buffer = new byte[Float.valueOf(duration * (SAMPLE_RATE / 1000) + 1).intValue()];
        for (int i = 0; i < duration * SAMPLE_RATE / 1000; i++) {
            double alpha = i / (SAMPLE_RATE / frequency) * 2.0 * Math.PI;
            buffer[i] = (byte) (Math.sin(alpha) * volume);
        }
        return buffer;
    }

    /**
     * Generates a "waveform" for silence.
     *
     * @param duration duration of the silence
     * @return byte array representing silence "waveform"
     */
    private static byte[] generateSilence(int duration) {
        byte buffer[] = new byte[Float.valueOf(duration * (SAMPLE_RATE / 1000) + 1).intValue()];
        // all the bytes are initialized with zeros
        return buffer;
    }

    /**
     * Writes the temporary file with the generated audio.
     *
     * @param inputStream input stream with the waveform
     * @param length      length of the waveform
     * @return name of the generated temporary file
     * @throws IOException
     */
    private static String writeWav(InputStream inputStream, int length) throws IOException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 1, 1, SAMPLE_RATE, false);
        File file = File.createTempFile("wav", ".");
        AudioSystem.write(new AudioInputStream(inputStream, format, length), AudioFileFormat.Type.WAVE, file);
        return file.getAbsolutePath();
    }
}
