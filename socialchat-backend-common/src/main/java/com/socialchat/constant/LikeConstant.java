package com.socialchat.constant;

public interface LikeConstant {

    Integer LIKE = 0;

    Integer DISLIKE = 1;

    // 点赞记录缓存，zset 结构，key 的结构为：like_record:targetType:targetId，
    // value 就是对应的点赞用户ID，score 是点赞时间，同时限制 redis 中 zset 的长度，这里只保存前 1000 条用户记录
    String LIKE_RECORD_KEY = "like_record:%s:%s";

    // 点赞计数缓存，string 结构，key 的结构为：like_count:targetType:targetId，value 就是对应的点赞数
    String LIKE_COUNT_KEY = "like_count:%s:%s";

    // 点赞分布式锁，string 结构，key 的结构为：like_lock:targetType:targetId，value 为唯一标识
    String LIKE_LOCK_KEY = "like_lock:%s:%s";

    Long MAX_LIKE_RECORD_LENGTH = 1000L;

    Integer MAX_LIKE_RETRIES = 3;

    Integer LIKE_THRESHOLD = 10;

    Integer POST_TYPE = 0;

    Integer COMMENT_TYPE = 1;

    Integer TIME_INTERVAL = 10;

    Integer LIKE_PAGE_SIZE = 100;

}
