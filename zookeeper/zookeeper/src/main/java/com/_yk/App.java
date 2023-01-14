package com._yk;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        zkLearn zkLearn = null;
        try {
            zkLearn = new zkLearn();
//            zkLearn.create("/test", "".getBytes(StandardCharsets.UTF_8));
            System.out.println("/zookeeper ||== >");
            zkLearn.get2Path("/zookeeper");
            System.out.println("all == >");
            zkLearn.connect();
        } catch (IOException | InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        } finally {
            if (zkLearn != null) {
                try {
                    zkLearn.close();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
