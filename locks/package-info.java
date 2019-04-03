
/**
 * Interfaces and classes providing a framework for locking and waiting
 * for conditions that is distinct from built-in synchronization and
 * monitors.  The framework permits much greater flexibility in the use of
 * locks and conditions, at the expense of more awkward syntax.
 *
 与内置同步和监视器不同，该包的接口和类提供了一套在使用锁和条件(condition)时更灵活的框架，
 代价是比内置同步和监视器更笨拙的语法。
 
 * <p>The {@link java.util.concurrent.locks.Lock} interface supports
 * locking disciplines that differ in semantics (reentrant, fair, etc),
 * and that can be used in non-block-structured contexts including
 * hand-over-hand and lock reordering algorithms.  The main implementation
 * is {@link java.util.concurrent.locks.ReentrantLock}.
 *
 Lock 接口支持的锁语法风格与传统的synchronized不同，他支持重入，公平等特性，并且可以用在
 非块结构的上下文中including
 * hand-over-hand and lock reordering algorithms.。主要实现时ReentrantLock。
 
 * <p>The {@link java.util.concurrent.locks.ReadWriteLock} interface
 * similarly defines locks that may be shared among readers but are
 * exclusive to writers.  Only a single implementation, {@link
 * java.util.concurrent.locks.ReentrantReadWriteLock}, is provided, since
 * it covers most standard usage contexts.  But programmers may create
 * their own implementations to cover nonstandard requirements.
 *
 ReadWriteLock接口同样定义了锁，支持共享读，但是排他的写。该接口只有ReentrantReadWriteLock一个实现
 ，因为他可以覆盖大多数的应用场景。不过程序员若果有其他应用需求的话，可以自己实现该接口。
 
 
 * <p>The {@link java.util.concurrent.locks.Condition} interface
 * describes condition variables that may be associated with Locks.
 * These are similar in usage to the implicit monitors accessed using
 * {@code Object.wait}, but offer extended capabilities.
 * In particular, multiple {@code Condition} objects may be associated
 * with a single {@code Lock}.  To avoid compatibility issues, the
 * names of {@code Condition} methods are different from the
 * corresponding {@code Object} versions.
 
 Condition接口描述与锁相关的条件变量。这与通过Object.wait访问隐式监视器
 相同，但是Condition提供了扩展功能。尤其是，多个Condition对象可以关联同一个锁。
 为了避免兼容性问题（注：其实就是与Object的wait和notify方法区分开），
 Condition方法的名字特意与Object类的版本不同。
 
 
 *
 * <p>The {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}
 * class serves as a useful superclass for defining locks and other
 * synchronizers that rely on queuing blocked threads.  The {@link
 * java.util.concurrent.locks.AbstractQueuedLongSynchronizer} class
 * provides the same functionality but extends support to 64 bits of
 * synchronization state.  Both extend class {@link
 * java.util.concurrent.locks.AbstractOwnableSynchronizer}, a simple
 * class that helps record the thread currently holding exclusive
 * synchronization.  The {@link java.util.concurrent.locks.LockSupport}
 * class provides lower-level blocking and unblocking support that is
 * useful for those developers implementing their own customized lock
 * classes.
 *
 AbstractQueueSynchronizer类是锁和其他同步器的超类，它基于线程阻塞队列。
 AbstractQueuedLongSynchronizer类提供了相同的功能，但是扩展支持64位的同步器状态（应该指AbstractQueueSynchronizer中的state变量）
 上述两个类都继承了类AbstractOwnableSynchronizer，该类帮助记录当前持有锁的独占线程。
 LockSupport类提供了更底层的阻塞非阻塞支持，对于想要自己实现锁的开发者
 非常有用。
 * @since 1.5
 */
package java.util.concurrent.locks;
