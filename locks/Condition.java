
package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * {@code Condition} factors out the {@code Object} monitor                 考虑每个对象有多个条件来触发的等待集合，Object对象的监视器方法wait,notify,notifyAll
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}        显得力不从心，这就是Condition出现的原因。（这一句话可以看第四段的例子）
 * and {@link Object#notifyAll notifyAll}) into distinct objects to         我们可以使用任意的Lock实现融合这些方法。就如Lock代替了synchronized一样，
 * give the effect of having multiple wait-sets per object, by              Condition代替了Object的监视器方法。
 * combining them with the use of arbitrary {@link Lock} implementations.   
 * Where a {@code Lock} replaces the use of {@code synchronized} methods    
 * and statements, a {@code Condition} replaces the use of the Object       
 * monitor methods.                                                         
 *
 * <p>Conditions (also known as <em>condition queues</em> or                条件（又名条件队列，条件变量）提供了一个方法，使得一个线程挂起(wait)直到
 * <em>condition variables</em>) provide a means for one thread to          来自另一个线程的通知到达。因为访问这个条件发生在多线程中，条件要与
 * suspend execution (to &quot;wait&quot;) until notified by another        一个Lock相关联。condition的wait使释放相关锁和挂起当前线程成为一个原子操
 * thread that some state condition may now be true.  Because access        作，就如Object.wait。
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 *
 * <p>A {@code Condition} instance is intrinsically bound to a lock.         一个Condition实例与一个lock内部绑定。为了获取指定锁的Condition实例，使用
 * To obtain a {@code Condition} instance for a particular {@link Lock}      Lock的newCondition方法。
 * instance use its {@link Lock#newCondition newCondition()} method.
 *
 * <p>As an example, suppose we have a bounded buffer which supports         例如：假设我们有一个有界缓存，支持put和take方法。如果缓存为空时take，线程
 * {@code put} and {@code take} methods.  If a                               将阻塞直到有元素可以获得。如果缓存为满时put，线程将阻塞直到缓存不满。我们
 * {@code take} is attempted on an empty buffer, then the thread will block  想分开控制 等待缓存不空的线程 和 等待缓存不满的线程，这样当他们各自的条件满足时
 * until an item becomes available; if a {@code put} is attempted on a       我们就可以只通知条件满足的那个线程。（wait会随机通知一个，不能满足我们的要求）
 * full buffer, then the thread will block until a space becomes available.  这可以通过两个Condition实例来实现。
 * We would like to keep waiting {@code put} threads and {@code take}        
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two                   
 * {@link Condition} instances.
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides        ArrayBlockingQueue类就提供了这一功能，所以我们不必要再次实现了。           
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 *
 * <p>A {@code Condition} implementation can provide behavior and semantics   Condition实现提供了与Object监视器方法不同的语义和行为，例如保证
 * that is                                                                     通知(signal)的顺序，通知(signal)的时候线程不必持有锁等。
 * different from that of the {@code Object} monitor methods, such as          
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the             如果一个实现提供了上述这样的特殊语义，请写文档。
 * implementation must document those semantics.
 *
 * <p>Note that {@code Condition} instances are just normal objects and can    注意Condition实例也是一个普通对象，你也可以在synchronized中使用它，Condition
 * themselves be used as the target in a {@code synchronized} statement,       也有监视器方法（wait，notify，notifyAll),这些方法和跟Lock相关的await，signal
 * and can have their own monitor {@link Object#wait wait} and                 方法没有任何关系，避免混淆，请不要同时使用它们。
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 *
 * <p>Except where noted, passing a {@code null} value for any parameter        除非特别说明，参数传null将导致NullPointerException。
 * will result in a {@link NullPointerException} being thrown.
 *
 * <h3>Implementation Considerations</h3>                                        实现细节。
 *
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious               通常来说，当线程等待一个Condition时，虚假唤醒有可能发生。这对大多数应用程序
 * wakeup</em>&quot; is permitted to occur, in                                   几乎没有影响。不过为了健壮性考虑，一个应用程序应该总是在循环中判断唤醒条件。
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a            https://en.wikipedia.org/wiki/Spurious_wakeup有说明和例子。
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 *
 * <p>The three forms of condition waiting                                        条件等待三种形式（可中断，不可中断，超时）可能在平台实现和执行特性上
 * (interruptible, non-interruptible, and timed) may differ in their ease of      有所不同。尤其是，可能难以提供这些特性并维护特定语义，如排序保证。
 * implementation on some platforms and in their performance characteristics.      
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may      进一步，中断悬挂线程的能力并不是在所有平台都可行。
 * not always be feasible to implement on all platforms.
 *
 * <p>Consequently, an implementation is not required to define exactly the        因此，实现该接口的类不要求准确定义这三种wait形式，也不要求必须支持
 * same guarantees or semantics for all three forms of waiting, nor is it          可中断悬挂线程。
 * required to support interruption of the actual suspension of the thread.
 *
 * <p>An implementation is required to                                            一个实现要求清晰的记录相关语义并保证提供三种wait形式中的一个，当实现类
 * clearly document the semantics and guarantees provided by each of the          支确实持悬挂线程的中断时，它必须遵循该接口所定义的中断语义。
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 *
 * <p>As interruption generally implies cancellation, and checks for              中断通常意味着取消，中断的检查通常也很少发生，实现类可以通过
 * interruption are often infrequent, an implementation can favor responding      方法return来实现响应中断。
 * to an interrupt over normal method return. This is true even if it can be      注：aqs锁的获取方法中利用return来响应中断，当然少不了unsafe.park unpark
 * shown that the interrupt occurred after another action that may have           的支持。
 * unblocked the thread. An implementation should document this behavior.         另一个动作已经使线程从阻塞中解放之前中断发生 这种情况也是正确的。
 *                                                                                还是那句话，实现这个接口的人请写文档。
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * Causes the current thread to wait until it is signalled or                  该方法造成当前线程等待直到该线程被通知或者线程中断。        
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>The lock associated with this {@code Condition} is atomically            与该Condition实例关联的Lock实例会释放锁并且当前线程失去线程调度的能力，   
     * released and the current thread becomes disabled for thread scheduling      休眠直至以下4个事情中的一个发生；（前述操作为原子操作）
     * purposes and lies dormant until <em>one</em> of four things happens:         
     * <ul>                          
     * <li>Some other thread invokes the {@link #signal} method for this           1 其他线程调用这个Condition实例的方法signal
     * {@code Condition} and the current thread happens to be chosen as the        
     * thread to be awakened; or                      
     * <li>Some other thread invokes the {@link #signalAll} method for this        2 signalAll调用
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the          3 其他线程中断当前线程（可中断悬挂线程），
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.                          4 虚假唤醒。
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must       该方法返回之时，就是线程重新获取锁的时候。
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:                                                     如果当前线程；
     * <ul>                                                                            1.进入这个方法时设置了interrupted状态
     * <li>has its interrupted status set on entry to this method; or                  2.线程挂起时可以响应中断的前提下设置了中断
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting                
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's         InterruptedException被抛出并且当前线程的interrupted 状态被清除。
     * interrupted status is cleared. It is not specified, in the first             在第一种情况时，锁被释放之前是否发生中断的测试并不明确。
     * case, whether or not the test for interruption occurs before the lock           ？？？？？？？？？？？？？？？？
     * is released.
     *
     * <p><b>Implementation Considerations</b>                                       实现细节。
     *
     * <p>The current thread is assumed to hold the lock associated with this       调用了该方法的线程默认被认为持有与该Condition相关的锁。
     * {@code Condition} when this method is called.                                由实现类来决定是否这一假设是否成立 且不成立时如何响应。 
     * It is up to the implementation to determine if this is                       典型情况，线程未持有锁的话调用该方法会抛出例如
     * the case and if not, how to respond. Typically, an exception will be          （IllegalMonitorStateException）的异常。
     * thrown (such as {@link IllegalMonitorStateException}) and the                 请写文档说明。
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal         实现类支持 响应中断覆盖掉了响应信号的方法返回。在这种情况下
     * method return in response to a signal. In that case the implementation        确保信号通知另一个线程，如果有的话。
     * must ensure that the signal is redirected to another waiting thread, if        注释：一个处于wait的线程接受到了信号，本来方法就返回了，但同时
     * there is one.                                                                 有一个中断信号，为了响应这个中断线程抛出了InterruptedException
     *                                                                               那这个信号就得去通知别的线程。                                         
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    void await() throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled.                      该方法造成当前线程等待直到被通知。
     *
     * <p>The lock associated with this condition is atomically                       与该条件相关的锁（原子操作）被释放，当前线程暂时失去线程调度的能力
     * released and the current thread becomes disabled for thread scheduling         ，休眠直到一下三个事情中的一个发生。
     * purposes and lies dormant until <em>one</em> of three things happens:    
     * <ul> 
     * <li>Some other thread invokes the {@link #signal} method for this               1.其他线程对这个condition实例调用signal方法，当前线程恰好被选择作为
     * {@code Condition} and the current thread happens to be chosen as the            被唤醒线程
     * thread to be awakened; or                                                         
     * <li>Some other thread invokes the {@link #signalAll} method for this            2.其他线程调用signalAll方法  
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.                              3.虚假唤醒发生。（https://en.wikipedia.org/wiki/Spurious_wakeup）
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must          方法返回之前，线程必须重新获取锁。
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread's interrupted status is set when it enters             当进入这个方法时当前线程的中断状态被设置，或者等待时线程时中断状态的，
     * this method, or it is {@linkplain Thread#interrupt interrupted}                   他将继续等待直到被通知。这个方法最终返回时，中断状态仍然被设置
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     *
     * <p><b>Implementation Considerations</b>                                          实现细节。
     * 
     * <p>The current thread is assumed to hold the lock associated with this           调用了该方法的线程默认被认为持有与该Condition相关的锁。
     * {@code Condition} when this method is called.                                    由实现类来决定是否这一假设是否成立 且不成立时如何响应。 
     * It is up to the implementation to determine if this is                           典型情况，线程未持有锁的话调用该方法会抛出例如
     * the case and if not, how to respond. Typically, an exception will be              （IllegalMonitorStateException）的异常。
     * thrown (such as {@link IllegalMonitorStateException}) and the                     请写文档说明。
     * implementation must document that fact.
     */
    void awaitUninterruptibly();

    /**
     * Causes the current thread to wait until it is signalled or interrupted,         该方法造成当前线程等待直到被通知或者中断，或者超过指定时
     * or the specified waiting time elapses.                                          间。
     *
     * <p>The lock associated with this condition is atomically                        与该Condition实例关联的Lock实例会释放锁并且当前线程失去线程调度的能力，
     * released and the current thread becomes disabled for thread scheduling          休眠直至以下5个事情中的一个发生；（前述操作为原子操作）
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this                1.signal调用并选中了该线程
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this             2.signalAll调用
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the               3.interrupt调用
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or                                       4.超时
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.                               5.虚假唤醒
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must           方法返回之时，就是锁重新获得之日。
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * <p>If the current thread:                                                         若当前线程；
     * <ul> 
     * <li>has its interrupted status set on entry to this method; or                     1.进入方法前设置了中断状态
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting                     2.线程wait状态，但接受到了中断信号 
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's               就会抛出中断异常，同时中断状态会被清除。
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock              第一种情况下，锁释放之前是否会检测中断发生并不明确。
     * is released.                                                                       注释；这句话的意思时 进入wait方法时要释放锁，这个时间点
     *                                                                                           判断线程中断与否并不强制要求，可以由实现类自己决定。
     * <p>The method returns an estimate of the number of nanoseconds                     返回值是剩余的等待时间，若大于0表明在等待过程中等待条件
     * remaining to wait given the supplied {@code nanosTimeout}                          满足了从而提早返回。若小于等于0则表明等待条件并未满足。  
     * value upon return, or a value less than or equal to zero if it                     可以根据返回值来进行下一步的动作。
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:                                                                 典型如下例；
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);                                                先获取锁然后判定进行下一步操作条件是否满足，但只等指定时间。
     *   lock.lock();                                                                       时间到了（即awaitNanos返回值小于0）方法返回false。
     *   try {                                                                              注释：一个广告投放系统，我们想给指定页面加载一个指定的广告，
     *     while (!conditionBeingWaitedFor()) {                                                   但指定时间到了还没有获取到想要加载的广告，就加载默认广告，
     *       if (nanos <= 0L)                                                                     避免等待时间太久用户体验不好，又避免了没有广告导致的减少效益。
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p>Design note: This method requires a nanosecond argument so                              设计说明；
     * as to avoid truncation errors in reporting remaining times.                                    参数用long（即纳秒）是为了尽可能地保证精度。
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     *
     * <p><b>Implementation Considerations</b>                                                    实现细节；
     *
     * <p>The current thread is assumed to hold the lock associated with this                         跟上述几个方法一样，调用该方法时默认持有锁。由实现去决定
     * {@code Condition} when this method is called.                                                  如何处理未持有锁的情况。我们推荐抛出IllegalMonitorStateException
     * It is up to the implementation to determine if this is                                         
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.                                                         写文档说明你的实现类是咋干的
     *                                                                                                注释；文档！文档！文档！！！！！这都第几遍了，作者是多么的看重文档。
     * <p>An implementation can favor responding to an interrupt over normal                        唤醒信号，或者指定的超时时间 被中断给截了是可以的，没问题。不过你要把
     * method return in response to a signal, or over indicating the elapse                          这个signal信号给另一个线程，如果有的话。
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     * @return an estimate of the {@code nanosTimeout} value minus                                      return  剩余时间
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>                                   上一个方法的指定时间单位的版本。
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     *
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * Wakes up one waiting thread.
     *
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signal();

    /**
     * Wakes up all waiting threads.
     *
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    void signalAll();
}
