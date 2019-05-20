/*在并发编程中通常使用的工具类集合。这个包有
1 标准可扩展框架（小型）
2 提供相当有用的功能的类（自己实现的话不仅繁琐，还很困难）

这是一个关于这包的组件的简要的描述。你也可以看java.util.concurrent.locks 和java.util.concurrent.atomic包的内容
Executors
Interfaces。
  Executor是一个简单的标准接口，用于自定义类似线程子系统。包括线程池，异步IO和轻量级任务框架。
根据你选择的实体Executor类，任务可以在一个新创建的线程，一个已经存在的执行任务线程，或调用execute的线程中执行，并且可以顺序执行或者同步执行。
ExecutorService提供更完整的异步任务执行框架。一个ExecutorService管理着任务的队列和调度，而且可以关闭自己。ScheduledExecutorService子接口和相关接口添加了
一些关于任务延时执行，任务周期执行的功能。ExecutorService提供了安排异步执行任何任务的方法。这些任务抽象为Callable方法，可以理解为带有结果返回的Runnable方法。
Future返回函数的结果，用来确定执行是否完成，
并且提供了取消执行的方法。RunnableFutrue是一个拥有run方法的Future。在执行时设置其结果。

Implementations。
  类ThreadPoolExecutor和ScheduledThreadPoolExecutor提供了可调控，灵活的线程池。
类Executors不仅为创建通用配置的线程池提供了工厂方法，也为使用这些线程池提供了工具方法。其他基于Executors的工具包括FutureTask
，一个Future接口的通用实现;ExecutorCompletionService，支持协调处理群体异步任务。
Class ForkJoinPool提供了一个执行器，主要是为了处理ForkJoinTask和他的子类。这些类采用任务偷取调度器来达到
对计算密集平行处理任务的高吞吐。

Queues
ConcurrentLinkedQueue类提供一个可扩展的线程安全的非阻塞FIFO队列。ConcurrentLinkedDeque在他的基础上添加了对Deque接口的支持
继承自BlockingQueue的实现有五个。
LinkedBlockingQueue，ArrayBlockingQueue，SynchronousQueue，PriorityBlockingQueue，DelayQueue
这些类适用于生产者消费者，平行任务处理以及相关并发设计的大多数场景。

扩展接口TransferQueue及其实现LinkedTransferQueue介绍了一个同步传输方法，即一个生产者可以阻塞等待他的消费者。
BlockingDeque接口继承自BolckingQueue 他支持FIFO和LIFO，Class LinkedBlockingDeque提供具体实现

Timing
TimeUnit类提供
  用于指定和控制基于超时的操作的多个时间粒度（包括纳秒）。该包的许多类都包含基于超时的操作，当然也可以无限等待。在所有使用超时等待的情况下，
超时指定了方法应该等待的最小时间。当超时发生时，实现类尽可能准确的检测到他们。（任何时间都不可能准确，例如等待5s，也许是等待了5.0001s，也可能是
4.988888s）然而，一个线程在检测到超时后到他再次执行之间的时间并不确定。？？？？
带有超时参数的方法，当传入的参数小于等于0时意味着一点也不要等待。若果你想一直等待的话，那可以使用Long.MAX_VALUE

Synchronizers
五个常见的专用同步工具。
Semaphore是一个经典的并发工具。
CountDownLatch简单但实用，他可以使你想要求的条件都满足之后使程序继续执行，否则一直阻塞。
Cyclic Barrier是一个可重置多路同步器，在并行程序中很有用。（循环利用版的CountDownLatch）
Phaser提供更灵活的栅栏形式，被用来控制多线程之间的阶段计算
Exchanger允许两个线程在交汇点交换各自的对象，在管道设计中也很有用。


ConcurrentCollections
除了队列，该包还提供了在多线程环境下使用的集合。
ConcurrentHashMap ConcurrentSkipListMap ConcurrentSkipListSet，CopyOnWriteArrayList，CopyOnWriteArraySet
当多线程访问一个共享的集合时，ConcurrentHashMap一般优于同步包装的HashMap，ConcurrentSkipListMap一般优于同步包装的TreeMap
当预期的读和遍历操作远远超过写操作的时候，CopyOnWriteArrayList优于同步包装的ArrayList。（读多写少的场景还是挺常见的）

Concurrent前缀可以帮你快速区分并发类和同步类。例如，HashTable和Collections.synchronizedMap(new HashMap() )是同步。
但CocurrentHashMap是并发。并发容器是线程安全的，但他并不是被独占锁保护。以ConcurrentHashMap为例，他允许多线程同时读取和写入。
同步类通过单独锁阻止所有的访问来保证安全性，不过代价是扩展性较差。一般情况下，请使用并发类而不是同步类。当然，集合 当不需要线程间
共享或者只有当持有其他锁时才能访问的话，非同步容器会更好。

关于迭代，并发类（包括队列）与java.util下的约定有一定的不同。并发类提供弱一致性而不是快速失败【fast-fail】
即当在迭代过程中有其他线程对集合进行修改的时候并不抛出ConcurrentModificationException，而是继续执行。
they are guaranteed to traverse elements as they existed upon
 * construction exactly once, and may (but are not guaranteed to)
 * reflect any modifications subsequent to construction.
并发类只保证迭代器构造时存在的元素会被遍历
，不保证迭代开始之后的任何修改可见。

内存可见性
<a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5">Chapter 17 of the Java Language Specification</a> 定义了
内存的happen-before关系，例如共享变量的读写。某个线程对共享变量的写入对其他线程来说，只要发生在这个写入之后，就保证他是可见的。synchronized
，volatile结构，Thread.start，Thread.join方法等也有happen-before关系。尤其，
1 同一个线程的执行顺序按照程序指令的顺序来进行。
2 同一个监视器，前一个锁释放一定在下一个锁的获得之前发生。同时，happenbefore关系具有传递性。前一个锁释放之前的操作同样在下一个锁获得之后的所有动作之前发生。
3 同一个volatile字段，对他的写入一定在随后对他的读取之前发生。读写volatile字段和进出监视器有相同的内存一致性效果，但这不表示互斥锁定。
4 一个线程中对start的调用发生在任何其他该线程start之后的动作之前。
5 被join的线程的所有动作都在join返成功回之前完成。
java.util.concurrent包和他的子包的所有类的方法都继承这一保证从而达到更高级的同步。尤其：
1 在一个线程，往一个并发容器放入A对象之前的操作在随后另一个线程中访问或移除该A对象的操作之前放生。
2 提交一个Runnable到Executor之前的action在他的执行开始之前发生。提交Callable到ExecutorService同样如此。
3 异步计算的action，即Future类，在随后的其他线程的Future.get()方法调用返回之前发生。
4 对于同一个同步器, 释放同步器之前的方法例如Lock.unLock(),Semaphore.release,CountDownLatch.countDown在获取同步器例如Lock.lock(),Semaphore.acquire(),Condition.await
,CountDownLatch.await()之前发生。
5 CyclicBarrier.await,Phaser.awaitAdvance在栅栏动作之前发生。新一轮的await在前一个栅栏动作之后发生。
 
 * @since 1.5
 */
package java.util.concurrent;