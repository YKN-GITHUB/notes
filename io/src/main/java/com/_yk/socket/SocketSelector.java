package com._yk.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class SocketSelector {


    ServerSocketChannel ssc;

    Selector selector;

    ExecutorService pool;

    AtomicInteger num = new AtomicInteger(0);


    LinkedBlockingQueue<BiConsumer<Selector, SelectionKey>> queue;

    public void open(int port) {
        try {
//            pool = Executors.newFixedThreadPool(4);
//            queue = new LinkedBlockingQueue<>();
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.bind(new InetSocketAddress(port));
            // 如果在epoll模型下，这里完成了 epoll_create => epfd
            selector = Selector.open();// 在Linux中优先选择epoll 但可以 -D 修正

            // 接受 accept 事件 epoll_ctl(epfd,ADD,fd,IN)
            ssc.register(selector, SelectionKey.OP_ACCEPT);
           /* for (int i = 0; i < 4; i++) {
                pool.submit(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 我收到 ： ");
                        queue.take().accept(selector, null);

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }*/
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {

            System.out.println("服务器启动");

            while (true) {
//                Set<SelectionKey> keys = selector.keys();
//                System.out.println("key size：" + keys.size());
                // epoll_wait(epfd)
                /*

                懒加载：
                     其实在触碰到 selector.select 才触发了epoll_ctl调用
                */
                while (selector.select() > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey next = iterator.next();
                        iterator.remove();
                        if (next.isValid() && next.isAcceptable()) {
                            acceptHandler(selector, next);
                        } else if (next.isReadable()) {
                            /*
                             * 如果使用多线程：
                             *   read事件抛到其他线程进行，当前线程继续执行，当前的selector多路复用器
                             *   会再次收到read事件到达了，而其他线程还没有处理read事件
                             * */
                            next.cancel();
                            readHandler(selector, next);
                          /*  queue.put(new BiConsumer<Selector, SelectionKey>() {
                                SelectionKey k = next;

                                @Override
                                public void accept(Selector selector, SelectionKey selectionKey) {
                                    SocketSelector.readHandler(selector, k);
                                }
                            });*/
                        } else if (next.isWritable()) {
                            // 可写 注册写事件，只要 send-Q 队列不阻塞就一直发送
//                            SocketChannel write = (SocketChannel) next.channel();
//                            ByteBuffer buffer = (ByteBuffer) next.attachment();
//                            buffer.flip();
//                            write.write(buffer);
                        }
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        SocketSelector server = new SocketSelector();
        server.open(9090);
        server.start();
    }

    private static void readHandler(Selector selector, SelectionKey next) {
        // 线性执行转变为 selector 还会阻塞
        new Thread(() -> {
            // 可读
            SocketChannel nsc = (SocketChannel) next.channel();
            ByteBuffer buffer = (ByteBuffer) next.attachment();


            buffer.clear();
            try {

                int read = 0;
                while (true) {
                    read = nsc.read(buffer);
                    // TODO
                    next.interestOps(SelectionKey.OP_READ);
                    if (read > 0) {
                        buffer.flip();
                        byte[] data = new byte[buffer.limit()];
                        buffer.get(data);
                        System.out.println(Thread.currentThread().getName() + " : " + new String(data));
                        buffer.clear();
                        /*nsc.register(selector, SelectionKey.OP_WRITE, buffer);

                        buffer.put("我收到了".getBytes());
                        buffer.flip();
                        nsc.write(buffer);
                        buffer.clear();*/
                        break;
                    } else if (read == 0) {
                        break;
                    } else {
                        next.cancel();
                        nsc.close();
                        System.out.println("client is close");
                        break;
                    }


                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void acceptHandler(Selector selector, SelectionKey next) throws IOException {
        // 连接事件到达
        ServerSocketChannel nssc = (ServerSocketChannel) next.channel();
        // 需要使用服务端接受
        SocketChannel client = nssc.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocateDirect(4096));
    }
}
