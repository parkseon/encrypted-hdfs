package kr.ac.cnu.networks.hadoop.crypto.aes;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class Decrypt {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length != 2) return;

        String uri = args[0];
        String dst = args[1];

        Class<?> codecClass = Class.forName("kr.ac.cnu.networks.hadoop.crypto.aes.AesParallelCryptoCodec");

        Configuration conf = new Configuration();

        CompressionCodec codec = (CompressionCodec) ReflectionUtils.newInstance(codecClass, conf);

        FileSystem fs = FileSystem.get(URI.create(uri), conf);
        InputStream in = null;

        in = fs.open(new Path(uri));
        CompressionInputStream cin = codec.createInputStream(in);
        FileOutputStream out = new FileOutputStream(dst);

        IOUtils.copyBytes(cin, out, 1048576, true);

        cin.close();
        in.close();
        out.close();
        fs.close();
    }
}
