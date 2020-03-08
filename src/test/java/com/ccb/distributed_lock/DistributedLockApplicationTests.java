package com.ccb.distributed_lock;

import com.ccb.distributed_lock.bean.Ticket;
import com.ccb.distributed_lock.lock.ZookeeperDistributedLock;
import com.ccb.distributed_lock.runnable.Seller;
import com.ccb.distributed_lock.runnable.SellerWithLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//@SpringBootTest
class DistributedLockApplicationTests {

    private volatile Ticket ticket = new Ticket();

    @Autowired
    private Lock mySQLock;  //注入MySQLDistributedLock

    private Lock zkLock= new ZookeeperDistributedLock();

    /**
     * 不加锁的情况，volatile并不能保证原子性，在售票过程中，同一张票可能会被卖出多次，且可能会出现超卖
     *
     * @throws IOException
     */
    @Test
    void sellTicket() throws IOException {
        Thread thread = new Thread(new Seller(ticket));
        Thread thread1 = new Thread(new Seller(ticket));
        Thread thread2 = new Thread(new Seller(ticket));
        Thread thread3 = new Thread(new Seller(ticket));
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        System.in.read();
    }

    /**
     * 使用加锁的方式，使每个线程对共享资源互斥访问，就不会有一张票被卖出多次或者出现超卖的状况
     * 但这只能控制单个JVM进程内的共享资源访问，对于分布式系统来说，可能会有多个服务进程访问redis，
     * MySQL内的公有数据，并对他们进行修改，此时，如果要做互斥访问就要使用分布式锁来实现了
     * @throws IOException
     */
    @Test
    void sellTicketWithLock() throws IOException {
        Lock lock = new ReentrantLock();
        Thread thread = new Thread(new SellerWithLock(ticket, lock));
        Thread thread1 = new Thread(new SellerWithLock(ticket, lock));
        Thread thread2 = new Thread(new SellerWithLock(ticket, lock));
        Thread thread3 = new Thread(new SellerWithLock(ticket, lock));
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        System.in.read();
    }

    /**
     * 使用MySQL实现分布式锁
     * 实现方式：建一个只有主键的表，因主键不能重复，可以利用这一点来实现分布式锁，
     * 加锁就是对数据库插入一条数据，如果重复就会抛出异常，也就说明锁已经被占用。
     * 当使用完成后，释放锁，只需要删除该条记录即可。
     *
     * 缺点：效率低，获取锁失败后，等待锁的实现不够优雅(获取锁失败后只能不断的睡眠->唤醒->重新获取锁)
     * @throws IOException
     */
    @Test
    void sellTicketWithMySQLDistributedLock() throws IOException {
        Thread thread = new Thread(new SellerWithLock(ticket, mySQLock));
        Thread thread1 = new Thread(new SellerWithLock(ticket, mySQLock));
        Thread thread2 = new Thread(new SellerWithLock(ticket, mySQLock));
        Thread thread3 = new Thread(new SellerWithLock(ticket, mySQLock));
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        System.in.read();
    }

    @Test
    void sellTicketWithZKDistributedLock() throws IOException {
        Thread thread = new Thread(new SellerWithLock(ticket, new ZookeeperDistributedLock()));
        Thread thread1 = new Thread(new SellerWithLock(ticket, new ZookeeperDistributedLock()));
        Thread thread2 = new Thread(new SellerWithLock(ticket, new ZookeeperDistributedLock()));
        Thread thread3 = new Thread(new SellerWithLock(ticket, new ZookeeperDistributedLock()));
        thread.start();
        thread1.start();
        thread2.start();
        thread3.start();
        System.in.read();
    }

//    @Test
//    public void testLock() {
//        lock.unlock();
//    }

}
