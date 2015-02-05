package org.jcodec.common;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecUtil {

    private static final VideoDecoder[] knownDecoders = new VideoDecoder[] { new H264Decoder() };

    public enum Format {
        MOV, MPEG_PS, MPEG_TS
    }

    public static VideoDecoder detectDecoder(ByteBuffer b) {
        int maxProbe = 0;
        VideoDecoder selected = null;
        for (VideoDecoder vd : knownDecoders) {
            int probe = vd.probe(b);
            if (probe > maxProbe) {
                selected = vd;
                maxProbe = probe;
            }
        }
        return selected;
    }

    public static byte[] asciiString(String fourcc) {
        char[] ch = fourcc.toCharArray();
        byte[] result = new byte[ch.length];
        for (int i = 0; i < ch.length; i++) {
            result[i] = (byte) ch[i];
        }
        return result;
    }

    public static void writeBER32(ByteBuffer buffer, int value) {
        buffer.put((byte) ((value >> 21) | 0x80));
        buffer.put((byte) ((value >> 14) | 0x80));
        buffer.put((byte) ((value >> 7) | 0x80));
        buffer.put((byte) (value & 0x7F));
    }
    
    public static void writeBER32Var(ByteBuffer bb, int value) {
        for (int i = 0, bits = MathUtil.log2(value); i < 4 && bits > 0; i++) {
            bits -= 7;
            int out = value >> bits;
            if(bits > 0)
                out |= 0x80;
            bb.put((byte)out);
        }
    }

    public static int readBER32(ByteBuffer input) {
        int size = 0;
        for (int i = 0; i < 4; i++) {
            byte b = input.get();
            size = (size << 7) | (b & 0x7f);
            if (((b & 0xff) >> 7) == 0)
                break;
        }
        return size;
    }

    public static int[] getAsIntArray(ByteBuffer yuv, int size) {
        byte[] b = new byte[size];
        int[] result = new int[size];
        yuv.get(b);
        for (int i = 0; i < b.length; i++) {
            result[i] = b[i] & 0xff;
        }
        return result;
    }

    public static ThreadPoolExecutor getPriorityExecutor(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<Runnable>(10, PriorityFuture.COMP)) {

            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityFuture<T>(newTaskFor, ((PriorityCallable<T>) callable).getPriority());
            }
        };
    }

    public static String removeExtension(String name) {
        if(name == null)
            return null;
        return name.replaceAll("\\.[^\\.]+$", "");
    }
}