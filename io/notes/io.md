# File IO

## pagecache

**OS没有绝对的数据可靠性
为什么设计pagecache？
    是为了减少硬件IO的调用，提速，优先使用内存
即便想要可靠性调成最慢的方式，但是单点问题会让你的性能损耗，没有收益
主从复制、主备HA kafka/ES 副本（socket IO）（同步/异步）**

> /etc/sysctl.conf
> sysctl -a | grep dirty

```shell
vm.dirty_background_bytes = 0
vm.dirty_background_ratio = 10   # 缓存页阈值 内存=>磁盘写 LRU策略 后台写
vm.dirty_ratio = 30   # 阻塞程序 内存=>磁盘写 LRU策略
vm.dirty_bytes = 0
vm.dirty_expire_centisecs = 3000 # 脏页过期
vm.dirty_writeback_centisecs = 500 # 脏页什么时间写 毫秒
```

```txt
redis(持久化 AOF)、mysql（binlog,undolog,redolog）
    都有三种策略 是由操作系统限制的
    1、每秒种写
    2、由系统控制
    3、每操作写

用户程序写给内核不一定会立刻写 => 需要flush
已经写过得脏页会被 LRU 抹杀掉
如果脏页没有写会先想磁盘写

调用 flush 是直接将pagecache写到磁盘上
硬件、内核、程序有缓存都有可能丢数据

程序的线性地址通过mmu对应到内存的逻辑地址
```

> Buffer 和 普通IO 那个快 为啥快
>

    Buffer快
    Buffer =(JVM)> default 8kb =(syscall)> write(8byte[])

## Java API

### NIO

> ByteBuffer

```
allocate(1024)          // 在堆上分配
allocateDirect(1024)    // 在堆外分配
 -- >
        | 堆内，JVM堆里的字节数组
        | 堆外，指的是JVM堆外的，也就是Java进程内的
        | mapped映射：是mmap调用的一个进程和内核共享的内存区域，切这个内存区域是pagecache/到文件的映射
        | on heap < off heap < mapped(file)
        | netty(on heap,off heap)
        | kafka(log:mmap)
buffer.position()       // 开始位置
buffer.limit()          // 结束位置
buffer.capacity()       // 总容量
buffer.put(data);       // 想缓存添加数据
buffer.flip();          // 翻转读写行为交替
buffer.get();           // 获取一个字节
buffer.compact();       // 整理缓存空间
buffer.clear();         // 清除缓存
```

> FileChannel


只有文件的通道才会有 map(mmap) 可以获取一个堆外、和文件映射的 java.nio.MappedByteBuffer

```
java.nio.MappedByteBuffer
put()           // 不是系统调用 但是数据会到达内核的pagecache，曾经只有通过 out.write()这样的系统调用，数据才能到达pagecache
                // mmap的内存映射，依然是内核的pagecache体系所约束的 也就是丢数据
                // 可以找C程序写的jni扩展库，使用Linux内核的Direct IO
                // 直接IO是忽略Linux的pagecache
                // 是把pagecache 交给了程序自己开辟一个字节数组当做pagecache，动用代码逻辑来维护一致性/dirty。。。一系列的问题
                // DB（数据库）一般会使用Driect IO
force()         // 刷写到磁盘
read()          // 向缓存对象写数据
```

# Network IO

## TCP/IP

**面向连接，可靠的传输协议**
MTU => 数据包大小
    | MSS => 数据内容大小
    | win => 协商的窗口大小 TCP拥塞控制 发送一批数据包、客户端发送数据过多会被丢弃
```
三次握手之后，才会在Client、Server开辟资源
参数
    cli
        nodelay 延迟拼包发送
        oob 提前发送首字节
        keepalive 不能确定对方依然活跃，会做心跳测试
        
三次握手，四次分手
    
    C -- FIN --> S(CLOSE_WAIT)
    C <-- FIN ACK -- S
    C(FIN_WAIT2) <-- FIN -- S 
    C(TIME_WAIT [2MSL(2倍的报文等待时间)]) -- ACK --> S(CLOSED)
    TIME_WAIT ：谁先发起的关闭谁出现，网络通信没有真实的连接，有可能最后的ACK没有到达对方，自己多留一会
                消耗四元组的规则，在TIME_WAIT没有结束之前，相同的对端不能使用这个资源，建立新的连接，这个不是DDOS攻击
                可以通过设置reuse
```

## Socket

**socket 是一个四元组（cip cport + sip sport）**


```
server.bind(new InetSocketAddress(9090), BACK_LOG); // 后续队列存放多少连接
server.setSoTimeout(SO_TIMEOUT);                    // timeout时间

client.setSoTimeout(CLI_TIMEOUT);                   // 客户端timeout时间

```

## 网络IO模型的演变

```
同步
异步 Linux现版本中是没有实现的 win:IOCP
阻塞
非阻塞

基本的系统调用
    socket = sfd
    bind(sfd.port)
    listen(sfd)         =>> 开启监听
    
    BIO     弊端：内核提供的API，存在大量阻塞模型
        accept(sfd,         =>> 阻塞等待客户端连接  ==> cfd(客户端连接)
        recv(cfd,           =>> 等待客户端传输数据 block
    SOCK_NONBLOCK  弊端：大量的用户态，内核态切换(系统调用)查询
        socket(domain,SOCK_NONBLOCK,protocol) = sfd
        fcntl(fd,F_SETFL,/* ...arg... */)
        无用的read被调起，才是问题所在
       
    Selector
        多条路IO通过一个系统调用，获得其中的IO状态，然后，由程序自己对着有状态的IO进行R/W
        只要程序自己读写，那么，你的IO模型就是同步的
        posix 标准的，OS都会提供的规范
        int select(int nfds, fd_set *readfds, fd_set *writefds,
                  fd_set *exceptfds, struct timeval *timeout)
                  FD_SETSIZE(1024) select是有限制的
        poll
        -----------> 之上的IO模型，改进到这种程度，在询问是否有事件到达的fds的复制切换，已经是弊端了
                    | 其实，这些IO模型都是要遍历所有的IO询问状态
                    | NIO:这个遍历的过程成本在用户态内核态的切换
                    | select、poll：这个遍历的过程触发了一次系统调用，用户态内核态的切换，过程中，把fds传递给内核，内核重新根据用户这次调用
                                    传过来的fds，遍历，修改状态
                    | 弊端：
                        | 每次都要重新，重复传递fds
                        | 每次，内核被调了之后，针对这次调用，触发一个遍历fds全量的复杂度
        epoll
            epoll_create    创建成功会返回 epfd => 内核开辟红黑树的空间
            epoll_ctl(int epfd, int op, int fd, struct epoll_event *event)
                            op(操作),fd(监控的),event(事件)
            epoll_wait(int epfd, struct epoll_event *events,
                      int maxevents, int timeout)
                            中断处理的延伸操作，访问epoll_create的空间，如果自身存在，就将自身的fd拷贝到链表中
                            当epoll_wait在次访问的时候，就可以直接获取有事件到达的fd，不需要在此遍历了
            -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
             strace -ff -o out java -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider -cp /root/netty-all-
            4.1.48.Final.jar:.  NettyIO
            
```




