package kr.ac.cnu.networks.hadoop.crypto;

import org.apache.hadoop.io.compress.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BasicParallelCryptoCodec implements SplittableCompressionCodec {
    @Override
    public CompressionOutputStream createOutputStream(OutputStream outputStream, Compressor compressor) throws IOException {
        return createOutputStream(outputStream);
    }

    @Override
    public Class<? extends Compressor> getCompressorType() {
        return BasicDummyCompressor.class;
    }

    @Override
    public Compressor createCompressor() {
        return new BasicDummyCompressor();
    }

    @Override
    public CompressionInputStream createInputStream(InputStream inputStream, Decompressor decompressor) throws IOException {
        return createInputStream(inputStream);
    }

    @Override
    public Class<? extends Decompressor> getDecompressorType() {
        return BasicDummyDecompressor.class;
    }

    @Override
    public Decompressor createDecompressor() {
        return new BasicDummyDecompressor();
    }
}
