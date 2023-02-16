package com._yk;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) throws IOException {

        Process ipconfig = Runtime.getRuntime().exec("java");
        InputStream inputStream = ipconfig.getInputStream();
        byte[] bytes = new byte[1024];
        String s = new String();
        while (inputStream.read(bytes) != -1) s += new String(bytes,"GBK");
        System.out.println(s);

//        System.out.println("===========================");
//        System.out.println(RuntimeUtil.execForStr("ipconfig"));
    }
}
