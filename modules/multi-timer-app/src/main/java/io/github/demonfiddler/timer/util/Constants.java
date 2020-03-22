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

/**
 * Various constants used by the multi-timer application.
 * 
 * @since 1.0
 */
public final class Constants {
	/** The application name (long form). */
	public static final String APP_LONG_NAME = "Quatinus Multi-Timer";
	/** The application name (short form). */
	public static final String APP_SHORT_NAME = "Multi-Timer";
	/** The application version. */
	public static final String APP_VERSION = "1.0.0";
	/** File extension with leading period. */
	public static final String FILE_EXT_DOT_TIMERS = ".timers";
	/** The file format version, used to support migration. */
	public static final int FORMAT_VERSION = 0;
	/** The number of milliseconds in a second. */
	public static final int MILLISECONDS_PER_SECOND = 1000;
	/** The number of minutes in an hour. */
	public static final int MINUTES_PER_HOUR = 60;
	/** The number of seconds in an hour. */
	public static final int SECONDS_PER_HOUR = 60 * 60;
	/** The number of seconds in a minute. */
	public static final int SECONDS_PER_MINUTE = 60;

	/** Private ctor prevents instantiation. */
	private Constants() {
	}
}
