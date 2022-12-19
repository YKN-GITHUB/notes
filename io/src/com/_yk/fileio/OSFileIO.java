package com._yk.fileio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OSFileIO {

    private static final byte[] data = "123456789\n".getBytes();
    private static final String path = "/root/out.txt";

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "0":
                testBasicFileIO();
            case "1":
                testBufferFileIO();
            case "2":
                testRandomFileIO();
            default:

        }
    }

    private static void testRandomFileIO() throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File(path), "rw");
        raf.write("hello world!\n".getBytes());
        raf.write("hello bigdata!\n".getBytes());

        raf.seek(4); // hell
        System.out.println("write ================");
        System.in.read();

        // 获取 FileChannel
        FileChannel channel = raf.getChannel();
        MappedByteBuffer mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);

        // 这里没有系统调用，直接写到了kernel的pagecache上
        mmap.put("@@@@".getBytes());

        // flush 刷新数据到磁盘
//        mmap.force();

        // 如果不重置指针，channel.read就会从 RandomAccessFile 的指针位置开始向buffer中写入，剩余位置用0填充
        raf.seek(0);
        mmap.put("ooxx".getBytes());

        ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
        // 这里可以直接读完
        int read = channel.read(buffer);

        // 翻转读写
        buffer.flip();
        int limit = buffer.limit();
        int sunZero = 0;
        for (int i = 0; i < limit; i++) {
            if (buffer.get(i) != 0)
                System.out.print((char) buffer.get(i));
            else
                sunZero++;

        }
        System.out.println("limit is " + limit + "read is " + read + " sunZero is " + sunZero);

//        FileChannel.open()
    }

    private static void whatByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
//        ByteBuffer.allocateDirect(1024);
        System.out.println(buffer.position()); // 开始位置
        System.out.println(buffer.limit()); // 总大小
        System.out.println(buffer.capacity()); // 总容量
        System.out.println(buffer);
        buffer.put(data); // 想缓存添加数据
        buffer.flip(); // 翻转读写行为交替
        buffer.get(); // 获取一个字节
        buffer.compact(); // 整理缓存空间
        buffer.clear(); // 清除缓存
    }


    /**
     * 直接写 FileOutputStream 没write一次调用一次 syscall write()
     *
     * @throws Exception
     */
    private static void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while (true) {
            Thread.sleep(3000);
            out.write(data);
        }

    }

    /**
     * JVM 内部存在一个 8 byte[] 的数组
     * 当写满JVM内部的 buffer之后 JVM 调用一次syscall
     *
     * @throws Exception
     */
    private static void testBufferFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        while (true) {
            Thread.sleep(3000);
            out.write(data);
        }
    }


}