/*
 * Copyright (C) 2024-2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.future0923.debug.tools.base.hutool.core.thread;

import io.github.future0923.debug.tools.base.hutool.core.util.RuntimeUtil;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 线程池工具
 *
 * @author luxiaolei
 */
public class ThreadUtil {

	/**
	 * 新建一个线程池，默认的策略如下：
	 * <pre>
	 *    1. 初始线程数为corePoolSize指定的大小
	 *    2. 没有最大线程数限制
	 *    3. 默认使用LinkedBlockingQueue，默认队列大小为1024
	 * </pre>
	 *
	 * @param corePoolSize 同时执行的线程数大小
	 * @return ExecutorService
	 */
	public static ThreadPoolExecutor newExecutor(int corePoolSize) {
		ExecutorBuilder builder = ExecutorBuilder.create();
		if (corePoolSize > 0) {
			builder.setCorePoolSize(corePoolSize);
		}
		return builder.build();
	}

	/**
	 * 获得一个新的线程池，默认的策略如下：
	 * <pre>
	 *    1. 初始线程数为 0
	 *    2. 最大线程数为Integer.MAX_VALUE
	 *    3. 使用SynchronousQueue
	 *    4. 任务直接提交给线程而不保持它们
	 * </pre>
	 *
	 * @return ExecutorService
	 */
	public static ThreadPoolExecutor newExecutor() {
		return ExecutorBuilder.create().useSynchronousQueue().build();
	}

	/**
	 * 获得一个新的线程池，只有单个线程，策略如下：
	 * <pre>
	 *    1. 初始线程数为 1
	 *    2. 最大线程数为 1
	 *    3. 默认使用LinkedBlockingQueue，默认队列大小为1024
	 *    4. 同时只允许一个线程工作，剩余放入队列等待，等待数超过1024报错
	 * </pre>
	 *
	 * @return ExecutorService
	 */
	public static ExecutorService newSingleExecutor() {
		return ExecutorBuilder.create()//
				.setCorePoolSize(1)//
				.setMaxPoolSize(1)//
				.setKeepAliveTime(0)//
				.buildFinalizable();
	}

	/**
	 * 获得一个新的线程池<br>
	 * 如果maximumPoolSize &gt;= corePoolSize，在没有新任务加入的情况下，多出的线程将最多保留60s
	 *
	 * @param corePoolSize    初始线程池大小
	 * @param maximumPoolSize 最大线程池大小
	 * @return {@link ThreadPoolExecutor}
	 */
	public static ThreadPoolExecutor newExecutor(int corePoolSize, int maximumPoolSize) {
		return ExecutorBuilder.create()
				.setCorePoolSize(corePoolSize)
				.setMaxPoolSize(maximumPoolSize)
				.build();
	}

	/**
	 * 获得一个新的线程池，并指定最大任务队列大小<br>
	 * 如果maximumPoolSize &gt;= corePoolSize，在没有新任务加入的情况下，多出的线程将最多保留60s
	 *
	 * @param corePoolSize     初始线程池大小
	 * @param maximumPoolSize  最大线程池大小
	 * @param maximumQueueSize 最大任务队列大小
	 * @return {@link ThreadPoolExecutor}
	 * @since 5.4.1
	 */
	public static ThreadPoolExecutor newExecutor(int corePoolSize, int maximumPoolSize, int maximumQueueSize) {
		return ExecutorBuilder.create()
				.setCorePoolSize(corePoolSize)
				.setMaxPoolSize(maximumPoolSize)
				.setWorkQueue(new LinkedBlockingQueue<>(maximumQueueSize))
				.build();
	}

	/**
	 * 获得一个新的线程池<br>
	 * 传入阻塞系数，线程池的大小计算公式为：CPU可用核心数 / (1 - 阻塞因子)<br>
	 * Blocking Coefficient(阻塞系数) = 阻塞时间／（阻塞时间+使用CPU的时间）<br>
	 * 计算密集型任务的阻塞系数为0，而IO密集型任务的阻塞系数则接近于1。
	 * <p>
	 * see: <a href="http://blog.csdn.net/partner4java/article/details/9417663">http://blog.csdn.net/partner4java/article/details/9417663</a>
	 *
	 * @param blockingCoefficient 阻塞系数，阻塞因子介于0~1之间的数，阻塞因子越大，线程池中的线程数越多。
	 * @return {@link ThreadPoolExecutor}
	 * @since 3.0.6
	 */
	public static ThreadPoolExecutor newExecutorByBlockingCoefficient(float blockingCoefficient) {
		if (blockingCoefficient >= 1 || blockingCoefficient < 0) {
			throw new IllegalArgumentException("[blockingCoefficient] must between 0 and 1, or equals 0.");
		}

		// 最佳的线程数 = CPU可用核心数 / (1 - 阻塞系数)
		int poolSize = (int) (RuntimeUtil.getProcessorCount() / (1 - blockingCoefficient));
		return ExecutorBuilder.create().setCorePoolSize(poolSize).setMaxPoolSize(poolSize).setKeepAliveTime(0L).build();
	}

	/**
	 * 获取一个新的线程池，默认的策略如下<br>
	 * <pre>
	 *     1. 核心线程数与最大线程数为nThreads指定的大小
	 *     2. 默认使用LinkedBlockingQueue，默认队列大小为1024
	 *     3. 如果isBlocked为{@code true}，当执行拒绝策略的时候会处于阻塞状态，直到能添加到队列中或者被{@link Thread#interrupt()}中断
	 * </pre>
	 *
	 * @param nThreads         线程池大小
	 * @param threadNamePrefix 线程名称前缀
	 * @param isBlocked        是否使用{@link BlockPolicy}策略
	 * @return ExecutorService
	 * @author luozongle
	 * @since 5.8.0
	 */
	public static ThreadPoolExecutor newFixedExecutor(int nThreads, String threadNamePrefix, boolean isBlocked) {
		return newFixedExecutor(nThreads, 1024, threadNamePrefix, isBlocked);
	}

	/**
	 * 获取一个新的线程池，默认的策略如下<br>
	 * <pre>
	 *     1. 核心线程数与最大线程数为nThreads指定的大小
	 *     2. 默认使用LinkedBlockingQueue
	 *     3. 如果isBlocked为{@code true}，当执行拒绝策略的时候会处于阻塞状态，直到能添加到队列中或者被{@link Thread#interrupt()}中断
	 * </pre>
	 *
	 * @param nThreads         线程池大小
	 * @param maximumQueueSize 队列大小
	 * @param threadNamePrefix 线程名称前缀
	 * @param isBlocked        是否使用{@link BlockPolicy}策略
	 * @return ExecutorService
	 * @author luozongle
	 * @since 5.8.0
	 */
	public static ThreadPoolExecutor newFixedExecutor(int nThreads, int maximumQueueSize, String threadNamePrefix, boolean isBlocked) {
		return newFixedExecutor(nThreads, maximumQueueSize, threadNamePrefix,
				(isBlocked ? RejectPolicy.BLOCK : RejectPolicy.ABORT).getValue());
	}

	/**
	 * 获得一个新的线程池，默认策略如下<br>
	 * <pre>
	 *     1. 核心线程数与最大线程数为nThreads指定的大小
	 *     2. 默认使用LinkedBlockingQueue
	 * </pre>
	 *
	 * @param nThreads         线程池大小
	 * @param maximumQueueSize 队列大小
	 * @param threadNamePrefix 线程名称前缀
	 * @param handler          拒绝策略
	 * @return ExecutorService
	 * @author luozongle
	 * @since 5.8.0
	 */
	public static ThreadPoolExecutor newFixedExecutor(int nThreads,
												   int maximumQueueSize,
												   String threadNamePrefix,
												   RejectedExecutionHandler handler) {
		return ExecutorBuilder.create()
				.setCorePoolSize(nThreads).setMaxPoolSize(nThreads)
				.setWorkQueue(new LinkedBlockingQueue<>(maximumQueueSize))
				.setThreadFactory(createThreadFactory(threadNamePrefix))
				.setHandler(handler)
				.build();
	}

	/**
	 * 直接在公共线程池中执行线程
	 *
	 * @param runnable 可运行对象
	 */
	public static void execute(Runnable runnable) {
		GlobalThreadPool.execute(runnable);
	}

	/**
	 * 执行异步方法
	 *
	 * @param runnable 需要执行的方法体
	 * @param isDaemon 是否守护线程。守护线程会在主线程结束后自动结束
	 * @return 执行的方法体
	 */
	public static Runnable execAsync(Runnable runnable, boolean isDaemon) {
		Thread thread = new Thread(runnable);
		thread.setDaemon(isDaemon);
		thread.start();

		return runnable;
	}

	/**
	 * 执行有返回值的异步方法<br>
	 * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
	 *
	 * @param <T>  回调对象类型
	 * @param task {@link Callable}
	 * @return Future
	 */
	public static <T> Future<T> execAsync(Callable<T> task) {
		return GlobalThreadPool.submit(task);
	}

	/**
	 * 执行有返回值的异步方法<br>
	 * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
	 *
	 * @param runnable 可运行对象
	 * @return {@link Future}
	 * @since 3.0.5
	 */
	public static Future<?> execAsync(Runnable runnable) {
		return GlobalThreadPool.submit(runnable);
	}

	/**
	 * 新建一个CompletionService，调用其submit方法可以异步执行多个任务，最后调用take方法按照完成的顺序获得其结果。<br>
	 * 若未完成，则会阻塞
	 *
	 * @param <T> 回调对象类型
	 * @return CompletionService
	 */
	public static <T> CompletionService<T> newCompletionService() {
		return new ExecutorCompletionService<>(GlobalThreadPool.getExecutor());
	}

	/**
	 * 新建一个CompletionService，调用其submit方法可以异步执行多个任务，最后调用take方法按照完成的顺序获得其结果。<br>
	 * 若未完成，则会阻塞
	 *
	 * @param <T>      回调对象类型
	 * @param executor 执行器 {@link ExecutorService}
	 * @return CompletionService
	 */
	public static <T> CompletionService<T> newCompletionService(ExecutorService executor) {
		return new ExecutorCompletionService<>(executor);
	}

	/**
	 * 新建一个CountDownLatch，一个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。
	 *
	 * @param threadCount 线程数量
	 * @return CountDownLatch
	 */
	public static CountDownLatch newCountDownLatch(int threadCount) {
		return new CountDownLatch(threadCount);
	}

	/**
	 * 创建新线程，非守护线程，正常优先级，线程组与当前线程的线程组一致
	 *
	 * @param runnable {@link Runnable}
	 * @param name     线程名
	 * @return {@link Thread}
	 * @since 3.1.2
	 */
	public static Thread newThread(Runnable runnable, String name) {
		final Thread t = newThread(runnable, name, false);
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}

	/**
	 * 创建新线程
	 *
	 * @param runnable {@link Runnable}
	 * @param name     线程名
	 * @param isDaemon 是否守护线程
	 * @return {@link Thread}
	 * @since 4.1.2
	 */
	public static Thread newThread(Runnable runnable, String name, boolean isDaemon) {
		final Thread t = new Thread(null, runnable, name);
		t.setDaemon(isDaemon);
		return t;
	}

	/**
	 * 挂起当前线程
	 *
	 * @param timeout  挂起的时长
	 * @param timeUnit 时长单位
	 * @return 被中断返回false，否则true
	 */
	public static boolean sleep(Number timeout, TimeUnit timeUnit) {
		try {
			timeUnit.sleep(timeout.longValue());
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * 挂起当前线程
	 *
	 * @param millis 挂起的毫秒数
	 * @return 被中断返回false，否则true
	 */
	public static boolean sleep(Number millis) {
		if (millis == null) {
			return true;
		}
		return sleep(millis.longValue());
	}

	/**
	 * 挂起当前线程
	 *
	 * @param millis 挂起的毫秒数
	 * @return 被中断返回false，否则true
	 * @since 5.3.2
	 */
	public static boolean sleep(long millis) {
		if (millis > 0) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 考虑{@link Thread#sleep(long)}方法有可能时间不足给定毫秒数，此方法保证sleep时间不小于给定的毫秒数
	 *
	 * @param millis 给定的sleep时间
	 * @return 被中断返回false，否则true
	 * @see ThreadUtil#sleep(Number)
	 */
	public static boolean safeSleep(Number millis) {
		if (millis == null) {
			return true;
		}

		return safeSleep(millis.longValue());
	}

	/**
	 * 考虑{@link Thread#sleep(long)}方法有可能时间不足给定毫秒数，此方法保证sleep时间不小于给定的毫秒数
	 *
	 * @param millis 给定的sleep时间
	 * @return 被中断返回false，否则true
	 * @see ThreadUtil#sleep(Number)
	 * @since 5.3.2
	 */
	public static boolean safeSleep(long millis) {
		long done = 0;
		long before;
		// done表示实际花费的时间，确保实际花费时间大于应该sleep的时间
		while (done < millis) {
			before = System.nanoTime();
			if (!sleep(millis - done)) {
				return false;
			}
			// done始终为正
			done += (System.nanoTime() - before) / 1_000_000;
		}
		return true;
	}

	/**
	 * @return 获得堆栈列表
	 */
	public static StackTraceElement[] getStackTrace() {
		return Thread.currentThread().getStackTrace();
	}

	/**
	 * 获得堆栈项
	 *
	 * @param i 第几个堆栈项
	 * @return 堆栈项
	 */
	public static StackTraceElement getStackTraceElement(int i) {
		StackTraceElement[] stackTrace = getStackTrace();
		if (i < 0) {
			i += stackTrace.length;
		}
		return stackTrace[i];
	}

	/**
	 * 创建本地线程对象
	 *
	 * @param <T>           持有对象类型
	 * @param isInheritable 是否为子线程提供从父线程那里继承的值
	 * @return 本地线程
	 */
	public static <T> ThreadLocal<T> createThreadLocal(boolean isInheritable) {
		if (isInheritable) {
			return new InheritableThreadLocal<>();
		} else {
			return new ThreadLocal<>();
		}
	}

	/**
	 * 创建本地线程对象
	 *
	 * @param <T>      持有对象类型
	 * @param supplier 初始化线程对象函数
	 * @return 本地线程
	 * @see ThreadLocal#withInitial(Supplier)
	 * @since 5.6.7
	 */
	public static <T> ThreadLocal<T> createThreadLocal(Supplier<? extends T> supplier) {
		return ThreadLocal.withInitial(supplier);
	}

	/**
	 * 创建ThreadFactoryBuilder
	 *
	 * @return ThreadFactoryBuilder
	 * @see ThreadFactoryBuilder#build()
	 * @since 4.1.13
	 */
	public static ThreadFactoryBuilder createThreadFactoryBuilder() {
		return ThreadFactoryBuilder.create();
	}

	/**
	 * 创建自定义线程名称前缀的{@link ThreadFactory}
	 *
	 * @param threadNamePrefix 线程名称前缀
	 * @return {@link ThreadFactory}
	 * @see ThreadFactoryBuilder#build()
	 * @since 5.8.0
	 */
	public static ThreadFactory createThreadFactory(String threadNamePrefix) {
		return ThreadFactoryBuilder.create().setNamePrefix(threadNamePrefix).build();
	}

	/**
	 * 结束线程，调用此方法后，线程将抛出 {@link InterruptedException}异常
	 *
	 * @param thread 线程
	 * @param isJoin 是否等待结束
	 */
	public static void interrupt(Thread thread, boolean isJoin) {
		if (null != thread && false == thread.isInterrupted()) {
			thread.interrupt();
			if (isJoin) {
				waitForDie(thread);
			}
		}
	}

	/**
	 * 等待当前线程结束. 调用 {@link Thread#join()} 并忽略 {@link InterruptedException}
	 */
	public static void waitForDie() {
		waitForDie(Thread.currentThread());
	}

	/**
	 * 等待线程结束. 调用 {@link Thread#join()} 并忽略 {@link InterruptedException}
	 *
	 * @param thread 线程
	 */
	public static void waitForDie(Thread thread) {
		if (null == thread) {
			return;
		}

		boolean dead = false;
		do {
			try {
				thread.join();
				dead = true;
			} catch (InterruptedException e) {
				// ignore
			}
		} while (false == dead);
	}

	/**
	 * 获取JVM中与当前线程同组的所有线程<br>
	 *
	 * @return 线程对象数组
	 */
	public static Thread[] getThreads() {
		return getThreads(Thread.currentThread().getThreadGroup().getParent());
	}

	/**
	 * 获取JVM中与当前线程同组的所有线程<br>
	 * 使用数组二次拷贝方式，防止在线程列表获取过程中线程终止<br>
	 * from Voovan
	 *
	 * @param group 线程组
	 * @return 线程对象数组
	 */
	public static Thread[] getThreads(ThreadGroup group) {
		final Thread[] slackList = new Thread[group.activeCount() * 2];
		final int actualSize = group.enumerate(slackList);
		final Thread[] result = new Thread[actualSize];
		System.arraycopy(slackList, 0, result, 0, actualSize);
		return result;
	}

	/**
	 * 获取进程的主线程<br>
	 * from Voovan
	 *
	 * @return 进程的主线程
	 */
	public static Thread getMainThread() {
		for (Thread thread : getThreads()) {
			if (thread.getId() == 1) {
				return thread;
			}
		}
		return null;
	}

	/**
	 * 获取当前线程的线程组
	 *
	 * @return 线程组
	 * @since 3.1.2
	 */
	public static ThreadGroup currentThreadGroup() {
		final SecurityManager s = System.getSecurityManager();
		return (null != s) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
	}

	/**
	 * 创建线程工厂
	 *
	 * @param prefix   线程名前缀
	 * @param isDaemon 是否守护线程
	 * @return {@link ThreadFactory}
	 * @since 4.0.0
	 */
	public static ThreadFactory newNamedThreadFactory(String prefix, boolean isDaemon) {
		return new NamedThreadFactory(prefix, isDaemon);
	}

	/**
	 * 创建线程工厂
	 *
	 * @param prefix      线程名前缀
	 * @param threadGroup 线程组，可以为null
	 * @param isDaemon    是否守护线程
	 * @return {@link ThreadFactory}
	 * @since 4.0.0
	 */
	public static ThreadFactory newNamedThreadFactory(String prefix, ThreadGroup threadGroup, boolean isDaemon) {
		return new NamedThreadFactory(prefix, threadGroup, isDaemon);
	}

	/**
	 * 创建线程工厂
	 *
	 * @param prefix      线程名前缀
	 * @param threadGroup 线程组，可以为null
	 * @param isDaemon    是否守护线程
	 * @param handler     未捕获异常处理
	 * @return {@link ThreadFactory}
	 * @since 4.0.0
	 */
	public static ThreadFactory newNamedThreadFactory(String prefix, ThreadGroup threadGroup, boolean isDaemon, UncaughtExceptionHandler handler) {
		return new NamedThreadFactory(prefix, threadGroup, isDaemon, handler);
	}

	/**
	 * 阻塞当前线程，保证在main方法中执行不被退出
	 *
	 * @param obj 对象所在线程
	 * @since 4.5.6
	 */
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public static void sync(Object obj) {
		synchronized (obj) {
			try {
				obj.wait();
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * 并发测试<br>
	 * 此方法用于测试多线程下执行某些逻辑的并发性能<br>
	 * 调用此方法会导致当前线程阻塞。<br>
	 * 结束后可调用{@link ConcurrencyTester#getInterval()} 方法获取执行时间
	 *
	 * @param threadSize 并发线程数
	 * @param runnable   执行的逻辑实现
	 * @return {@link ConcurrencyTester}
	 * @since 4.5.8
	 */
	@SuppressWarnings("resource")
	public static ConcurrencyTester concurrencyTest(int threadSize, Runnable runnable) {
		return (new ConcurrencyTester(threadSize)).test(runnable);
	}

	/**
	 * 创建{@link ScheduledThreadPoolExecutor}
	 *
	 * @param corePoolSize 初始线程池大小
	 * @return {@link ScheduledThreadPoolExecutor}
	 * @since 5.5.8
	 */
	public static ScheduledThreadPoolExecutor createScheduledExecutor(int corePoolSize) {
		return new ScheduledThreadPoolExecutor(corePoolSize);
	}

	/**
	 * 开始执行一个定时任务，执行方式分fixedRate模式和fixedDelay模式。<br>
	 * 注意：此方法的延迟和周期的单位均为毫秒。
	 *
	 * <ul>
	 *     <li>fixedRate 模式：以固定的频率执行。每period的时刻检查，如果上个任务完成，启动下个任务，否则等待上个任务结束后立即启动。</li>
	 *     <li>fixedDelay模式：以固定的延时执行。上次任务结束后等待period再执行下个任务。</li>
	 * </ul>
	 *
	 * @param executor              定时任务线程池，{@code null}新建一个默认线程池
	 * @param command               需要定时执行的逻辑
	 * @param initialDelay          初始延迟，单位毫秒
	 * @param period                执行周期，单位毫秒
	 * @param fixedRateOrFixedDelay {@code true}表示fixedRate模式，{@code false}表示fixedDelay模式
	 * @return {@link ScheduledThreadPoolExecutor}
	 * @since 5.5.8
	 */
	public static ScheduledThreadPoolExecutor schedule(ScheduledThreadPoolExecutor executor,
													   Runnable command,
													   long initialDelay,
													   long period,
													   boolean fixedRateOrFixedDelay) {
		return schedule(executor, command, initialDelay, period, TimeUnit.MILLISECONDS, fixedRateOrFixedDelay);
	}

	/**
	 * 开始执行一个定时任务，执行方式分fixedRate模式和fixedDelay模式。
	 *
	 * <ul>
	 *     <li>fixedRate 模式：以固定的频率执行。每period的时刻检查，如果上个任务完成，启动下个任务，否则等待上个任务结束后立即启动。</li>
	 *     <li>fixedDelay模式：以固定的延时执行。上次任务结束后等待period再执行下个任务。</li>
	 * </ul>
	 *
	 * @param executor              定时任务线程池，{@code null}新建一个默认线程池
	 * @param command               需要定时执行的逻辑
	 * @param initialDelay          初始延迟
	 * @param period                执行周期
	 * @param timeUnit              时间单位
	 * @param fixedRateOrFixedDelay {@code true}表示fixedRate模式，{@code false}表示fixedDelay模式
	 * @return {@link ScheduledThreadPoolExecutor}
	 * @since 5.6.5
	 */
	public static ScheduledThreadPoolExecutor schedule(ScheduledThreadPoolExecutor executor,
													   Runnable command,
													   long initialDelay,
													   long period,
													   TimeUnit timeUnit,
													   boolean fixedRateOrFixedDelay) {
		if (null == executor) {
			executor = createScheduledExecutor(2);
		}
		if (fixedRateOrFixedDelay) {
			executor.scheduleAtFixedRate(command, initialDelay, period, timeUnit);
		} else {
			executor.scheduleWithFixedDelay(command, initialDelay, period, timeUnit);
		}

		return executor;
	}
}
