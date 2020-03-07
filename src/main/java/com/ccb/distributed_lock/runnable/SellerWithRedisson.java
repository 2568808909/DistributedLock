package com.ccb.distributed_lock.runnable;

import com.ccb.distributed_lock.bean.Ticket;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;


public class SellerWithRedisson implements Runnable {

    private Ticket ticket;

    private RLock lock;

    public SellerWithRedisson(Ticket ticket, RLock lock) {
        this.ticket = ticket;
        this.lock = lock;
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        while (ticket.getNumber() != 0) {
            lock.lock(30, TimeUnit.SECONDS);
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
