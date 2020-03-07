package com.ccb.distributed_lock.runnable;

import com.ccb.distributed_lock.bean.Ticket;

public class Seller implements Runnable {

    private Ticket ticket;

    public Seller(Ticket ticket) {
        this.ticket = ticket;
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        while (ticket.getNumber() != 0) {
            if (ticket.getNumber() > 0) {
                System.out.println(thread.getName() + "卖出第" + ticket.getNumber() + "张票");
                ticket.sell();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
