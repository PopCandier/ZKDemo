package com.pop.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * @author Pop
 * @date 2019/7/13 14:48
 */
public class CuratorDemo {
    //集群环境，由于我这里一一台虚拟机配置了三个zk节点，如果是三台虚拟机，ip地址可能不同
    private static String CONNECT_STR="192.168.255.102:2181,192.168.255.102:2182,192.168.255.102:2183";
        //,192.168.255.102:2182,192.168.255.102:2183
    public static void main(String[] args) throws Exception {

//        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient()
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STR).sessionTimeoutMs(5000)//会话超时时间
                .retryPolicy(new ExponentialBackoffRetry(100,3)).//重试策略
                        build();
        //ExponentialBackoffRetry 衰减重试
        //RetryOneTime 只试一次
        //RetryUntilElapsed
        //RetryNTimes 在限定时间内重试


        //表示如果因为连接网络波动，或者失败，将会休息1秒，然后重试，一共会重试3次，每次都会递增，例如第一次是100，第二次
        //可能就是200
        curatorFramework.start();//启动

//        createData(curatorFramework);
//            updateData(curatorFramework);
        deleteData(curatorFramework);
        //操作，crud
//        curatorFramework.create();//增加
//        curatorFramework.setData();//修改
//        curatorFramework.delete();//删除
//        curatorFramework.getData();//查询
    }


    private static void createData(CuratorFramework curatorFramework) throws Exception {

        curatorFramework.create().
                creatingParentsIfNeeded()//由于我们直接增加了子节点和父节点
                //所以可以添加这个来保证整个路径天际了
                .withMode(CreateMode.PERSISTENT)
                /*节点的属性，这里可以设置 临时节点，临时有序节点
                持久节点，临时持久节点
                同时，临时节点不允许有父节点
                * */
                .forPath("/data/program","test".getBytes());

    }

    private static void updateData(CuratorFramework curatorFramework) throws Exception {
        curatorFramework.setData().forPath("/data/program","up".getBytes());
    }

    private static void deleteData(CuratorFramework curatorFramework) throws Exception {

        Stat stat = new Stat();
        /**
         * 类似于先获得版本号，在去进行删除
         * 下面这个方法执行后，stat将会获得值
         */
        String value=new String(curatorFramework.getData().storingStatIn(stat).forPath("/data/program"));
        //删除注意版本号的，保证原子性
        curatorFramework.delete().
                withVersion(stat.getVersion())//按照版本，很重要
                .forPath("/data/program");
    }
}
