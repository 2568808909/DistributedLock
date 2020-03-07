package com.ccb.distributed_lock.lock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * zookeeper实现分布式锁是通过其临时顺序节点实现的，第一次获取锁时，会在某个路径下创建一个临时顺序节点，
 * 判断所创建的节点是否为该路径下的第一个节点，如果是则成功获取锁。否则就监听前一个节点的状态，并阻塞该线程。
 * 当前一个节点被删除后，唤醒该线程，继续重复以上操作获取锁。
 */
public class ZookeeperDistributedLock implements Lock {

    private static final String LOCK_PATH = "/LOCK";

    private static final String ZK_HOST = "localhost:2181";
    private ZkClient zkClient = new ZkClient(ZK_HOST, 300);

    private String currentPath;
    private String beforePath;

    private volatile CountDownLatch countDownLatch;

    private Logger logger = LoggerFactory.getLogger(ZookeeperDistributedLock.class);

    public ZookeeperDistributedLock() {
        if (!zkClient.exists(LOCK_PATH)) {
            zkClient.createPersistent(LOCK_PATH);
        }
    }

    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }
        waitForLock();
        lock();
    }

    private void waitForLock() {
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                logger.info(Thread.currentThread().getName() + "捕获到DataDelete事件");
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }
        };
        countDownLatch = new CountDownLatch(1);
        zkClient.subscribeDataChanges(beforePath, listener);
        if (zkClient.exists(beforePath)) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        zkClient.unsubscribeDataChanges(beforePath, listener);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        if (StringUtils.isEmpty(currentPath)) {
            currentPath = zkClient.createEphemeralSequential(LOCK_PATH + "/", "lock");
            System.out.println("----------------------------->" + currentPath);
        }
        List<String> children = zkClient.getChildren(LOCK_PATH)
                .stream()
                .sorted()
                .collect(Collectors.toList());
        if (currentPath.equals(LOCK_PATH + "/" + children.get(0))) {
            return true;
        }
        int index = Collections.binarySearch(children, currentPath.substring(6));
        beforePath = LOCK_PATH + "/" + children.get(index - 1);
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        zkClient.delete(currentPath);
        currentPath = null;
        beforePath = null;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
