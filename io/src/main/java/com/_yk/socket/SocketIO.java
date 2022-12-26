package com._yk.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketIO {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
//            serverSocket = new ServerSocket(9090, 2);
            serverSocket = new ServerSocket(9090);
            // 窗口大小
            serverSocket.setReceiveBufferSize(20);
            // 当TCP握手信息还没有消失时，是否可以创建新的连接
            serverSocket.setReuseAddress(true);
            // accept 等待时间 毫秒值
            serverSocket.setSoTimeout(0);
            System.out.println("server start up in 9090");
            while (true) {

                // 阻塞等待客户端连接
                Socket client = serverSocket.accept();
                client.setKeepAlive(true);
                client.setOOBInline(true);
                client.setReceiveBufferSize(20);
                client.setReuseAddress(true);
                client.setSoLinger(true, 0);
//                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(true);

                System.out.println("client ip == >" + client.getInetAddress().getHostAddress() + ":" + client.getPort());
                // 抛出线程读取 cfd
                new Thread(() -> {
                    try {
                        InputStream input = client.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        char[] data = new char[1024];
                        while (true) {
                            int num = reader.read(data);
                            if (num > 0)
                                System.out.println("client num some data is :" + num + " val :" + new String(data, 0, num));
                            else if (num == 0)
                                System.out.println("client readed nothing!");
                            else {
                                System.out.println("client readed -1...");
                                client.close();
                                break;
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
