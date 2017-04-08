/*
Copyright [2017] [DCS <Info-dcs@diehl.com>]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.github.dcsolutions.kalinka.cluster.zk;

import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class AsyncUtil {

	protected AsyncUtil() {}

	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	private static ConcurrentHashMap<UUID, AssertionError> errorMap = new ConcurrentHashMap<>();

	public static void eventually(final Runnable r) {
		eventually(r, 10000);
	}

	public static void eventually(final Runnable r, final long timeout) {
		eventually(r, timeout, 100);
	}

	public static void eventually(final Runnable r, final long timeout, final long interval) {
		final UUID execution = UUID.randomUUID();
		final long end = System.currentTimeMillis() + timeout;
		long tries = 0;
		long failed = 0;
		boolean success = false;
		while (System.currentTimeMillis() < end) {
			tries++;
			final Future<Void> f = executor.submit(new Invocation(r, execution));
			try {
				f.get(timeout, TimeUnit.MILLISECONDS);
				if (errorMap.containsKey(execution)) {
					throw errorMap.get(execution);
				}
				success = true;
				break;
			} catch (final InterruptedException ex) {
				failed++;
				try {
					Thread.sleep(interval);
				} catch (final InterruptedException ex2) {}
			} catch (final ExecutionException ex) {
				failed++;
				try {
					Thread.sleep(interval);
				} catch (final InterruptedException ex2) {}
			} catch (final TimeoutException ex) {
				failed++;
				try {
					Thread.sleep(interval);
				} catch (final InterruptedException ex2) {}
			} catch (final AssertionError ex) {
				failed++;
				try {
					Thread.sleep(interval);
				} catch (final InterruptedException ex2) {}
			} finally {
				errorMap.remove(execution);
			}
		}
		if (!success) {
			fail("Eventually executed " + tries + " times and failed " + failed + " times after " + timeout + " milliseconds.");
		}
	}

	private static class Invocation implements Callable<Void> {
		private final Runnable task;
		private final UUID exec;

		public Invocation(final Runnable r, final UUID execution) {
			task = r;
			exec = execution;
		}

		@Override
		public Void call() {
			final InvocationUncaughtExceptionHandler handler = new InvocationUncaughtExceptionHandler();
			final Thread t = new Thread(task);
			t.setUncaughtExceptionHandler(handler);
			t.start();
			try {
				t.join();
			} catch (final InterruptedException ex) {}
			if (handler.hadException()) {
				errorMap.put(exec, handler.getException());
			}
			return null;
		}
	}

	private static class InvocationUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		private AssertionError ae;

		@Override
		public void uncaughtException(final Thread t, final Throwable e) {
			if (e.getClass().isAssignableFrom(AssertionError.class)) {
				ae = (AssertionError) e;
			}
		}

		public boolean hadException() {
			return ae != null;
		}

		public AssertionError getException() {
			return ae;
		}
	}

}

