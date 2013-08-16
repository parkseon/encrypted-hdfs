package kr.ac.cnu.networks.hadoop.crypto.aes;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.util.ReflectionUtils;

import java.io.*;
import java.net.URI;

public class Encrypt {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if(args.length != 2) return;

        String localSrc = args[0];
        String dst = args[1];

        InputStream in = new BufferedInputStream(new FileInputStream(localSrc));

        Configuration conf = new Configuration();
        Class<?> codecClass = Class.forName("kr.ac.cnu.networks.hadoop.crypto.aes.AesParallelCryptoCodec");
        CompressionCodec codec = (CompressionCodec)ReflectionUtils.newInstance(codecClass, conf);


        FileSystem fs = FileSystem.get(URI.create(dst), conf);
        OutputStream out = fs.create(new Path(dst));
        CompressionOutputStream cout = codec.createOutputStream(out);

        IOUtils.copyBytes(in,  cout,  1048576, true);

        cout.flush();
        cout.close();
        in.close();
        out.close();
        fs.close();
    }
}
