package com.ccb.distributed_lock.lock;

import com.ccb.distributed_lock.mapper.LockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Service
public class MySQLDistributedLock implements Lock {

    @Autowired
    private LockMapper lockMapper;

    /**
     * 通过递归不断尝试获取锁，获取失败后睡眠100ms后再次获取
     */
    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }
        //等待一波
        waitForLock();
        //在此尝试获取锁
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
        try {
            lockMapper.lock();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        lockMapper.unlock();
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
