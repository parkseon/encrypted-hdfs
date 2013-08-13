package kr.ac.cnu.networks;

import org.apache.hadoop.io.compress.Decompressor;

import java.io.IOException;

public class AesDummyDecompressor implements Decompressor {
    @Override
    public int decompress(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void end() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean finished() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsDictionary() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean needsInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemaining() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Override
    public void setDictionary(byte[] b, int off, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        throw new UnsupportedOperationException();
    }

}
