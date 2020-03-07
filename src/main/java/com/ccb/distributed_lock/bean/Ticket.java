package com.ccb.distributed_lock.bean;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Ticket {

    private Integer number=100;

    public void sell() {
        number--;
    }
}
