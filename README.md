# Zookeeper

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







  

  

