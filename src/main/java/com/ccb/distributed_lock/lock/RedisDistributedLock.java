package com.ccb.distributed_lock.lock;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 使用redis实现分布式锁，可以通过setnx命令实现，
 * 通过该命令set数据，只有不存在的key才可以set成功。
 * 但是，如果该服务在setnx后宕机了，该锁就得不到释放，
 * (之后加expire命令也不行，有可能没执行expire就宕了)
 * 此时，则可以使用set key value ex [time] nx|xx (nx参数会实现setnx的效果，默认为xx),
 * 在一条命令内完成key和过期时间的设置。
 *
 * 然而，对于高并发场景，GC会比较频繁，随GC而来的就是stop-the-world(用户线程被暂停，等待GC完成)，
 * 假设，使用redis获取的分布式锁设置了超时时间为30秒，在第29秒时，JVM开始一次Full GC，STW导致用户线程暂停，
 * 第31秒时GC完成，由于已经超过了30s，这个锁就会超时被删除，锁也会被其他线程获取，问题也会随之产生。
 * 一般来说，我们会为redis实现的分布式锁设置超时时间，业务执行完后会执行delete key的操作，问题就产生在这里，
 * 锁已经被另一个线程获取后，但是在本线程调用delete key把锁给释放了，这就会引发很多不应该出现的错误。
 *
 * 直接redis实现分布式锁非常复杂，但是官方也有相应的实现--Redisson。
 *
 * 这时，又有另一个问题，假设，我们的Redis是以集群方式运行，集群中，每个主节点都会有几个自己的从节点，
 * 数据写入主节点后，会同步复制到从节点。如果我们获取了一个锁，也就往主节点set一条数据，此时，改主节点宕机了，
 * 没来得及将这条数据复制到从节点，这条数据就丢失了，锁也就无了，其他线程又可以获取锁了。这就是没有容错性。
 *
 * 不过Redisson使用了RedLock算法解决了这个问题，其思想是给多个Reids节点都上锁，如果多数上锁成功则认为
 * 获取锁成功。
 * 算法步骤：
 *      1.获取当前时间
 *      2.按顺序依次想N个Redis节点获取锁，为确保某个节点获取失败不影响算法继续运行，获取锁还需要设置一个超时时间
 *        超时后向另一个节点获取
 *      3.计算获取锁整个过程消耗的时间，当前时间减第一步获取的时间如果小于锁的有效时间，且大多数节点都加锁成功，
 *        则认为获取锁成功，否则获取锁失败，所有加过锁的节点上的锁将会被释放
 *      4.如果获取锁成功，锁的有效时间会被重新计算(因为每个节点获取锁的时间不同)，此时 锁的有效时间=设置的有效时间-加锁消耗的时间
 *
 *然鹅，还是有问题存在的，如果有5个redis节点(编号1 2 3 4 5)，我们定义N为3，我们一个线程获取锁时向1 2 3加锁，3加锁失败，但是由于
 * 大多数成功，所以获取锁成功；此时另一个线程也要获取锁，3在此时重启成功，请求3 4 5，都获取成功，此时就会有两个线程都获取到了锁
 *
 * 总的来说： redis实现分布式锁无法真正做到安全，也无法优雅的实现释放锁后通知其他线程获取，并不是最佳方案。
 */
public class RedisDistributedLock implements Lock {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }

        waitForLock();
        lock();
    }

    /**
     * 获取不到锁后，睡眠100ms
     */
    private void waitForLock() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return redisTemplate.boundValueOps("lock").setIfAbsent("xxx", 30, TimeUnit.SECONDS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        redisTemplate.delete("lock");
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
