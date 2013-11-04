package de.knowwe.core.compile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.collections.PriorityList;
import de.d3web.collections.PriorityList.Group;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.plugin.Plugins;

/**
 * This class represents the compile manager for a specific web. It is
 * responsible to manage every compile process for all articles and section of
 * the web. Therefore all compile code has been removed out of sections and
 * articles and placed here.
 * <p>
 * The compile manager holds a set of compilers. The compilers can be plugged
 * into the manager using the defined extension point. Each compiler may
 * implement its own compilation procedure. If the compiler uses the package
 * mechanism to define certain compiling bundles (such as d3web does for
 * knowledge bases or owl for triple stores) the compiler usually have multiple
 * subsequent compilers for each such individual bundle.
 * <p>
 * To enhance performance, each compiler top level compiles individually, maybe
 * in parallel. Nevertheless, if the compilers have different priorities
 * (defined through the compiler's extension), they are ordered by these
 * priorities. Only compilers with same priority may compile in parallel.
 * <p>
 * The compile managers itself can be accessed through the {@link Environment}
 * using {@link Environment#getCompileManager(String)}.
 */
public class CompilerManager {

	private static final Map<Class<? extends Compiler>, ScriptManager<? extends Compiler>> scriptManagers = new HashMap<Class<? extends Compiler>, ScriptManager<? extends Compiler>>();

	private final PriorityList<Double, Compiler> compilers;
	private final String web;
	private Iterator<Group<Double, Compiler>> running = null;
	private final ExecutorService threadPool;

	public CompilerManager(String web) {
		this.web = web;
		this.compilers = Plugins.getCompilers();
		for (Compiler compiler : compilers) {
			compiler.init(this);
		}
		ExecutorService pool = createExecutorService();
		this.threadPool = pool;
	}

	private static ExecutorService createExecutorService() {
		int threadCount = Runtime.getRuntime().availableProcessors() * 3 / 2 + 1;
		ExecutorService pool = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "wiki-compiler");
				return thread;
			}
		});
		Logger.getLogger(CompilerManager.class.getName())
				.fine("created multicore thread pool of size " + threadCount);
		return pool;
	}

	@SuppressWarnings("unchecked")
	public static <C extends Compiler> ScriptManager<C> getScriptManager(C compiler) {
		return (ScriptManager<C>) getScriptManager(compiler.getClass());
	}

	public static <C extends Compiler> ScriptManager<C> getScriptManager(Class<C> compilerClass) {
		@SuppressWarnings("unchecked")
		ScriptManager<C> result = (ScriptManager<C>) scriptManagers.get(compilerClass);
		if (result == null) {
			result = new ScriptManager<C>(compilerClass);
			scriptManagers.put(compilerClass, result);
		}
		return result;
	}

	public static <C extends Compiler, T extends Type> void addScript(T type, CompileScript<C, T> script) {
		addScript(Priority.DEFAULT, type, script);
	}

	public static <C extends Compiler, T extends Type> void addScript(Priority priority, T type, CompileScript<C, T> script) {
		getScriptManager(script.getCompilerClass()).addScript(priority, type, script);
	}

	/**
	 * Returns the web this compiler belongs to.
	 * 
	 * @created 30.10.2013
	 * @return the web of this compiler
	 */
	public String getWeb() {
		return web;
	}

	/**
	 * Starts the compilation based on a specified set of changing sections. The
	 * method returns true if the compilation can be started. The method returns
	 * false if the request is ignored, e.g. because of an already ongoing
	 * compilation.
	 * 
	 * @created 30.10.2013
	 * @return if the compilation has been started
	 */
	public boolean startCompile(final Collection<Section<?>> added, final Collection<Section<?>> removed) {
		synchronized (this) {
			if (isCompiling()) return false;
			running = compilers.groupIterator();
		}
		threadPool.execute(new Runnable() {

			@Override
			public void run() {
				try {
					doCompile(added, removed);
				}
				catch (Throwable e) {
					Logger.getLogger(CompilerManager.class.getName()).log(Level.SEVERE,
							"unexpected internal error starting compile", e);
				}
				finally {
					synchronized (this) {
						running = null;
						this.notifyAll();
					}
				}
			}
		});
		return true;
	}

	private void doCompile(final Collection<Section<?>> added, final Collection<Section<?>> removed) throws InterruptedException {
		while (true) {
			// get the current compilers
			List<Compiler> simultaneousCompilers;
			synchronized (this) {
				if (!running.hasNext()) break;
				Group<Double, Compiler> group = running.next();
				simultaneousCompilers = group.getElements();
			}

			// start all simultaneous compilers and
			// observe the active ones until they all have terminated
			final Set<Compiler> activeCompilers = new LinkedHashSet<Compiler>(simultaneousCompilers);
			for (final Compiler compiler : simultaneousCompilers) {
				// wait until we are allowed to compile
				threadPool.execute(new Runnable() {

					@Override
					public void run() {
						try {
							// compile the content
							compiler.compile(added, removed);
						}
						catch (Throwable e) {
							Logger.getLogger(CompilerManager.class.getName()).log(Level.SEVERE,
									"unexpected internal error while compiling with " + compiler,
									e);
						}
						finally {
							// and notify that the compiler has finished
							synchronized (this) {
								activeCompilers.remove(compiler);
								this.notifyAll();
							}
						}
					}
				});
			}

			// we wait until all have been terminated
			synchronized (this) {
				while (!activeCompilers.isEmpty())
					this.wait();
			}
		}
	}

	/**
	 * Returns if this compiler manager is currently compiling any changes. You
	 * may use {@link #awaitTermination()} or {@link #awaitTermination(long)} to
	 * wait for the compilation to complete.
	 * 
	 * @created 30.10.2013
	 * @return if a compilation is ongoing
	 */
	public synchronized boolean isCompiling() {
		return running != null;
	}

	/**
	 * Returns the priority-sorted list of compilers that are currently defined
	 * for the web this CompilerManager is created for.
	 * 
	 * @created 31.10.2013
	 * @return the currently defined compilers
	 */
	public List<Compiler> getCompilers() {
		return Collections.unmodifiableList(compilers);
	}

	/**
	 * Adds a new compiler with the specific priority.
	 * <p>
	 * Please not that it is allowed that compilers are added and removed while
	 * compiling the wiki. Usually a more prioritized compiler may add or remove
	 * sub-sequential Compilers depending on specific markups, e.g. defining a
	 * knowledge base or triple store for specific package combination to be
	 * compiled.
	 * 
	 * @created 31.10.2013
	 * @param priority the priority of the compiler
	 * @param compiler the instance to be added
	 */
	public void addCompiler(double priority, Compiler compiler) {
		// debug code: check that we only add items
		// that not already have been added
		if (compilers.contains(compiler)) {
			throw new IllegalStateException("do not add equal compilers instances multiple times");
		}
		// add the compiler, being thread-save
		synchronized (this) {
			compilers.add(priority, compiler);
			compiler.init(this);
		}
	}

	/**
	 * Removes an existing compiler with the specific priority.
	 * <p>
	 * Please not that it is allowed that compilers are added and removed while
	 * compiling the wiki. Usually a more prioritized compiler may add or remove
	 * sub-sequential Compilers depending on specific markups, e.g. defining a
	 * knowledge base or triple store for specific package combination to be
	 * compiled.
	 * 
	 * @created 31.10.2013
	 * @param compiler the instance to be removed
	 */
	public void removeCompiler(Compiler compiler) {
		// debug code: check that we only remove items
		// that already have been added
		if (!compilers.contains(compiler)) {
			throw new NoSuchElementException("removeing non-exisitng compiler instance");
		}
		// remove the compiler, being thread-save
		synchronized (this) {
			compilers.remove(compiler);
		}
	}

	/**
	 * Blocks until all compilers have completed after a compile request, or the
	 * current thread is interrupted, whichever happens first.
	 * 
	 * @param unit the time unit of the timeout argument
	 * @throws InterruptedException if interrupted while waiting
	 * @see #compile
	 */
	public void awaitTermination() throws InterruptedException {
		// repeatedly wait 10 seconds until all compiles have been completed
		while (!awaitTermination(10000)) {
		}
	}

	/**
	 * Blocks until all compilers have completed after a compile request, or the
	 * timeout occurs, or the current thread is interrupted, whichever happens
	 * first.
	 * 
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return <tt>true</tt> if the compilation has finished and <tt>false</tt>
	 *         if the timeout elapsed before termination
	 * @throws InterruptedException if interrupted while waiting
	 * @see #compile
	 */
	public boolean awaitTermination(long timeout) throws InterruptedException {
		long endTime = System.currentTimeMillis() + timeout;
		synchronized (this) {
			while (true) {
				// if every compile has terminated within the specified time,
				// return true
				if (running == null) return true;

				// but reduce the timeout by the time the previous compiler has
				// required to complete, and stop if time has been elapsed.
				long remainingTime = endTime - System.currentTimeMillis();
				if (remainingTime <= 0) return false;
				this.wait(remainingTime);
			}
		}
	}
}