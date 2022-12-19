package com._yk.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class SocketClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("192.168.0.105", 9090);
        while (true){
            // 获取用户输入
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String userIn = bufferedReader.readLine();
            if (userIn.equals("exit")){
                socket.close();
                break;
            }
            System.out.println("用户输入：" + userIn);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(userIn.getBytes());

        }
    }
}