package com.socialchat.constant;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface CollectConstant {

    Integer COLLECT = 0;

    Integer DISCOLLECT = 1;

    // 收藏记录缓存，zset 结构，key 的结构为：collect_record:targetType:targetId，
    // value 就是对应的收藏用户ID，score 是收藏时间，同时限制 redis 中 zset 的长度，这里只保存前 1000 条用户记录
    String COLLECT_RECORD_KEY = "collect_record:%s:%s";

    // 收藏计数缓存，string 结构，key 的结构为：collect_count:targetType:targetId，value 就是对应的收藏数
    String COLLECT_COUNT_KEY = "collect_count:%s:%s";

    // 收藏分布式锁，string 结构，key 的结构为：collect_lock:targetType:targetId，value 为唯一标识
    String COLLECT_LOCK_KEY = "collect_lock:%s:%s";

    Long MAX_COLLECT_RECORD_LENGTH = 1000L;


    Integer POST_TYPE = 0;

    Integer CHAT_TYPE = 1;

    Integer MAX_COLLECT_RETRIES = 5;

    Long COLLECT_TIME_INTERVAL = 200L;

    TimeUnit TIME_UNIT = TimeUnit.MICROSECONDS;

    Duration COLLECT_LOCK_EXPIRE_TIME = Duration.ofSeconds(10);

}
