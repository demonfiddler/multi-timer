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

package io.github.demonfiddler.timer.model;

/**
 * Describes all possible timer states.
 * 
 * @since 1.0
 */
public enum TimerState {
	STOPPED, WAITING, RUNNING, WARNING, COMPLETE;

	/**
	 * Returns whether the timer is in a quiescent state. That is to say,
	 * {@link #STOPPED stopped}, {@link #WAITING waiting} or {@link #COMPLETE complete}.
	 * 
	 * @return <code>true</code> if the timer is quiescent.
	 */
	public boolean isQuiescent() {
		return this == STOPPED || this == WAITING || this == COMPLETE;
	}

	/**
	 * Returns whether the timer is in a running state. That is to say,
	 * {@link #RUNNING running} or {@link #WARNING warning}.
	 * 
	 * @return <code>true</code> if the timer is running.
	 */
	public boolean isRunning() {
		return this == RUNNING || this == WARNING;
	}

	/**
	 * Returns whether the timer is in a running or waiting state. That is to say,
	 * {@link #RUNNING running}, {@link #WAITING waiting} or {@link #WARNING warning}.
	 * 
	 * @return <code>true</code> if the timer is running or waiting.
	 */
	public boolean isRunningOrWaiting() {
		return this == RUNNING || this == WAITING || this == WARNING;
	}
}