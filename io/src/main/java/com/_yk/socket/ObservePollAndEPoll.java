package com._yk.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class ObservePollAndEPoll {
    /*
     * poll
     * -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
     * 仅仅只是在调用 poll(fds)函数
     *
     * epoll
     *
     */
    public static void main(String[] args) {

        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(9090));
            ssc.configureBlocking(false);
            // 设置reuse
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);

            // 开启多路复用器
            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                while (selector.select() > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey next = iterator.next();
                        iterator.remove();
                        if (next.isAcceptable()) {
                            acceptHandler(next, selector);
                        } else if (next.isReadable()) {
                            readHandler(next);
                        }
                    }
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void acceptHandler(SelectionKey next, Selector selector) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) next.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            client.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(8192));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void readHandler(SelectionKey next) {
        SocketChannel client = (SocketChannel) next.channel();
        ByteBuffer buffer = (ByteBuffer) next.attachment();
        buffer.clear();
        try {
            while (true) {

                int read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}