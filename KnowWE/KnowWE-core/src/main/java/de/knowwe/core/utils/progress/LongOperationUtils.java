package de.knowwe.core.utils.progress;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.utils.KnowWEUtils;

public class LongOperationUtils {

	private static ExecutorService threadPool;

	public static ExecutorService getThreadPool() {
		// initialize thread pool if not exists
		if (threadPool == null) {
			int threadCount = Runtime.getRuntime().availableProcessors() * 3 / 2;
			threadPool = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					Thread thread = new Thread(r, "kpi-calculation");
					thread.setPriority(Thread.MIN_PRIORITY);
					return thread;
				}
			});
			Logger.getLogger(LongOperationUtils.class.getName())
					.fine("created multicore thread pool of size " + threadCount);
		}
		// and return new executor based on the thread pool
		return threadPool;
	}

	/**
	 * Checks if the operation of the current thread has been interrupted (e.g.
	 * aborted by the user) through {@link AjaxProgressListener#cancel()}. If
	 * so, an {@link InterruptedException} is thrown and the threads interrupt
	 * state is restored.
	 * 
	 * @created 30.07.2013
	 * @throws InterruptedException
	 */
	public static void checkCancel() throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
	}

	/**
	 * Adds / registers a potential long operation to a specific section.
	 * 
	 * @created 30.07.2013
	 * @param section the section to add the operatiopn for
	 * @param operation the operation to be added
	 * @return an identifier to be used to access the registered operation
	 * @see #getLongOperation(Section, String)
	 */
	public static String registerLongOperation(Section<?> section, LongOperation operation) {
		String key = getRegistrationID(section, operation);
		if (key != null) return key;

		Map<String, LongOperation> map = accessLongOperations(section, true);
		key = UUID.randomUUID().toString();
		map.put(key, operation);
		return key;
	}

	/**
	 * Returns the id of an operation being registered to a specific section.
	 * This method returns null if the operation is not registered to the
	 * section.
	 * 
	 * @created 30.07.2013
	 * @param section the section to check the operations for
	 * @param operation the operation to get the id for
	 * @return the registered id
	 */
	public static String getRegistrationID(Section<?> section, LongOperation operation) {
		Map<String, LongOperation> map = accessLongOperations(section, true);
		for (Entry<String, LongOperation> entry : map.entrySet()) {
			if (entry.getValue().equals(operation)) return entry.getKey();
		}
		return null;
	}

	/**
	 * Returns a list of operations that are registered as potential operations
	 * for a specific section. This method always returns a list, potentially
	 * empty. it never returns null.
	 * 
	 * @created 30.07.2013
	 * @param section the section to get the operations for
	 * @return the list of operations
	 */
	public static Collection<LongOperation> getLongOperations(Section<?> section) {
		return Collections.unmodifiableCollection(accessLongOperations(section, false).values());
	}

	/**
	 * Returns an operation that is registered as potential operations for a
	 * specific section with the specified name. if no such operation exists,
	 * null is returned.
	 * 
	 * @created 30.07.2013
	 * @param section the section to get the operations for
	 * @param operationID the id of the requested operation
	 * @return the operations
	 */
	public static LongOperation getLongOperation(Section<?> section, String operationID) {
		return accessLongOperations(section, false).get(operationID);
	}

	private static Map<String, LongOperation> accessLongOperations(Section<?> section, boolean create) {
		String key = LongOperation.class.getName();
		@SuppressWarnings("unchecked")
		Map<String, LongOperation> storedObject =
				(Map<String, LongOperation>) KnowWEUtils.getStoredObject(section, key);
		if (storedObject == null) {
			if (!create) return Collections.emptyMap();
			storedObject = new LinkedHashMap<String, LongOperation>();
			KnowWEUtils.storeObject(section, key, storedObject);
		}
		return storedObject;
	}

	/**
	 * Starts the given {@link LongOperation} in its own thread.
	 * 
	 * @created 13.09.2013
	 * @param context the context of the user requesting the start of the
	 *        operation
	 * @param operation the operation to be started
	 * @param doFinally a function that will be called after the operation is
	 *        done (can be null if there is nothing to do)
	 */
	public static void startLongOperation(final UserActionContext context, final LongOperation operation) throws IOException {

		operation.before(context);

		final AjaxProgressListener listener =
				ProgressListenerManager.getInstance().createProgressListener(context, operation);

		new Thread("long-operation-worker") {

			@Override
			public void run() {
				try {
					operation.execute(listener);
				}
				catch (IOException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING,
							"cannot complete operation", e);
					listener.setError("Error occured: " + e.getMessage() + ".");
				}
				catch (InterruptedException e) {
					Logger.getLogger(getClass().getName()).log(Level.INFO,
							"operation canceled by user");
					listener.setError("Canceled by user.");
				}
				catch (Throwable e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"cannot complete operation, unexpected internal error", e);
					listener.setError("Unexpected internal error: " + e.getMessage() + ".");
				}
				finally {
					listener.setRunning(false);
					operation.doFinally();
				}
			}
		}.start();
	}

}
