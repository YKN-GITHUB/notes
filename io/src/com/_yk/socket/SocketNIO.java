package com._yk.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class SocketNIO {
    public static void main(String[] args) {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        try {

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(9090));
            ssc.configureBlocking(false); // 设置BLOCK状态，对应 OS SOCK_NONBLOCK

            while (true) {
                TimeUnit.SECONDS.sleep(1);
                SocketChannel client = ssc.accept(); // 因为是 非阻塞的 OS = -1 Java = Null
                if (client == null) {
                    System.out.println("null ---");
                } else {
                    client.configureBlocking(false); // 设置客户端连接同样不阻塞
                    System.out.println(client.getLocalAddress() + "连接了");
                    // 维持住连接 存放到list中
                    clients.add(client);
                }

                ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                for (SocketChannel c : clients) {
                    // 尝试读取
                    int num = c.read(buffer);
                    if (num > 0) {
                        buffer.flip(); // 翻转读取
                        byte[] info = new byte[buffer.limit()]; // 与缓存区一样大读取一次性
                        buffer.get(info);
                        System.out.println(c.getRemoteAddress() + " : " + new String(info));
                        buffer.clear();

                    } else if (num < 0) {
                        // 这里说明了什么
                        System.out.println("num < 0");
                    } else {
                        System.out.println("num == 0");
                    }


                }
            }


        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
