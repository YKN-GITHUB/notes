package com._yk;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        zkLearn zkLearn = null;
        try {
            zkLearn = new zkLearn();
            zkLearn.connect();
            zkLearn.get2Path("/zookeeper");
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
