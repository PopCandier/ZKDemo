package com.pop.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pop
 * @date 2019/7/14 22:12
 */
public class AclDemo {


    private static String CONNECT_STR="192.168.255.102:2181,192.168.255.102:2182,192.168.255.102:2183";
    //,192.168.255.102:2182,192.168.255.102:2183
    public static void main(String[] args) throws Exception {

//        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient()
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STR).sessionTimeoutMs(5000)//会话超时时间
                .retryPolicy(new ExponentialBackoffRetry(100, 3)).//重试策略
                build();

        curatorFramework.start();
        //zk 中的权限

//        demo1(curatorFramework);
        demo2(curatorFramework);

    }

    private static void demo2(CuratorFramework curatorFramework) throws Exception {
        List<ACL> list = new ArrayList<>();

        ACL acl = new ACL(ZooDefs.Perms.READ|ZooDefs.Perms.WRITE
                ,new Id("digest",DigestAuthenticationProvider.generateDigest("admin:admin"
              ))

        );
        list.add(acl);
        //如果你希望在节点创建的过程中，再增加其他的权限给其它权限
        curatorFramework.setACL().withACL(list).forPath("/temp");
//        curatorFramework.create().withMode(CreateMode.PERSISTENT)
//                .withACL(list)
//                .forPath("/pp");

       }

    private static void demo1(CuratorFramework curatorFramework) throws Exception {
        List<ACL> list = new ArrayList<>();
        /**
         * ACL的权限创建有几种
         * nt READ = 1;
         *         int WRITE = 2;
         *         int CREATE = 4;
         *         int DELETE = 8;
         *         int ADMIN = 16;
         *         int ALL = 31;
         *  意味着创建这个节点你所拥有的权限
         *  new Id("id","192.168.255.102")
         *  new Id("world","anyone")
         *
         *  ZooDefs.Ids.ANYONE_ID_UNSAFE
         *  当然你也通过ids设置简单的一些整合好的权限
         */

        ACL acl = new ACL(
                ZooDefs.Perms.READ|ZooDefs.Perms.WRITE
                //这里就是一个只读权限 |分号

                ,new Id("digest",
                DigestAuthenticationProvider.generateDigest(
                        "admin:admin"
                ))
                /**
                 *id对象标识一个身份，第一个表示权限模式
                 IP/Digest/world/super
                 ip:某个网段的客户端连接可以拥有权限 192.168.255 255这个网段
                 Digest 需要账号密码的模式，比较常用用‘：’隔开
                 world 开放权限，设了没设一样
                 super 超级用户权限，所有权限都可以享有
                 *
                 */
        );
        list.add(acl);
        curatorFramework.create().withMode(CreateMode.PERSISTENT)
                .withACL(list)
                .forPath("/pp");
        /**
         * 同时注意一点，客户端连接时候需要增加权限认证
         * addauth digest admin:admin
         * 这样子，但是当你重新开一个会话，当然也会告诉你没权限
         */}
}
