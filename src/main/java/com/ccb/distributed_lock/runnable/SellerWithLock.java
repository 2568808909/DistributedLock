package com.ccb.distributed_lock.runnable;

import com.ccb.distributed_lock.bean.Ticket;

import java.util.concurrent.locks.Lock;

public class SellerWithLock implements Runnable {

    private Ticket ticket;

    private Lock lock;

    public SellerWithLock(Ticket ticket, Lock lock) {
        this.ticket = ticket;
        this.lock = lock;
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        while (ticket.getNumber() != 0) {
            lock.lock();
            try {
                if (ticket.getNumber() > 0) {
                    System.out.println(thread.getName() + "卖出第" + ticket.getNumber() + "张票");
                    ticket.sell();
                }
            } finally {
                lock.unlock();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
