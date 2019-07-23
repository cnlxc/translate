

package java.util.concurrent.locks;
import sun.misc.Unsafe;

/**
 * Basic thread blocking primitives for creating locks and other                   用于创建锁和其他同步类的基本的线程阻塞原语。
 * synchronization classes.
 *
 * <p>This class associates, with each thread that uses it, a permit               这个类和每一个使用它的线程以允许与否的形式关联 （某种意义上与Semaphore类一样）
 * (in the sense of the {@link java.util.concurrent.Semaphore                      如果许可可以获得，对park方法的调用将立即返回。否则可能阻塞。对unpark的调用会
 * Semaphore} class). A call to {@code park} will return immediately               释放许可。（与信号量不同的是这里的许可不会累计，最多只有一个）
 * if the permit is available, consuming it in the process; otherwise
 * it <em>may</em> block.  A call to {@code unpark} makes the permit               注释：我们知道信号量就是提供一个共享资源的许可证，许可证是5，则有5个对象可以
 * available, if it was not already available. (Unlike with Semaphores                同时使用这个资源。该类借鉴这一思想，线程的park和unpark也有一个信号量叫许可，
 * though, permits do not accumulate. There is at most one.)                          一个线程park则许可变为0，unpark则许可又变为1。
 *
 * <p>Methods {@code park} and {@code unpark} provide efficient                     方法park和unpark提供了高效的阻塞线程的方式，不像Thread.suspend和Thread.resume
 * means of blocking and unblocking threads that do not encounter the              那样会造成死锁问题。Thread.suspend和Thread.resume顺序若不对就会死锁，但park和unpark
 * problems that cause the deprecated methods {@code Thread.suspend}               因为许可的原因不会死锁。
 * and {@code Thread.resume} to be unusable for such purposes: Races               参考资料：https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
 * between one thread invoking {@code park} and another thread trying             
 * to {@code unpark} it will preserve liveness, due to the
 * permit. Additionally, {@code park} will return if the caller's                   另外，如果调用者的线程被中断park也将返回，同时也提供超时版本。park方法也许在任何
 * thread was interrupted, and timeout versions are supported. The                  时刻都可能返回，所以通常来说，park必须被在循环中调用，重复检查条件是否成立。
 * {@code park} method may also return at any other time, for "no                   在这个意义上来说，park作为对忙等的一种优化，避免了浪费大量时间的自旋，但是park
 * reason", so in general must be invoked within a loop that rechecks               一定要与unpark配对使用。
 * conditions upon return. In this sense {@code park} serves as an                   
 * optimization of a "busy wait" that does not waste as much time
 * spinning, but must be paired with an {@code unpark} to be
 * effective.
 *
 * <p>The three forms of {@code park} each also support a                             三种形式的park都支持blocker对象参数。这个对象被记录当线程被阻塞，允许监视器和
 * {@code blocker} object parameter. This object is recorded while                    诊断工具标识线程被阻塞的原因。（例如，工具也许通过getBlocker访问blockers）
 * the thread is blocked to permit monitoring and diagnostic tools to                  相比与不带这个参数的原始的形式，我们更推荐带参数的形式。
 * identify the reasons that threads are blocked. (Such tools may
 * access blockers using method {@link #getBlocker(Thread)}.)                           一般使用this对象作为这个blocker参数的值。
 * The use of these forms rather than the original forms without this
 * parameter is strongly encouraged. The normal argument to supply as
 * a {@code blocker} within a lock implementation is {@code this}.
 *
 * <p>These methods are designed to be used as tools for creating                      这些方法用来 作为创建更高级的同步程序的工具，他们本身对并发控制应用并没有用。
 * higher-level synchronization utilities, and are not in themselves                   park方法只再构建如下形式的代码时使用。
 * useful for most concurrency control applications.  The {@code park}
 * method is designed for use only in constructions of the form:
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>                       当不能继续时就park这个线程。
 *
 * where neither {@code canProceed} nor any other actions prior to the                 既不是再canProceed（可以继续 ）时，也不是在意味着阻塞或锁定的park之前。因为
 * call to {@code park} entail locking or blocking.  Because only one                  每一个线程只关联一个许可证，任何对park的使用都可能影响线程的预期效果。
 * permit is associated with each thread, any intermediary uses of
 * {@code park} could interfere with its intended effects.
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out                    用例。这是一个先进先出非重入锁类的伪代码。
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex { 
 *   private final AtomicBoolean locked = new AtomicBoolean(false);                     原子boolean类作为锁
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();                                           同步队列作为存放等待获取锁的线程的队列。
 *
 *   public void lock() { 
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);                                                              直接进队（注释：所以是先进先出），
 *
 *     // Block while not first in queue or cannot acquire lock                          while循环直到
 *     while (waiters.peek() != current ||                                                从队头获取的是当前线程并且cas设置锁的值成功。失败则park当前线程
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();                                                                   循环出来了，证明锁已经拿到，则把当前线程移出等待队列。                                                              
 *     if (wasInterrupted)          // reassert interrupt status on exit                   
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);                                                                     锁释放，
 *     LockSupport.unpark(waiters.peek());                                                    则可以通知等对队列的队头去尝试获取锁了。
 *   }
 * }}</pre>
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * Makes available the permit for the given thread, if it                                对指定线程制造一个可获取的许可，如果这个许可已经不可获得的话。
     * was not already available.  If the thread was blocked on                              如果线程阻塞于park方法，那么调用该方法将使线程非阻塞 。如果被
     * {@code park} then it will unblock.  Otherwise, its next call                          指定的线程没有开始的话，那么不保证起作用。
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * @param thread the thread to unpark, or {@code null}, in which case
     *        this operation has no effect
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the          使得当前线程不能进行线程调度，除非有许可可以获得。
	                                                                                   注释：上记的除非的意思是  线程许可默认是被获取状态的，所以直接调用park会阻塞。
     * permit is available.                                                                 但当你先调用了unpark使得许可变成可以获得了，那这时候调用park也不会阻塞。
     *                                                                                
     * <p>If the permit is available then it is consumed and the call returns         如果许可可以获得，这个方法就会消耗掉这个许可且返回。否则当以下三种情况中的一种出现时，
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling                       才会返回。
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the                       1，其他线程调用了当前线程的unpark方法。
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}                       2，其他线程调用了当前线程的中断方法。
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.                            3.系统虚假返回。（操作系统内部原因。）
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the                    这个方法不会报告到底是哪个原因造成了返回。调用者应该自己重新检查当时造成线程park 
     * method to return. Callers should re-check the conditions which caused                的条件是否还成立。另外，调用者自己决定中断返回的处理方法。
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to                            纳秒版park（park指定时间，时间到了无论怎样都返回）
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param nanos the maximum number of nanoseconds to wait
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * Disables the current thread for thread scheduling purposes, until                           延时版park（直到指定时间都park，时间到了无论怎样方法都返回）
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null                                     Blocker是用来诊断用的。（例如死锁了等）
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * Disables the current thread for thread scheduling purposes unless the                           不带Blocker的park
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.                                    返回伪随机初始化或更新的辅助种子。
     * Copied from ThreadLocalRandom due to package access restrictions.                                      由于包访问限制而从ThreadLocalRandom复制。
     */ 
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
