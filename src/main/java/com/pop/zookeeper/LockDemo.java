package com.pop.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @program: ZKDemo
 * @description:
 * @author: Pop
 * @create: 2019-07-18 19:33
 **/
public class LockDemo {
    /**
     * 总的来说，分布式锁的意义在于
     * 多个客户端去zk服务上创建某个相同名字的节点，由于zl
     * 服务器不允许有相同名字上的节点，那么有且只有一个节点
     * 创建成功，我们创建成功节点的那个客户端看做是获得锁
     * 的线程，其余创建节点失败的客户端将会zk上创建有序的节点
     * 最先到的所创建的序号越小，后面越大。
     * 同时每个节点将会监听他的前一个节点，例如 序号为1 的节点
     * 获得了锁，那么2 将会监听1，当1的节点释放，将会删除，表释放了锁
     * 同时因为2注册了1的节点变化事件，所以2的客户端将会得到通知，他回去尝试获得锁
     * ，当然这个时候3也会去监听2。
     * 这样可以避免惊群效应（一起哄抢锁）
     * 发现比自己小的节点删除以后，
     * 客户端会受到watcher，然后判断自己
     * 是否是最小的节点，如果是就获得锁
     * 否则就接着等待。
     * 这样每个客户端只需要监控一个节点。
     */
    private static String CONNECT_STR="192.168.255.102:2181,192.168.255.102:2182,192.168.255.102:2183";
    public static void main(String[] args) {

        /**
         * curator 对于锁这块做了封装，有InterProcessMutex这样的api
         * 也提供了以下几种实现。
         * InterProcessMutex：分布式可重入排他锁
         * InterProcessSemaphoreMutex：分布式排他锁
         * InterProcessReadWriteLock: 分布式读写锁
         *
         */

        CuratorFramework curatorFramework = null;

        curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STR)
                .sessionTimeoutMs(5000)
                .retryPolicy(new ExponentialBackoffRetry(1000,10))
                .build();

        curatorFramework.start();

        //创建锁
        final InterProcessMutex lock=
        new InterProcessMutex(curatorFramework,"/locks");

        //然后模仿多个客户端的连接。
        for(int i =0;i<10;i++){

            new Thread(()->{

                System.out.println(Thread.currentThread().getName()+"->尝试获得锁");
                try {
                    lock.acquire();
                    System.out.println(Thread.currentThread().getName()+"->获得锁成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(4000);//模仿业务处理
                    lock.release();//释放锁
                    System.out.println(Thread.currentThread().getName()+"->释放锁成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        }


    }

}

