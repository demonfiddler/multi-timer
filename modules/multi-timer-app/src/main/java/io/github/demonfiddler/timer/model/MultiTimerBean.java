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

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;

import io.github.demonfiddler.timer.util.Constants;
import io.github.demonfiddler.timer.util.TimerUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

/**
 * A JavaFX bean that represents a arbitrary collection of coordinated timers.
 * 
 * @since 1.0
 */
@JsonbPropertyOrder({ "format-version", "interval", "delay-start", "minutes-offset" })
public class MultiTimerBean implements ListChangeListener<TimerBean>, ChangeListener<TimerState> {
	private final IntegerProperty formatVersion = new SimpleIntegerProperty(Constants.FORMAT_VERSION);
	private final BooleanProperty delayStart = new SimpleBooleanProperty();
	private final IntegerProperty minutesOffset = new SimpleIntegerProperty();
	@JsonbTransient
	private final ObjectProperty<TimerState> state = new SimpleObjectProperty<>(TimerState.STOPPED);
	private final ListProperty<TimerBean> timers = new SimpleListProperty<>(FXCollections.observableArrayList());
	@JsonbTransient
	private ScheduledFuture<?> scheduledStart;

	{
		timers.addListener(this);
	}

	/**
	 * Callback invoked when the state of any owned timer state changes. The method
	 * sets the receiver's state to {@link TimerState.RUNNING running} if any of the timers
	 * is running; otherwise to {@link TimerState.STOPPED stopped}.
	 * 
	 * @param observable The state property of the timer bean whose state has
	 *                   changed.
	 * @param oldValue   The previous state value.
	 * @param newValue   The new state value.
	 */
	@Override
	public void changed(ObservableValue<? extends TimerState> observable, TimerState oldValue, TimerState newValue) {
		setState(
				getTimers().stream().anyMatch(t -> t.getState().isRunning()) ? TimerState.RUNNING : TimerState.STOPPED);
	}

	/**
	 * Returns the <code>delayStart</code> property. When this property is set to
	 * <code>true</code> and the bean is {@link #start() started}, it will enter the
	 * {@link TimerState#WAITING waiting} state until the current time reaches the
	 * specified number of {@link #minutesOffsetProperty minutes} past the hour, at
	 * which point it will transition to the {@link TimerState#RUNNING running}
	 * state.
	 * 
	 * @return the delayStart property.
	 */
	public BooleanProperty delayStartProperty() {
		return delayStart;
	}

	/**
	 * Returns the format version property.
	 * @return the format version property.
	 * @see io.github.demonfiddler.timer.util.Constants#FORMAT_VERSION
	 */
	public IntegerProperty formatVersionProperty() {
		return formatVersion;
	}

	/**
	 * Returns the <code>delayStart</code> property value.
	 * 
	 * @return the delayStart property value.
	 * @see #delayStartProperty()
	 */
	public boolean getDelayStart() {
		return delayStart.get();
	}

	/**
	 * Returns the format version.
	 * @return the format version.
	 */
	public int getFormatVersion() {
		return formatVersion.get();
	}

	/**
	 * Returns the <code>minutesOffset</code> property value.
	 * 
	 * @return the minutesOffset property value.
	 * @see #minutesOffsetProperty()
	 */
	public int getMinutesOffset() {
		return minutesOffset.get();
	}

	/**
	 * Returns the <code>state</code> property value.
	 * 
	 * @return the state property value.
	 */
	public TimerState getState() {
		return state.get();
	}

	/**
	 * Returns the <code>timers</code> property. This is a list of the timers owned
	 * by this multi-timer.
	 * 
	 * @return the timers property.
	 * @see #timersProperty()
	 */
	public List<TimerBean> getTimers() {
		return timers.get();
	}

	/**
	 * Handles the addition of timers to this multi-timer instance.
	 * 
	 * @param list A list containing the added timers.
	 * @param from The inclusive start index of the added timers.
	 * @param to   The exclusive end index of the added timers.
	 */
	private void handleTimersAdded(List<? extends TimerBean> list, int from, int to) {
		for (int i = from; i < to; i++)
			list.get(i).stateProperty().addListener(this);
	}

	/**
	 * Handles the addition of timers to this multi-timer instance.
	 * 
	 * @param list A list containing the added timers.
	 * @param from The inclusive start index of the added timers.
	 * @param to   The exclusive end index of the added timers.
	 */
	private void handleTimersRemoved(List<? extends TimerBean> list, int from, int count) {
		for (int i = 0; i < count; i++) {
			TimerBean bean = list.get(i);
			bean.stop();
			bean.stateProperty().removeListener(this);
		}
	}

	/**
	 * Returns the <code>minutesOffset</code> property. When the
	 * {@link #delayStartProperty() delayStart} property is set to <code>true</code>
	 * and the bean is {@link #start() started}, it will enter a
	 * {@link TimerState#WAITING waiting} state until the current time reaches the
	 * number of minutes past the hour specified by this property, at which point it
	 * will transition to the {@link TimerState#RUNNING running} state.
	 * 
	 * @return the minutesOffset property.
	 */
	public IntegerProperty minutesOffsetProperty() {
		return minutesOffset;
	}

	/**
	 * Callback invoked when a timer is added to or removed from this multi-timer
	 * instance.
	 * 
	 * @param c The timer bean change descriptor.
	 */
	@Override
	public void onChanged(Change<? extends TimerBean> c) {
		while (c.next()) {
			if (c.wasAdded()) {
				handleTimersAdded(c.getList(), c.getFrom(), c.getTo());
			} else if (c.wasRemoved()) {
				handleTimersRemoved(c.getRemoved(), c.getFrom(), c.getRemovedSize());
			}
		}
	}

	/**
	 * Starts or stops all the timers owned by this multi-timer instance.
	 * 
	 * @param start <code>true</code> to start the timers, <code>false</code> to
	 *              stop them.
	 */
	public void run(boolean start) {
		if (start)
			start();
		else
			stop();
	}

	/**
	 * Sets the <code>delayStart</code> property value.
	 * 
	 * @param newStartAtOffset The new delayStart property value to set.
	 * @see #delayStartProperty()
	 */
	public void setDelayStart(boolean newStartAtOffset) {
		delayStart.set(newStartAtOffset);
	}

	/**
	 * Sets the format version.
	 * @param  the format version.
	 */
	public void setFormatVersion(int newFormatVersion) {
		formatVersion.set(newFormatVersion);
	}

	/**
	 * Sets the <code>minutesOffset</code> property value.
	 * 
	 * @param newMinutesOffset The new minutesOffset property value to set.
	 * @see #minutesOffsetProperty()
	 */
	public void setMinutesOffset(int newMinutesOffset) {
		minutesOffset.set(newMinutesOffset);
	}

	/**
	 * Sets the <code>state</code> property value.
	 * 
	 * @param newState The new state to set.
	 */
	private void setState(TimerState newState) {
		Objects.requireNonNull(newState, "newState is required");
		state.set(newState);
	}

	/**
	 * Sets the list of timers owned by this multi-timer instance. Note that the
	 * receiver does not take ownership of this list: it merely clears its existing
	 * internal list then adds a reference to each timer to the internal list.
	 * 
	 * @param timers The list of timers to set.
	 * @see #timersProperty()
	 */
	public void setTimers(List<TimerBean> timers) {
		List<TimerBean> timersList = getTimers();
		timersList.clear();
		timersList.addAll(timers);
	}

	/**
	 * Starts all the timers owned by this multi-timer instance, or schedules them
	 * for a delayed start if the <code>delayStart</code> property is set to
	 * <code>true</code>.
	 */
	private void start() {
		if (getDelayStart()) {
			if (scheduledStart == null) {
				// Compute the number of milliseconds past the current hour.
				GregorianCalendar when = new GregorianCalendar();
				long nowMinutes = when.get(GregorianCalendar.MINUTE);
				long nowSeconds = when.get(GregorianCalendar.SECOND);
				long nowMillis = when.get(GregorianCalendar.MILLISECOND);
				long nowMillisPastTheHour = (nowMinutes * 60 + nowSeconds) * 1000 + nowMillis;
				// Compute the time at which we should start the timers.
				when.set(GregorianCalendar.MINUTE, (int) getMinutesOffset());
				when.set(GregorianCalendar.SECOND, 0);
				when.set(GregorianCalendar.MILLISECOND, 0);
				// If the specified offset past the current hour has already elapsed, start the
				// timers at this offset past the next hour instead.
				long startMillisPastTheHour = getMinutesOffset() * 60 * 1000;
				if (startMillisPastTheHour < nowMillisPastTheHour)
					when.add(GregorianCalendar.HOUR_OF_DAY, 1);
				long delay = when.getTimeInMillis() - System.currentTimeMillis();
				scheduledStart = TimerUtils.EXECUTOR.schedule(() -> {
					getTimers().forEach(t -> t.start());
					scheduledStart = null;
				}, delay, TimeUnit.MILLISECONDS);
				getTimers().forEach(t -> t.standby());
				// NOTE: this state change must be applied AFTER placing the timers into standby
				// mode.
				setState(TimerState.WAITING);
			}
		} else {
			getTimers().forEach(t -> t.start());
		}
	}

	/**
	 * Returns the <code>state</code> property.
	 * 
	 * @return the state property.
	 */
	public ObjectProperty<TimerState> stateProperty() {
		return state;
	}

	/**
	 * Stops all the timers owned by this multi-timer instance. The method has no
	 * effect on any timers that are already complete or stopped.
	 */
	private void stop() {
		if (scheduledStart != null) {
			scheduledStart.cancel(true);
			scheduledStart = null;
		}
		getTimers().forEach(t -> t.stop());
	}

	/**
	 * Returns the <code>timers</code> list property.
	 * 
	 * @return the timers list property.
	 */
	public ListProperty<TimerBean> timersProperty() {
		return timers;
	}
}
