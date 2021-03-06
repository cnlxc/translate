

/*
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;

/**
 * {@code Lock} implementations provide more extensive locking                     相比synchronized方法和声明，Lock接口的实现提供更加可扩展的锁操作
 * operations than can be obtained using {@code synchronized} methods              Lock可以有更灵活的结构，有非常不同的属性，也支持多关联的Condition
 * and statements.  They allow more flexible structuring, may have                 对象(相对于传统的wait和notify而言)           
 * quite different properties, and may support multiple associated                            
 * {@link Condition} objects.                                                                 
 *
 * <p>A lock is a tool for controlling access to a shared resource by              Lock是用来控制多线程访问共享资源的工具。一般而言，一个锁提供独占访问
 * multiple threads. Commonly, a lock provides exclusive access to a               共享资源的机制:一个时刻只能有一个线程可以访问共享资源，且共享资源的访
 * shared resource: only one thread at a time can acquire the lock and             问需要首先获取锁。然而，一些锁也允许并发访问共享资源，例如ReadWriteLock
 * all access to the shared resource requires that the lock be                     的读锁。
 * acquired first. However, some locks may allow concurrent access to             
 * a shared resource, such as the read lock of a {@link ReadWriteLock}.            
 *                                                                                 
 * <p>The use of {@code synchronized} methods or statements provides               传统synchronized方法和声明的使用隐含了与对象关联的监视器锁。
 * access to the implicit monitor lock associated with every object, but           强制锁的获取和释放以代码块的结构：当多个锁被获取时他们必须以相反的顺序释放
 * forces all lock acquisition and release to occur in a block-structured way:     并且全部的锁必须在和获取时相同的代码范围内释放。
 * when multiple locks are acquired they must be released in the opposite          
 * order, and all locks must be released in the same lexical scope in which        
 * they were acquired.                                                             
 *
 * <p>While the scoping mechanism for {@code synchronized} methods                 虽然synchronized使得编程更加容易，并且避免了普遍的与锁相关的错误，不过
 * and statements makes it much easier to program with monitor locks,              有时候我们需要更灵活的方式来使用锁。
 * and helps avoid many common programming errors involving locks,                 例如，一些多线程并发遍历数据的算法要求hand-over-hand或者链式的形式使用
 * there are occasions where you need to work with locks in a more                 锁：获得A节点的锁，接下来B节点，然后释放A节点的锁，并获取C节点的锁，再然后
 * flexible way. For example, some algorithms for traversing                       释放B，获取D以此类推。Lock接口的实现允许锁的获取和释放在不同的代码范围
 * concurrently accessed data structures require the use of                        （其实就是一对花括号），而且允许以任何顺序获取释放锁。
 * &quot;hand-over-hand&quot; or &quot;chain locking&quot;: you                    
 * acquire the lock of node A, then node B, then release A and acquire             
 * C, then release B and acquire D and so on.  Implementations of the              
 * {@code Lock} interface enable the use of such techniques by                     
 * allowing a lock to be acquired and released in different scopes,                
 * and allowing multiple locks to be acquired and released in any                  
 * order.                                                                          
 *
 * <p>With this increased flexibility comes additional                             灵活性的获取意味着程序员要负担更多的责任。块结构的缺失
 * responsibility. The absence of block-structured locking removes the             导致不能像synchronized那样自动释放锁。大多数情况下，应该像下面这样
 * automatic release of locks that occurs with {@code synchronized}                
 * methods and statements. In most cases, the following idiom                      
 * should be used:                                                                 
 *
 *  <pre> {@code                                                                    
 * Lock l = ...;                                                                    Lock l = ...; 
 * l.lock();                                                                        l.lock();
 * try {                                                                            try {
 *   // access the resource protected by this lock                                    // access the resource protected by this lock
 * } finally {                                                                      } finally {
 *   l.unlock();                                                                      l.unlock();
 * }}</pre>                                                                         }
 *
 * When locking and unlocking occur in different scopes, care must be               当锁的获取和释放不在同一范围时，一定要注意确保代码在持有锁时被执行，
 * taken to ensure that all code that is executed while the lock is                 try-finally或者try-catch确保锁被释放。
 * held is protected by try-finally or try-catch to ensure that the                 
 * lock is released when necessary.                                                 
 *
 * <p>{@code Lock} implementations provide additional functionality                Lock相比于synchronized的同步块实现更多的功能。tryLock尝试获取锁；
 * over the use of {@code synchronized} methods and statements by                  lockInterruptibly中断获取锁，tryLock(long,Timeunit)可超时获取锁。
 * providing a non-blocking attempt to acquire a lock ({@link                      
 * #tryLock()}), an attempt to acquire the lock that can be                        
 * interrupted ({@link #lockInterruptibly}, and an attempt to acquire              
 * the lock that can timeout ({@link #tryLock(long, TimeUnit)}).                   
 *
 * <p>A {@code Lock} class can also provide behavior and semantics                 Lock也提供了与内置监视器锁非常不同的行为和语义，例如保证顺序（没理解）
 * that is quite different from that of the implicit monitor lock,                 非重入；死锁检测。如果Lock的实现提供了这些功能的话，一定要写文档进行注释。
 * such as guaranteed ordering, non-reentrant usage, or deadlock                    
 * detection. If an implementation provides such specialized semantics              
 * then the implementation must document those semantics.                           
 *                                                                                   
 * <p>Note that {@code Lock} instances are just normal objects and can              注意Lock实例仅仅是普通的对象，他们亦可以作为synchronized声明的目标。
 * themselves be used as the target in a {@code synchronized} statement.             获取一个Lock对象的监视器锁与这个Lock对象的lock方法没有明确的关系。
 * Acquiring the                                                                    为了避免混乱，不要使用这个方式， except within their own implementation.  
 * monitor lock of a {@code Lock} instance has no specified relationship            （没理解）
 * with invoking any of the {@link #lock} methods of that instance.                 
 * It is recommended that to avoid confusion you never use {@code Lock}             
 * instances in this way, except within their own implementation.                   
 *                                                                                   
 * <p>Except where noted, passing a {@code null} value for any                       除非特别声明，任何参数传递null值都将抛出NullPointerException。
 * parameter will result in a {@link NullPointerException} being                     
 * thrown.                                                                           
 *                                                                                   
 * <h3>Memory Synchronization</h3>                                                   内存同步。
 *                                                                                   
 * <p>All {@code Lock} implementations <em>must</em> enforce the same                全部的Lock实现必须和内置监视器有相同的内存同步语义，如下描述。
 * memory synchronization semantics as provided by the built-in monitor              https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4
 * lock, as described in                                                             The Java Language Specification (17.4 Memory Model)
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4"> 
 * The Java Language Specification (17.4 Memory Model)</a>:                          
 * <ul>                                                                              
 * <li>A successful {@code lock} operation has the same memory                       
 * synchronization effects as a successful <em>Lock</em> action.                     
 * <li>A successful {@code unlock} operation has the same                            
 * memory synchronization effects as a successful <em>Unlock</em> action.            
 * </ul>                                                                             
 *
 * Unsuccessful locking and unlocking operations, and reentrant
 * locking/unlocking operations, do not require any memory
 * synchronization effects.
 *
 * <h3>Implementation Considerations</h3>                                              实现细节。                  
 *
 * <p>The three forms of lock acquisition (interruptible,                              三种锁的获取形式（中断，非中断，超时）在行为特性上，顺序保证上各有不同
 * non-interruptible, and timed) may differ in their performance                       进一步，有些Lock实现没有中断锁获取的能力。所以，一个Lock实现不要求定义
 * characteristics, ordering guarantees, or other implementation                       全部三种形式的准确语义，也不会要求一定支持中断锁的获取。
 * qualities.  Further, the ability to interrupt the <em>ongoing</em>                  一个锁的实现类只要求对它的每一个方法有明确的文档记录。它也必须遵循在
 * acquisition of a lock may not be available in a given {@code Lock}                  这个接口定义的中断语义，例如对于锁获取的中断支持程度就有完全支持或者只是部分
 * class.  Consequently, an implementation is not required to define                   方法支持。
 * exactly the same guarantees or semantics for all three forms of                     
 * lock acquisition, nor is it required to support interruption of an                  
 * ongoing lock acquisition.  An implementation is required to clearly                 
 * document the semantics and guarantees provided by each of the                       
 * locking methods. It must also obey the interruption semantics as                    
 * defined in this interface, to the extent that interruption of lock                  
 * acquisition is supported: which is either totally, or only on                       
 * method entry.                                                                       
 *                                                                                     
 * <p>As interruption generally implies cancellation, and checks for                   中断一般意味着取消，中断判断也不频繁，可以通过方法返回值来响应中断。中断可以
 * interruption are often infrequent, an implementation can favor responding           在另一个方法已经使线程不阻塞的时候发生，这是没问题的。但文档要记录这种情况。
 * to an interrupt over normal method return. This is true even if it can be           
 * shown that the interrupt occurred after another action may have unblocked           
 * the thread. An implementation should document this behavior.                        
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * Acquires the lock.                                                       获取锁
     *                                                                          
     * <p>If the lock is not available then the current thread becomes          如果锁不可获得则线程休眠直到锁被获取。
     * disabled for thread scheduling purposes and lies dormant until the       
     * lock has been acquired.                                                  
     *                                                                          
     * <p><b>Implementation Considerations</b>                                  实现注意事项
     *                                                                          
     * <p>A {@code Lock} implementation may be able to detect erroneous use     一个锁实现能够检测到错误的使用，例如可能造成死锁的调用，也许在这种情况下
     * of the lock, such as an invocation that would cause deadlock, and        会抛出为受检异常。实现类必须记录各种情况及异常类型。
     * may throw an (unchecked) exception in such circumstances.  The           
     * circumstances and the exception type must be documented by that          
     * {@code Lock} implementation.                                            
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is                           除非线程中断否则获取锁。
     * {@linkplain Thread#interrupt interrupted}.                               
     *                                                                          
     * <p>Acquires the lock if it is available and returns immediately.         如果可以则立即获取锁并返回。若果不可以则当前线程挂起直到一下两件事中的一件
     *                                                                          发生：
     * <p>If the lock is not available then the current thread becomes               当前线程获取到锁。
     * disabled for thread scheduling purposes and lies dormant until                其他线程中断了当前线程，并且终端响应被支持。
     * one of two things happens:                                               
     *                                                                          
     * <ul>                                                                     
     * <li>The lock is acquired by the current thread; or                       
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the       
     * current thread, and interruption of lock acquisition is supported.       
     * </ul>                                                                    
     *
     * <p>If the current thread:                                                如果当前线程支持中断的话：
     * <ul>
     * <li>has its interrupted status set on entry to this method; or            那么当进入这个方法时中断状态被设定，或者获取锁时线程是interrupted的，
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the       则当前线程抛出InterruptedException并且线程的中断状态会被清除。
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>                                实现注意事项
     *
     * <p>The ability to interrupt a lock acquisition in some                 响应中断的获取锁能力也许在一些实现类中无法做到，或者虽然能够做到但是代价昂贵。
     * implementations may not be possible, and if possible may be an         程序员应该知道什么时候会发生这种情况，并且在实现类应记述这种情况。
     * expensive operation.  The programmer should be aware that this         
     * may be the case. An implementation should document when this is        
     * the case.                                                              
     *                                                                        
     * <p>An implementation can favor responding to an interrupt over         实现类可以通过方法返回值的方式支持中断响应
     * normal method return.                                                  
     *                                                                        
     * <p>A {@code Lock} implementation may be able to detect                 一个锁的实现类能够检测到错误的使用，例如可能造成死锁的调用，也许在这种情况下
     * erroneous use of the lock, such as an invocation that would            会抛出为受检异常。实现类必须记录各种情况及异常类型。
     * cause deadlock, and may throw an (unchecked) exception in such         
     * circumstances.  The circumstances and the exception type must          
     * be documented by that {@code Lock} implementation.                     
     *
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.         只有在调用该方法时是能够获取的情况下才能拿到锁。
     *
     * <p>Acquires the lock if it is available and returns immediately         能够立即拿到锁则返回true，
     * with the value {@code true}.
     * If the lock is not available then this method will return               不能立即拿到则返回false。
     * immediately with the value {@code false}.
     *
     * <p>A typical usage idiom for this method would be:                       典型的使用方法：
     *  <pre> {@code                                                            Lock lock = ...;
     * Lock lock = ...;                                                         if (lock.tryLock()) {
     * if (lock.tryLock()) {                                                      try {
     *   try {                                                                      // manipulate protected state
     *     // manipulate protected state                                          } finally {
     *   } finally {                                                                lock.unlock();
     *     lock.unlock();                                                         }
     *   }                                                                      } else {
     * } else {                                                                   // perform alternative actions
     *   // perform alternative actions                                         }}
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and     这种使用方法确保锁在获取后会被释放，以及在没有获取锁的时候不会去调用unlock()释放锁。
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the    在被给的时间内锁被释放并且未发生线程中断则获取锁。
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the lock is available this method returns immediately              获取锁之后立即返回true。
     * with the value {@code true}.
     * If the lock is not available then                                         不能获取锁的话，则线程挂起直到以下三件事中的一件发生：
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or                       当前线程获取到锁；其他线程中断了当前线程；被指定的时间用尽。
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned.       若获得锁的话返回true。
     *
     * <p>If the current thread:                                                 如果当前线程支持中断的话：
     * <ul>                                                                      
     * <li>has its interrupted status set on entry to this method; or             那么要么进入这个方法时中断状态被设定，要么获取锁时线程是interrupted的， 
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring            当前线程就会抛出InterruptedException并且线程的中断状态会被清除。
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}     如果指定的时间经过了还没拿到锁则返回false。
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.              指定时间若小于等于0，方法将不会等待。注：相当于tryLock()
     *
     * <p><b>Implementation Considerations</b>                                   实现注意事项
     *
     * <p>The ability to interrupt a lock acquisition in some implementations     响应中断的获取锁能力也许在一些实现类中无法做到，或者虽然能够做到但是代价昂贵。
     * may not be possible, and if possible may                                   程序员应该知道什么时候会发生这种情况，并且在实现类应记述这种情况。
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * <p>An implementation can favor responding to an interrupt over normal        可以通过方法返回值来表明中断，或者表明超时。
     * method return, or reporting a timeout.
     *
     * <p>A {@code Lock} implementation may be able to detect                      一个锁的实现类能够检测到错误的使用，例如可能造成死锁的调用，也许在这种情况下
     * erroneous use of the lock, such as an invocation that would cause           会抛出为受检异常。实现类必须记录各种情况及异常类型。
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.                                                            释放锁。
     *
     * <p><b>Implementation Considerations</b>                                       注意事项
     *
     * <p>A {@code Lock} implementation will usually impose                          一个锁的实现类通常会强加一些释放锁的限制。（例如只有持有锁的线程可以释放锁）      
     * restrictions on which thread can release a lock (typically only the            并且会在违反限制时抛出（未受检）异常。同样，实现类必须记述这些限制和异常类型。
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    void unlock();

    /**
     * Returns a new {@link Condition} instance that is bound to this                 返回一个与该Lock实例绑定的Condition实例。
     * {@code Lock} instance.
     *
     * <p>Before waiting on the condition the lock must be held by the                 condition的等待发生之前锁必须被当前线程持有，在开始wait之前对Condition#await()
     * current thread.                                                                 方法的调用将自动释放锁，wait返回之前重新获得锁。 注：？？wait究竟时指什么
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>                                        实现注意事项
     *
     * <p>The exact operation of the {@link Condition} instance depends on             如何准确的操作Condition实例依赖于Lock的实现类，同样，要记录于文档中。
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
