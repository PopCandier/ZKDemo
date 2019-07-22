Zookeeper

### zookeeper的安装

等待补充

### zookeeper的讲解

zk的概念其实不难理解，他的应用场景包括master/slave选举，分布式锁，服务注册等。

而他本身作为一个分布式协调组件，他的存在意义也是为了解决一致性问题。

说起zk前身，就需要讲到chubby，chubby也是一个解决一致性的应用，他是一致性协议paxos的具体实现，由于chubby并不是开源的，所以雅虎开发了zookeeper，最后捐献给了apache，zk的虽然也是解决一致性问题，但是他的协议不是paxos而是zab。

由于zk是一个高可用的分布式协调组件，所以意味着他本身也是可以集群的，那么说到集群，就一定会出现主从节点。

所以zk给我们的第一个印象就是

* 能够选举，并且可以提供崩溃恢复
* 数据同步，保持一致性



#### zk的服务器节点

* Leader 集群环境下的主节点
* Follower 集群环境下的从节点
* Observer 本质和Follower一样，但无选举投票权利

zk通过解压后启动zk服务 `sh zkServer.sh start`

后将会创建一个zk实例，每一个实例意味着一个服务器节点，也就是zk提供服务的节点，每个节点拥有自己的树形节点，用于存储不同的树节点，注意，这里的服务器节点和树节点不是一个东西，前者是zk实例后者是实例里的数据。

#### zk实例的数据节点（树节点）

树节点（在这段里统称为节点，请不要和上面的服务器节点搞混），也就是zk中的数据，同时每个节点都拥有自己类型。

* 持久节点

  * 创建后将会一直存在zk服务器上（因为会写到文件里），直到主动删除

* 持久有序节点

  * 会多一个序号，来表示他创建的顺序，方便维护

* 临时节点 

    * 临时节点的生命周期和客户端会话绑定在一起，这意味着客户端失效后该节点会自动清理，也就是具体心跳数后没有反馈。

* 临时有序节点
  * 多了一个顺序

* CONTAINER 
  * 当子节点被删除后，Container也会被删除
* PERSISTENT_WITH_TTL
  * 超过TTL未被修改，且没有子节点的情况下，将会被删除
* PERSISTENT_SEQUENTIAL_WITH_TTL
  * 客户端断开连接后不会自动删除Znode，如果该Znode没有子Znode且在给定TTL时间内无修改，该Znode将会被删除，TTL单位是毫秒，TTL单位是毫秒，必须大于0且小于或等于 EphemeralType.MAX_TTL



#### 会话

客户端连接到zk服务器的时候，会有几种状态

* Client 初始化连接，状态转化为CONNECTING(①)
* Client 与 Server 成功建立起连接，状态转化为CONNECTING(②)
* Client 丢失了与Server 的连接或者没有接受到Server的响应，状态转化为CONNECTING(③)
* Client 连上另外的 Server 或者连上了之前的Server 状态转为CONNECTING(②)
* 若会话过期（Server负责声明会话过期，而不是Client），状态转为CLOSED(⑤)，变成CLOSED
* Client也可以主动关闭会话(④)，状态转为CLOSED



#### Stat 状态信息

节点除了存储数据外，本身还有很多自己本身的信息，可以用stat

命令查看。

## JAVA中权限的设置



用zookeeper 来实现分布式锁

利用zk的节点特性实现独占锁，也就是同级节点的唯一性，多个进程尝试往zk执行节点下创建一个，只有一个成功，表示获取锁成功，另外则失败，创建失败的节点通过zk的watcher机制监听监听这个节点变化，一旦监听到子节点删除，则再出发所有进程去尝试写锁。

##### 从源码方面解释



##### 分布式加锁

```java
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
```

构建了一个锁，在这里我们使用的是`InterProcessMutex`

```java
//创建锁
        final InterProcessMutex lock=
        new InterProcessMutex(curatorFramework,"/locks");

//第一层，首先获得了实例化了标准锁驱动
 public InterProcessMutex(CuratorFramework client, String path)
    {
        this(client, path, new StandardLockInternalsDriver());
    }

//第二层，1 表示 maxLeases,这个表示每一次减少的索引数
//因为未获得锁的连接将会被创建成有序零时节点
 public InterProcessMutex(CuratorFramework client, String path, LockInternalsDriver driver)
    {
        this(client, path, LOCK_NAME, 1, driver);
    }

//第三层 对本地的客户端，锁实现和路径进行封装
 InterProcessMutex(CuratorFramework client, String path, String lockName, int maxLeases, LockInternalsDriver driver)
    {
        basePath = PathUtils.validatePath(path);
        internals = new LockInternals(client, driver, path, lockName, maxLeases);
    }

//到此为止，InterProcessMutex 初始化完毕
```

  我们回到`acquire`方法，很明显，这是一个获取锁的方法

```java
System.out.println(Thread.currentThread().getName()+"->尝试获得锁");
                try {
                    lock.acquire();
                    System.out.println(Thread.currentThread().getName()+"->获得锁成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }

/*
当然，这是一个重载方法，如果你没值表示，超过多少时间等待，否则就传入-1
*/
public void acquire() throws Exception
    {
        if ( !internalLock(-1, null) )
        {
            throw new IOException("Lost connection while trying to acquire lock: " + basePath);
        }
    }
 //其中的方法
 private boolean internalLock(long time, TimeUnit unit) throws Exception
    {
        /*
           Note on concurrency: a given lockData instance
           can be only acted on by a single thread so locking isn't necessary
        */
		//获得当线程
        Thread currentThread = Thread.currentThread();
		//查看当前线程是否已经获得了锁
        LockData lockData = threadData.get(currentThread);
        if ( lockData != null )
        {
            // re-entering 如果获得了锁，将会重入+1，并返回true
            lockData.lockCount.incrementAndGet();
            return true;
        }
		//否则会尝试获得锁，这里传入时间的类型，还有需要等待获得锁的节点，这个节点会被计算出来，当然这个
		//getLockNodeBytes()方法返回的是null
        String lockPath = internals.attemptLock(time, unit, getLockNodeBytes());
        if ( lockPath != null )
        {
            LockData newLockData = new LockData(currentThread, lockPath);
            threadData.put(currentThread, newLockData);
            return true;
        }

        return false;
    }
```

  进入`attemptLock`方法

```java
String attemptLock(long time, TimeUnit unit, byte[] lockNodeBytes) throws Exception
    {
        final long      startMillis = System.currentTimeMillis();
        final Long      millisToWait = (unit != null) ? unit.toMillis(time) : null;
        //由于传进来的byte[] 是null，所以它将会获得当前在zk服务器注册到底序列号是多少，并初始化
        //这个是个累加过程，如果你的前面1已经存在，那么这个时候你就是2 
        //当然，这个地方采取的是ip地址
        final byte[]    localLockNodeBytes = (revocable.get() != null) ? new byte[0] : lockNodeBytes;
        int             retryCount = 0;

        String          ourPath = null;
        boolean         hasTheLock = false;
        boolean         isDone = false;
        while ( !isDone )
        {
            isDone = true;

            try
            { //尝试创建锁，这个方法也就是会创建一个临时有序节点，如果你
                ourPath = driver.createsTheLock(client, path, localLockNodeBytes);
                hasTheLock = internalLockLoop(startMillis, millisToWait, ourPath);
            }
            catch ( KeeperException.NoNodeException e )
            {
            //....
    }
```

`createsTheLock`方法，也就是去zk上创建节点

```java
public String createsTheLock(CuratorFramework client, String path, byte[] lockNodeBytes) throws Exception
    {
        String ourPath;
        if ( lockNodeBytes != null )//不为空表示已经有人创建了 /Locks 这个节点，所以序列还没创建好
        { //未获得锁的后续节点，将会创建临时有序节点，并且他们的序号将会成为他的值
            ourPath = client.create().
            creatingParentContainersIfNeeded().//用于创建父节点，如果不支持CreateMode.CONTAINER,将会采取CreateMode.PERSISTENT
            withProtection().//添加的临时节点会添加GUID前缀
            withMode(CreateMode.EPHEMERAL_SEQUENTIAL).//临时有序节点，zk能保证节点的有序性
            forPath(path, lockNodeBytes);
        }
        else
        {//这个就是创建好的情况，如果能走到这里，说明创建锁成功。这个是没有值的
        /*
        	不过，由于才存在并发的情况，如果同时有两个或者以上的线程走到了这个，将会创建相同的线程，但是
        	zk的特性，节点只允许只有一个名字，所以只有一个线程会创建成功，其它会失败，而失败的客户端将会抛出异常，我们看看外面异常做了什么。
        */
            ourPath = client.create().creatingParentContainersIfNeeded().withProtection().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(path);
        }
        return ourPath;
    }
```

如果并发，去创建同一把锁(节点)的话，做了什么处理。

```java
catch ( KeeperException.NoNodeException e )
            {
                // gets thrown by StandardLockInternalsDriver when it can't find the lock node
                // this can happen when the session expires, etc. So, if the retry allows, just try it all again
    /*
    不过这里到底处理因为seesion过去，而产生到底找不到节点异常。
    是不是处理那个节点创建失败的问题，还要再考虑一下。
    */
                if ( client.getZookeeperClient().getRetryPolicy().allowRetry(retryCount++, System.currentTimeMillis() - startMillis, RetryLoop.getDefaultRetrySleeper()) )
                {
                    isDone = false;
                }
                else
                {
                    throw e;
                }
            }
```

等待获得锁的方法。

```java
hasTheLock = internalLockLoop(startMillis, millisToWait, ourPath);
```

```java
 private boolean internalLockLoop(long startMillis, Long millisToWait, String ourPath) throws Exception
    {
        boolean     haveTheLock = false; //是否获得锁的标志
        boolean     doDelete = false; //是否需要删除子节点
        try
        {
            if ( revocable.get() != null )
            {	//如果不为空，意味着自己前面有节点，那么我们就监听自己的前一个节点
                client.getData().usingWatcher(revocableWatcher).forPath(ourPath);
            }
			//服务器启动状态，并且没有获得锁
            while ( (client.getState() == CuratorFrameworkState.STARTED) && !haveTheLock )
            {	//获得后续排序的节点
                List<String>        children = getSortedChildren();
                //获取自己创建的临时有序节点
                String              sequenceNodeName = ourPath.substring(basePath.length() + 1); // +1 to include the slash
				//对几个参数做一个封装
                PredicateResults    predicateResults = driver.getsTheLock(client, children, sequenceNodeName, maxLeases);
                
                //...
｝
```

获得锁的方法

```java
 public PredicateResults getsTheLock(CuratorFramework client, List<String> children, String sequenceNodeName, int maxLeases) throws Exception
    {	//这个地方是之前获得自己创建节点的名称
    	//看看自己是不是第一个节点，是的话，会返回0，也就是位置
        int             ourIndex = children.indexOf(sequenceNodeName);
        validateOurIndex(sequenceNodeName, ourIndex);
		//有因为 maxLeases 在初始化的时候是 1 ，所以 outIndex 只有一个选择，那就是 0
		//的时候，这个才会返回true，意味着获得了锁
        boolean         getsTheLock = ourIndex < maxLeases;
        //是否需要监听，如果我的前一个节点位置在子树中不是0号位，也就是不是第一个
        //如果获得了锁，那么就不用监听，否则就监听自己的上一个节点，也就是 自己的位置（ourIndex -1） 
        String          pathToWatch = getsTheLock ? null : children.get(ourIndex - maxLeases);
		//	做一个封装，传出去，包含自己是否获得了锁，还有监听的路径（自己的上一个节点）
        return new PredicateResults(pathToWatch, getsTheLock);
    }
    
static void validateOurIndex(String sequenceNodeName, int ourIndex) throws KeeperException
    {//这里可知，获得了前一个节点的序号，序号肯定是不允许是小于0的
        if ( ourIndex < 0 )
        {
            throw new KeeperException.NoNodeException("Sequential path not found: " + sequenceNodeName);
        }
    }
```

回到之前的循环获得锁的方法

```java
 PredicateResults    predicateResults = driver.getsTheLock(client, children, sequenceNodeName, maxLeases);
                if ( predicateResults.getsTheLock() )//是否获得了锁
                {
                    haveTheLock = true;
                }
                else
                { //否则进行监听，因为监听的路径已经返回回来了
                    String  previousSequencePath = basePath + "/" + predicateResults.getPathToWatch();

                    synchronized(this)
                    {
                        try 
                        {
                            // use getData() instead of exists() to avoid leaving unneeded watchers which is a type of resource leak
                            client.getData().usingWatcher(watcher).forPath(previousSequencePath);
                            if ( millisToWait != null )
                            {
                                millisToWait -= (System.currentTimeMillis() - startMillis);
                                startMillis = System.currentTimeMillis();
                                if ( millisToWait <= 0 )
                                {
                                    doDelete = true;    // timed out - delete our node
                                    break;
                                }

                                wait(millisToWait);//带超时的阻塞
                            }
                            else
                            {
                                wait();//阻塞
                            }
                        }
                        catch ( KeeperException.NoNodeException e ) 
                        {
                            // it has been deleted (i.e. lock released). Try to acquire again
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            ThreadUtils.checkInterrupted(e);
            doDelete = true;
            throw e;
        }
        finally
        {
            if ( doDelete )
            {
                deleteOurPath(ourPath);
            }
        }
        return haveTheLock;
    }

```

同时这个方法，在获得锁的同时将会跳出循环。

```java
 try
            {
                ourPath = driver.createsTheLock(client, path, localLockNodeBytes);
                hasTheLock = internalLockLoop(startMillis, millisToWait, ourPath);
            }
            catch ( KeeperException.NoNodeException e )
            {
                // gets thrown by StandardLockInternalsDriver when it can't find the lock node
                // this can happen when the session expires, etc. So, if the retry allows, just try it all again
                if ( client.getZookeeperClient().getRetryPolicy().allowRetry(retryCount++, System.currentTimeMillis() - startMillis, RetryLoop.getDefaultRetrySleeper()) )
                {
                    isDone = false;
                }
                else
                {
                    throw e;
                }
            }
        }

        if ( hasTheLock )
        {
            return ourPath;//将自己的创建的节点返回回去
        }

        return null;
    }
```

然后回到最初的地方，完成分布式锁的操作

```java
private boolean internalLock(long time, TimeUnit unit) throws Exception
    {
        /*
           Note on concurrency: a given lockData instance
           can be only acted on by a single thread so locking isn't necessary
        */

        Thread currentThread = Thread.currentThread();

        LockData lockData = threadData.get(currentThread);
        if ( lockData != null )
        {
            // re-entering
            lockData.lockCount.incrementAndGet();
            return true;
        }

        String lockPath = internals.attemptLock(time, unit, getLockNodeBytes());
        //返回的时候，也就是获得锁的时候，将会得到自己创建的临时有序节点
        if ( lockPath != null )
        {	//存入lockData方便之后重入，并通知获得锁
            LockData newLockData = new LockData(currentThread, lockPath);
            threadData.put(currentThread, newLockData);
            return true;
        }
		//否则，获得锁失败
        return false;
    }
```

##### 分布式解锁

回到我们的主要代码

```java
try {
                    Thread.sleep(4000);//模仿业务处理
                    lock.release();//释放锁
                    System.out.println(Thread.currentThread().getName()+"->释放锁成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
```

进入内部方法

```java
public void release() throws Exception
    {
        /*
            Note on concurrency: a given lockData instance
            can be only acted on by a single thread so locking isn't necessary
         */

        Thread currentThread = Thread.currentThread();
        LockData lockData = threadData.get(currentThread);
        if ( lockData == null )
        {
            throw new IllegalMonitorStateException("You do not own the lock: " + basePath);
        }
		//得到自己的锁重入次数，并-1
        int newLockCount = lockData.lockCount.decrementAndGet();
        if ( newLockCount > 0 )//表示有重入，直接返回
        {
            return;
        }
        if ( newLockCount < 0 )
        {
            throw new IllegalMonitorStateException("Lock count has gone negative for lock: " + basePath);
        }
        try
        {//解锁的方法
            internals.releaseLock(lockData.lockPath);
        }
        finally
        {//解锁完毕，删除这个缓存的线程
            threadData.remove(currentThread);
        }
    }
```

进入

```java
void releaseLock(String lockPath) throws Exception
    {
        revocable.set(null);
        deleteOurPath(lockPath);
    }
```

```java
private void deleteOurPath(String ourPath) throws Exception
    {
        try
        {
            client.delete().guaranteed().forPath(ourPath);
        }
        catch ( KeeperException.NoNodeException e )
        {
            // ignore - already deleted (possibly expired session, etc.)
        }
    }//其实就是把这个节点删除掉。
```

##### 使用zookeeper 实现Leader 选举

在分布式计算中，leader election 是很重要的一个功能， 这个选举过程是这样子的：指派一个进程作为组织者，将 任务分发给各节点。在任务开始前，哪个节点都不知道谁是 leader 或者 coordinator。当选举算法开始执行后，每 个节点最终会得到一个唯一的节点作为任务 leader。除此 之外，选举还经常会发生在 leader 意外宕机的情况下，新 的 leader 要被选举出来。 

Curator 有两种选举 recipe（Leader Latch 和 Leader  Election）

* Leader Latch

  * 参与选举的所有节点，会创建一个顺序节点，其中最小的 

    节点会设置为 master 节点, 没抢到 Leader 的节点都监听 

    前一个节点的删除事件，在前一个节点删除后进行重新抢 

    主，当 master 节点手动调用 close（）方法或者 master 

    节点挂了之后，后续的子节点会抢占 master。 

    其中 spark 使用的就是这种方法

* Leader Selector

  * LeaderSelector 和 Leader Latch 最的差别在于，leader 

    可以释放领导权以后，还可以继续参与竞争

代码演示。

```java
public class SelectorClient2 extends 
LeaderSelectorListenerAdapter implements Closeable 
{
private final String name;
private final LeaderSelector leaderSelector;
public SelectorClient2(CuratorFramework
client, String path, String name) {
this.name = name;
// 利用一个给定的路径创建一个 leader 
selector
// 执行 leader 选举的所有参与者对应的路径必
须一样
// 本例中 SelectorClient 也是一个
LeaderSelectorListener，但这不是必须的。
leaderSelector = new 
LeaderSelector(client, path, this);
// 在大多数情况下，我们会希望一个 selector
放弃 leader 后还要重新参与 leader 选举
leaderSelector.autoRequeue();
}
public void start(){
leaderSelector.start();
}
@Override
public void close() throws IOException {
	leaderSelector.close();
}
@Override
public void takeLeadership(CuratorFramework 
curatorFramework) throws Exception {
System.out.println(name + " 现在是 leader了，持续成为 leader ");
//选举为 master，
System.in.read();//阻塞，让当前获得 leader权限的节点一直持有，直到该进程关闭
}
private static String 
CONNECTION_STR="192.168.13.102:2181,192.168.13.103
:2181,192.168.13.104:2181";
public static void main(String[] args) throws IOException {
CuratorFramework curatorFramework= 
    CuratorFrameworkFactory.builder().
    connectString(CONNECTION_STR).sessionTimeoutMs(5000).retryPolicy(new ExponentialBackoffRetry(1000,3)).build();
    
    curatorFramework.start();
    
    SelectorClient2 sc=new SelectorClient2(curatorFramework,"/leader","ClientB");
    sc.start();
    System.in.read();
}
```

#### Zookeeper 数据的同步流程

zk作为一个高可用的分布式协调组件，他本身也可以做集群的处理。

我们属性的zk中有三种角色

* Leader
* Follwer
* Observer

事务请求将会由Leader处理，这里的事务请求包括客户端的对zk服务器节点的增删改方法，由于节点发生改变，其它的Follwer就需要进行节点的同步，以保证外部无论连接到哪个zk集群的节点都可以读到最新的数据。

当然，客户端有可能会连接到Follwer进行事务操作，这个时候Follwer将会转发给Leader进行处理。

也就是，读请求可以由于Follwer和Leader处理

但是，写请求必须由Leader处理，即便Follwer得到了也必须转发给

Leader节点处理。

当Leader 节点处理好后(未提交)，将会进行广播给所有从节点。

也就是会发送一个proposal给所有的Follower节点，当从节点确定自己已经和Leader节点

数据同步完成后，将会返回一个ack给Leader，表示，我已经同步完成

但是，由于集群的数目可能很大，zk本质来说也并不是

强一致性，所以只要从节点的半数节点同步完成，也就是ack的接受

数目超过一半，Leader就认为这个数据同步成功，并且提交(commoit)

各个Follwer，表示同步完成。(类2PC事务)



#### Zab协议

ZAB（Zookeeper Atomic Broadcast） 协议是为分布式协调服务 ZooKeeper 专门设计的一种支持崩溃恢复的原子广播协议。在 ZooKeeper 中，主要依赖 ZAB 协议来实现分布式数据一致性，基于该协议，ZooKeeper 实现了一种主备模式的系统架构来保持集群中各个副本之间的数据一致性。

ZAB 协议包含两种基本模式。

分别是1. 崩溃恢复2. 原子广播当整个集群在启动时，或者当 leader 节点出现网络中断、崩溃等情况时，ZAB 协议就会进入恢复模式并选举产生新的 Leader，当 leader 服务器选举出来后，并且集群中有过半的机器和该 leader 节点完成数据同步后（同步指的是数据同步，用来保证集群中过半的机器能够和 leader 服务器的数据状态保持一致），ZAB 协议就会退出恢复模式。当集群中已经有过半的 Follower 节点完成了和 Leader 状态同步以后，那么整个集群就进入了消息广播模式。这个时候，在 Leader 节点正常工作时，启动一台新的服务器加入到集群，那这个服务器会直接进入数据恢复模式，和leader 节点进行数据同步。同步完成后即可正常对外提供非事务请求的处理。需要注意的是：leader 节点可以处理事务请求和非事务请求，follower 节点只能处理非事务请求，如果 follower 节点接收到非事务请求，会把这个请求转发给 Leader 服务器

#### 消息广播的实现原理

，消息广播的过程实际上是一个 

简化版本的二阶段提交过程 

\1. leader 接收到消息请求后，将消息赋予一个全局唯一的 

64 位自增 id，叫：zxid，通过 zxid 的大小比较既可以实 

现因果有序这个特征 

\2. leader 为每个 follower 准备了一个 FIFO 队列（通过 TCP 

协议来实现，以实现了全局有序这一个特点）将带有 zxid 

的消息作为一个提案（proposal）分发给所有的 follower 

\3. 当 follower 接收到 proposal，先把 proposal 写到磁盘， 

写入成功以后再向 leader 回复一个 ack 

\4. 当 leader 接收到合法数量（超过半数节点）的 ACK 后， 

leader 就会向这些 follower 发送 commit 命令，同时会 

在本地执行该消息 

\5. 当 follower 收到消息的 commit 命令以后，会提交该消息

#### 崩溃恢复的实现原理

前面我们已经清楚了 ZAB 协议中的消息广播过程，ZAB 协 

议的这个基于原子广播协议的消息广播过程，在正常情况 

下是没有任何问题的，但是一旦 Leader 节点崩溃，或者由 

于网络问题导致 Leader 服务器失去了过半的 Follower 节 

点的联系（leader 失去与过半 follower 节点联系，可能是 

leader 节点和 follower 节点之间产生了网络分区，那么此 

时的 leader 不再是合法的 leader 了），那么就会进入到崩 

溃恢复模式。崩溃恢复状态下 zab 协议需要做两件事 

\1. 选举出新的 leader 

\2. 数据同步 

前面在讲解消息广播时，知道 ZAB 协议的消息广播机制是 

简化版本的 2PC 协议，这种协议只需要集群中过半的节点 

响应提交即可。但是它无法处理 Leader 服务器崩溃带来的 

数据不一致问题。因此在 ZAB 协议中添加了一个“崩溃恢 

复模式”来解决这个问题。 

那么 ZAB 协议中的崩溃恢复需要保证，如果一个事务 

Proposal 在一台机器上被处理成功，那么这个事务应该在 

所有机器上都被处理成功，哪怕是出现故障。为了达到这 

个目的，我们先来设想一下，在 zookeeper 中会有哪些场 

景导致数据不一致性，以及针对这个场景，zab 协议中的 

崩溃恢复应该怎么处理。



### Zookeeper 的一致性问题

zk的一致性是顺序一致性模型，之前看出分布式锁也是这个原因，当一个程序来争抢锁的时候，其它程序看到应该是已经获得锁的状态，而不是其它状态。

之前谈到的顺序一致性，当三个客户端 ClientA/B/C串行执行的时候，

如果A去更新了某个节点`/data`的值由x=0变成了x=1，那么，由于zk是个集群环境，那么意味着B和C连接的可能不是A连接的zk节点，也就意味着，此时数据还未同步到BC连接的客户端。

也就是说，A更新值成功，此时B客户端所连接的Follwer节点已经同步，则意味着A和B都可以获得最新的值，也就是x=1，但是C的读操作发生在B之后，他可能此时读到是个过期的值，也就是x=0，这显然是一种弱一致性模型。

#### 回到顺序一致性问题

弱一致性会有明显的一致性问题，所以我们回到顺序一致性问题上面。，顺序一致性提供了更强的一致性保证。

![1563777591635](C:\Users\范凌轩\AppData\Roaming\Typora\typora-user-images\1563777591635.png)

首先B0优先于A0发起请求，读取x的值，这个时候值是0

B2在A0设置之后读取x的值，所以这个时候的值为1

但是，在A0、B1、C0的这个时间段，这两个请求是重叠的，B1和C0读到的值可能是1也可能是0

**需要注意的是**，由于网路延迟和系统本身的执行请求的不确定导致，请求发的早的客户端不一定在服务端执行得早，最终还是以服务器的执行为主。

简单来说：顺序一致性是针对单个操作，单个数据对象，属于CAP的中的C（一致性）的范畴，一个数据更新后，能够立马被后续的读操作读到。



单对于zk来说，他无法保证每个zk实例中，两个客户端可以获得相同的数据视图，

因为网络环境比较复杂，所以一个客户端可能会在另一个客户端收到更改通知之后执行更新。

假设客户端A和B的两个场景，由于A的操作/a变成了1，此时B去读取，也许会读到旧的值，如果AB要读取必须读取到相同的值，可以在B读取操作之前执行 `sync`方法。



此外，zk基于zxid（事务id）以及阻塞队列的方式实现请求顺序的一致性，如果一个client

连接到了一个最新follower上，那么他的read读了最新是数据，然后因为某个原因重新连接到一个一个还没有完全数据同步带的follwer节点上，那么这次读到的数据不就是旧的数据了吗？其实呢zk的客户端将会记录自己读到的最大的zxid，我们之前说过zxid的高32位记录的是选举朝代 epoch，后面是同步的次数，如果发现client重新连接到server发现，client的zxid比自己大，连接会失败，因为这个节点的数据不是最新的，所以会连接失败。



### Leader 选举的原理

Leader 的选举 会发生在两个阶段，一个是服务器启动的时候Leader选举，另一个是之前说过的，Leader节点崩溃后导致的Leader选举。

当然，选举是需要依据的，所以需要了解几个重要的参数

* 服务器ID（myid）
  * 比如有三台服务器，编号分别是1，2，3
  * 编号越大在选择算法中的权重越大
* zxid事务id
  * 值越大说明数据越新，在选举算法中的权重也越大
* 逻辑时钟（epoch-logicalclock）
  * 或者叫投票的次数，同一轮投票过程中逻辑时钟是相同的，每一次投完一次票这个数据就会增加，然后与接收到的其他服务器返回的投票信息中的数值相比，根据不同的值做出不同的判断。
* 选举状态
  * LOOKING  竞选状态
  * FOLLOWING 随从状态，同步Leader状态，参与投票
  * OBSERVING，观察状态，同步Leader状态，不参与投票
  * LEADING 领导者状态。

#### 服务器启动时Leader



