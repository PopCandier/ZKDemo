package com.pop.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author Pop
 * @date 2019/7/14 23:04
 */
public class WatcherDemo {
    private static String CONNECT_STR="192.168.255.102:2181,192.168.255.102:2182,192.168.255.102:2183";
    public static void main(String[] args) throws Exception {
        /**
         * PathChildCache 针对子节点的创建，删除和更新触发事件
         *
         * NodeCache 针对当前节点的变化触发事件
         * TreeCache 综合事件 以上都包含
         */
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STR).sessionTimeoutMs(5000)//会话超时时间
                .retryPolicy(new ExponentialBackoffRetry(100, 3)).//重试策略
                build();

        curatorFramework.start();

//        addListenerWithNode(curatorFramework);
//        addListenerWithChild(curatorFramework);
        addListenerWithAll(curatorFramework);
        System.in.read();//不让他死亡
    }

    /**
     * 只针对当前节点的监听，但是不对子节点监听
     *
     *
     * 使用的时候
     * java.lang.NoSuchMethodError: org.apache.curator.framework.CuratorFramework.newWatcherRemoveCuratorFramework()Lorg/apache/curator/framework/WatcherRemoveCuratorFramework;
     * 原因是版本太高了
     *
     * 可以对crud进行反馈
     * 只要你的名字符合你设置的路径，在这里是/watch 都会有事件
     * @param curatorFramework
     */
    private static void addListenerWithNode(CuratorFramework curatorFramework) throws Exception {

        NodeCache nodeCache = new NodeCache(curatorFramework,
                "/watch",false);
        //增加事件监听，里面是回调
        NodeCacheListener nodeCacheListener =()->{
            System.out.println("接受到事件");
            System.out.println(nodeCache.getCurrentData().getPath()+"---"+
                    new String( nodeCache.getCurrentData().getData()));
        };

        nodeCache.getListenable().addListener(nodeCacheListener);
        nodeCache.start();

    }

    /**
     * 只对设置的watch 节点下的子节点都会有事件
     * @param curatorFramework
     * @throws Exception
     */
    private static void addListenerWithChild(CuratorFramework curatorFramework) throws Exception {

        PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorFramework,
                "/watch",true);
        /**
         * 我发现，如果这里cacheData不设置为true，仿佛不会触发事件？
         * 然后，如果父节点被删除，也就是、/watch被删除，如果再次创建，貌似不会再生效了
         */
        //增加事件监听，里面是回调
        PathChildrenCacheListener nodeCacheListener =(curatorFramework1,pathChildrenCacheEvent)->{
            //得到事件类型，还有修改的节点数据
            System.out.println(pathChildrenCacheEvent.getType()
                    +"->"+
                    new String(pathChildrenCacheEvent.getData().getData()));
            /**
             * 这里传进的curatorFramework1，还有更具本华进行操作
             */

        };

        pathChildrenCache.getListenable().addListener(nodeCacheListener);
        pathChildrenCache.start(PathChildrenCache.StartMode.NORMAL);

    }

    /**
     * 会对父子节点都会进行监听，包括后续的添加子节点的子节点
     * @param curatorFramework
     * s说明一下，就是2.5.0就是我之前用的版本，貌似不支持Treenode这个东西，
     * 但是到4.0就开支持，但是可能在低版本也可以支持吧，只是我么你测试
     * @throws Exception
     */
    private static void addListenerWithAll(CuratorFramework curatorFramework) throws Exception {

        TreeCache treeCache = new TreeCache(curatorFramework,
                "/watch");
        /**
         * 我发现，如果这里cacheData不设置为true，仿佛不会触发事件？
         * 然后，如果父节点被删除，也就是、/watch被删除，如果再次创建，貌似不会再生效了
         */
        //增加事件监听，里面是回调
        TreeCacheListener nodeCacheListener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                System.out.println(treeCacheEvent.getType()+"->"+new String(treeCacheEvent.getData().getData()));
            }
        };

        treeCache.getListenable().addListener(nodeCacheListener);
        treeCache.start();

    }
}
