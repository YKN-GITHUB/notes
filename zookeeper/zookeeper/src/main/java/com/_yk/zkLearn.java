package com._yk;

import org.apache.zookeeper.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class zkLearn implements Watcher {

    private ZooKeeper zk ;

    /**
     *
     */
    public void connect() throws IOException, InterruptedException, KeeperException {
        zk = new ZooKeeper("192.168.25.147:2181", 3000, this);
        get2Path("/");
    }

    public void get2Path(String path) throws InterruptedException, KeeperException {
        List<String> paths = zk.getChildren(path, false);
        println(paths, System.out::println);
    }

    public void create(String path,byte[] data) throws InterruptedException, KeeperException {
        zk.create(path, data, new ArrayList<>(), CreateMode.EPHEMERAL);
    }

    public void info2Path(){

    }

    public void close() throws InterruptedException {
        if (zk == null) return;
        zk.close();
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println(event);
    }

    public <E> void println(List<E> data, Consumer<E> outFunc) {
        for (E datum : data) {
            outFunc.accept(datum);
        }
    }
}
