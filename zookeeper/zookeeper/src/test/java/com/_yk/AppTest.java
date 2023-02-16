package com._yk;

import javafx.stage.Stage;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class AppTest {
    ZooKeeper zk;

    @Before
    public void event() throws IOException, InterruptedException {
        CountDownLatch countDown = new CountDownLatch(1);
        zk = new ZooKeeper("node01:2181,node02:2181,node03:2181,node04:2181", 3000, event -> {
            /**
             * WatchedEvent state:SyncConnected type:None path:null
             * 这里只能接受到 对连接的信息，其他属性都是null，不要使用其他信息，会抛空指针异常
             * @param event
             */

                    /*
                    获取状态
                     */
            Watcher.Event.KeeperState state = event.getState();
                    /*
                    获取事件类型
                     */
            Watcher.Event.EventType type = event.getType();
            System.out.println(event);
            switch (state) {
                case Unknown:
                    break;
                case Disconnected:
                    break;
                case NoSyncConnected:
                    break;
                case SyncConnected:
                    System.out.println("connected ...");
                    countDown.countDown();
                    break;
                case AuthFailed:
                    break;
                case ConnectedReadOnly:
                    break;
                case SaslAuthenticated:
                    break;
                case Expired:
                    break;
                case Closed:
                    break;
            }
            System.out.println("default watch...");
        });

        // 阻塞等待 zk连接成功
        countDown.await();
        ZooKeeper.States state = zk.getState();
        switch (state) {
            case CONNECTING:
                System.out.println("ing ...");
                break;
            case ASSOCIATING:
                break;
            case CONNECTED:
                System.out.println("ed ...");
                break;
            case CONNECTEDREADONLY:
                break;
            case CLOSED:
                break;
            case AUTH_FAILED:
                break;
            case NOT_CONNECTED:
                break;
        }


    }


    /**
     * 创建节点时
     * 同步阻塞：
     * 如果节点已经存在会抛出异常
     * 异步回调
     * 如果节点存在 name 为null rc 为错误代码
     * 创建成功 name 会返回节点名称 rc 为0
     *
     * @throws InterruptedException
     * @throws KeeperException
     * @throws IOException
     */
    @Test
    public void testCreateZNode() throws InterruptedException, KeeperException, IOException {
        CountDownLatch countDown = new CountDownLatch(1);
//        String res = zk.create("/xxoo", "haha".getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//        System.out.println(res);
        zk.create("/oxox", "heihei".getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, (rc, path, ctx, name) -> {
            System.out.println(Arrays.asList(rc, path, ctx, name));
            countDown.countDown();
        }, "我在创建 /oxox 节点");
        // 这一步为了可以让回调输出
        countDown.await(40, TimeUnit.SECONDS);
//        TimeUnit.SECONDS.sleep(40);
    }

    @Test
    public void getData() throws InterruptedException, KeeperException {
        Stat stat = new Stat();
        System.out.println("stat create is " + stat);
        byte[] data = zk.getData("/oxox", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                // 只有针对这个节点有事件才会被吊起
                System.out.println("get data event is run... " + event + "tread name is " + Thread.currentThread().getName());
                try {
                    // 因为注册的事件只会触发一次，所以需要重复注册
                    // 为 true 时，会将默认的 watch 继续监控，default watch 就是 session 的watch
//                zk.getData("/oxox", true, stat);
                    zk.getData("/oxox", this, stat);
                } catch (KeeperException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, stat);
        System.out.println("getData is " + new String(data, StandardCharsets.UTF_8));
        int version = stat.getVersion();
        System.out.println("after get data ,stat is " + stat + " version is " + version);
        // 用于触发 事件
        Stat stat1 = zk.setData("/oxox", "new data".getBytes(StandardCharsets.UTF_8), version);
        System.out.println(Thread.currentThread().getName());
        zk.setData("/oxox", "new data2".getBytes(StandardCharsets.UTF_8), stat1.getVersion());


    }

    @Test
    public void asyncGetData() throws InterruptedException {
        zk.getData("/oxox", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                // 注册事件
                System.out.println("async event is " + event);
                try {
                    // 这里是用作重复注册事件
                    zk.getData("/oxox", this, new Stat());
                } catch (KeeperException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                // 获取到数据之后的回调

                System.out.println(Arrays.asList(rc, path, ctx, new String(data, StandardCharsets.UTF_8), stat));
                try {
                    // 这一步并没有触发 watch
                    Stat stat1 = zk.setData("/oxox", "new data".getBytes(StandardCharsets.UTF_8), stat.getVersion());
                    System.out.println(Thread.currentThread().getName());
                    zk.setData("/oxox", "new data2".getBytes(StandardCharsets.UTF_8), stat1.getVersion());
                } catch (KeeperException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "");
        TimeUnit.SECONDS.sleep(30);
    }

}
