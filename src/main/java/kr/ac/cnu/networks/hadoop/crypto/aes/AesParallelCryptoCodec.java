package kr.ac.cnu.networks.hadoop.crypto.aes;

import kr.ac.cnu.networks.hadoop.crypto.BasicParallelCryptoCodec;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AesParallelCryptoCodec extends BasicParallelCryptoCodec {
    @Override
    public String getDefaultExtension() {
        return ".haes";
    }

    @Override
    public CompressionOutputStream createOutputStream(OutputStream outputStream) throws IOException {
        return new AesCryptoOutputStream(outputStream);
    }

    @Override
    public CompressionInputStream createInputStream(InputStream inputStream) throws IOException {
        return new AesCryptoInputStream(inputStream);
    }

    @Override
    public SplitCompressionInputStream createInputStream(InputStream seekableIn, Decompressor decompressor, long start, long end, READ_MODE read_mode) throws IOException {
        if (!(seekableIn instanceof Seekable)) {
            throw new IOException("InputStream must be an instance of " + Seekable.class.getName());
        }

        long adjStart = start;

        ((Seekable)seekableIn).seek(adjStart);

        SplitCompressionInputStream in = new AesCryptoInputStream(seekableIn, start, end, read_mode);

        return in;
    }
}
