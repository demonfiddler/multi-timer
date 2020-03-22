/*
 * Copyright © 2020 Adrian Price. All rights reserved.
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

package io.github.demonfiddler.timer.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Various timer-related utility methodsand objects.
 * @since 1.0
 */
public final class TimerUtils {
	/**
	 * A custom thread factory that creates daemon threads.
	 */
	static final class DaemonThreadFactory implements ThreadFactory {
		private final ThreadFactory delegate = Executors.defaultThreadFactory();

		/** {@inheritDoc} */
		@Override
		public Thread newThread(Runnable r) {
			Thread t = delegate.newThread(r);
			t.setDaemon(true);
			return t;
		}
	}

	/**
	 * A scheduled executor that uses a pool of daemon threads.
	 */
	public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(10,
			new DaemonThreadFactory());


	/** Private ctor prevents instantiation. */
	private TimerUtils() {
	}
}
