# 分布式锁 #

## 锁的本质 ##

- 同一时刻，单线程执行

- 标记加锁过程，识别加锁对象

## 概述 ##

常规锁

同一个JVM下的多个Java线程的竞争

分布式锁

分布式环境下的，不同进程下的竞争

## 目标 ##
- 明确锁、分布式锁的基本特性
- 常见的分布式锁的解决思路
- 自实现一个简单版本

## 本地锁 ##

### Synchronized ###
java 关键字

### Java Lock ###

    void lock();
    void lockInterruptibly() throws InterruptedException;
    boolean tryLock();
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
    void unlock();
    Condition newCondition();

基本用法

	Lock lock = getLockInstance();
	try{
	    lock.lock();
	    doSomthing();
	}catch(Exception e){
	    log.error(e);
	}finally{
		if(Objects.nonNull(lock)){
			lock.unlock();  
		}	    
	}

## 本地锁特性 ##

- 互斥

保证每一时刻都只有一个线程执行

- 重入

某些场景需要可重入

- 阻塞

未获取到锁，是否阻塞当前线程

- 公平性

公平性和非公平性

## 分布式锁特性 ##

- 基本特性

具备本地锁的很多的基本特性

- 高可用

锁服务稳定高可用，客户端做好降级

- 高性能

性能要求比较高

- 锁超时

客户端超时后自动释放或自动续延

- 监控

监控锁的运行情况

## 技术思路 ##

|        | 数据库 | zookeeper | redis  |
|  ----  |  ---- |    ----    | ----  |
| 核心思路  | 版本号 或 唯一索引 | 时序节点 | SETNX |


## 基于Redis实现分布式锁 ##

目录结构

    com.bage.study.redis.lock
     - DistributeLock.java
     - DistributeLockBuilder.java
     - LockConfig.java
     - LuaScript.java
     - RedisDistributeLock.java
     - RedisTemplete.java

DistributeLock 接口 

	public interface DistributeLock extends java.util.concurrent.locks.Lock {
	
	}

上锁过程

    /**
     * 利用原子锁进行上锁
     * @return
     */
    public boolean tryLock() {
    
        String key = buildLockKey();
        String value = buildLockValue();
    
        return setIfNotExist(key, value, lockConfig.getExpiredSecond());
    }
    public boolean setIfNotExist(String key, String value,long expiredSeconds){
        Jedis jedis = getJedis();
        Object eval = jedis.set(key, value,"NX","EX", expiredSeconds);
        return isSuccess(eval);
    }


解锁过程

	public void unlock() {
	    String key = buildLockKey();
	    String value = buildLockValue();
	
	    List<String> keys = Arrays.asList(key);
	    List<String> args = Arrays.asList(value);
	
	    redisTemplete.exvel(LuaScript.UNLOCK_SCRIPT, keys, args);
	}

解锁LUA脚本

	public static final String UNLOCK_SCRIPT = "" +
	        "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
	        "    return redis.call(\"del\",KEYS[1])\n" +
	        "else\n" +
	        "    return 0\n" +
	        "end" +
	        "";

key 的组成

    private String buildLockKey() {
        StringBuilder sb = new StringBuilder()
                .append(lockConfig.getIp())
                .append(".")
                .append(lockConfig.getThreadId())
                .append(".")
                .append(lockConfig.getKey());
        return sb.toString();
    }

使用demo 
     
     @Test
     public void test() {
         DistributeLock lock = new DistributeLockBuilder().getLock("com.bage");
         try {
             if(lock.tryLock()){
                 // doSomething();
             }
         }catch (Exception e){
             log.error(e.getMessage(),e);
         } finally {
             if(Objects.nonNull(lock)){
                 lock.unlock();
             }
         }
     }

完整代码Github地址 [https://github.com/bage2014/study/com.bage.study.redis.lock](com.bage.study.redis.lock)

## redis 分布式锁其他实现 ##

### 百度 DLock ###
Github 链接  [https://github.com/bage2014/study/tree/master/study-redis/src/main/java/com/bage/study/redis/lock](https://github.com/bage2014/study/tree/master/study-redis/src/main/java/com/bage/study/redis/lock)

- 优点（官方说明）
 1.	原子性
 2.	由本地锁对象内部存储持有者重入次数，等于零时释放锁, 从而保证锁的可重入.
 3.	基于Redis过期机制，自动续租，既保证锁持有者有充足时间完成相应动作, 又避免持有者crash后锁不被释放的情形，提高了锁的可用性。
 4.	采用lock-free的变种CLH锁队列维护竞争线程，并由重试线程唤醒Head去竞争锁, 从而将锁竞争粒度限定在进程级, 有效避免不必要的锁竞争. 此外还实现了非公平锁，以提升吞吐量。

- 不足
基于CLH queue 释放锁，只能唤醒本机的等待队列的线程，不能唤醒其他机器的执行队列。

### Redisson ###

github地址 [https://github.com/redisson/redisson](https://github.com/redisson/redisson)

- 优点
 1.	原子性
 2.	可重入锁（Reentrant Lock）
 2.	公平锁（Fair Lock）
 3.	联锁（MultiLock）
 4.	红锁（RedLock）
 5.	读写锁（ReadWriteLock）
- 不足
 1. 厚重，复杂，有一定学习成本

## 注意点 ##

1. 加锁原子性
2. 解锁
2. 超时处理
3. redis 集群master、slave切换过程

