package com.ccb.distributed_lock.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

public interface LockMapper {

    @Insert("insert into `lock` values(1)")
    int lock();

    @Delete("delete from `lock`")
    int unlock();
}
