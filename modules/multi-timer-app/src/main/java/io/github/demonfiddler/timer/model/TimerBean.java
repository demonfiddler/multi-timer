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

import static io.github.demonfiddler.timer.util.Constants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;

import io.github.demonfiddler.timer.util.TimerUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * A JavaFX bean that implements a single timer.
 * 
 * @since 1.0
 */
@JsonbPropertyOrder({ "name", "interval", "warn-after", "repeat" })
public class TimerBean {
	/**
	 * A runnable object that is called for each tick while the timer is running.
	 */
	private class Ticker implements Runnable {
		/** {@inheritDoc} */
		@Override
		public void run() {
			switch (getState()) {
			case STOPPED:
			case COMPLETE:
			case WAITING:
				break;
			case RUNNING:
			case WARNING:
				final long now = System.currentTimeMillis();
				final long remainingMillis = Math.max(finishTime - now, 0);
				final long intervalMillis = getIntervalMillis();
				// Bound properties must be updated on the UI thread.
				Platform.runLater(() -> {
					setProgress(1.0 - ((double) remainingMillis / intervalMillis));
					setRemainingMillis(remainingMillis);
					if (now >= finishTime) {
						setState(TimerState.COMPLETE);
						if (getRepeat()) {
							setState(TimerState.RUNNING);
							warningTime += intervalMillis;
							finishTime += intervalMillis;
						} else {
							cleanup();
						}
					} else if (now >= warningTime) {
						setState(TimerState.WARNING);
					}
				});
				break;
			}
		}
	}

	public static final String PROP_NAME = "name";
	public static final String PROP_INTERVAL = "interval";
	public static final String PROP_WARN_AFTER = "warnAfter";
	public static final String PROP_REPEAT = "repeat";
	public static final String PROP_STATE = "state";
	public static final String PROP_PROGRESS = "progress";
	public static final String PROP_REMAINING_MILLIS = "remainingMillis";

	private final StringProperty name = new SimpleStringProperty(this, PROP_NAME, "(unnamed)");
	@JsonbTransient
	private final ObjectProperty<Duration> intervalDuration = new SimpleObjectProperty<>(new Duration());
	@JsonbTransient
	private final ObjectProperty<Duration> warnAfterDuration = new SimpleObjectProperty<>(new Duration());
	@JsonbTransient
	private final IntegerProperty intInterval;
	@JsonbProperty("interval")
	private final StringProperty isoInterval;
	@JsonbTransient
	private final IntegerProperty intWarnAfter;
	@JsonbProperty("warn-after")
	private final StringProperty isoWarnAfter;
	@JsonbTransient
	private final LongProperty remainingMillis = new SimpleLongProperty(this, PROP_REMAINING_MILLIS);
	private final BooleanProperty repeat = new SimpleBooleanProperty(this, PROP_REPEAT);
	@JsonbTransient
	private final DoubleProperty progress = new SimpleDoubleProperty(this, PROP_PROGRESS);
	@JsonbTransient
	private final ObjectProperty<TimerState> state = new SimpleObjectProperty<>(this, PROP_STATE, TimerState.STOPPED);
	private long warningTime;
	private long finishTime;
	private final Runnable ticker = new Ticker();
	private ScheduledFuture<?> future;

	{
		intInterval = intervalDuration.get().intValueProperty();
		isoInterval = intervalDuration.get().isoValueProperty();
		intInterval.addListener(this::onIntIntervalChanged);
		intWarnAfter = warnAfterDuration.get().intValueProperty();
		isoWarnAfter = warnAfterDuration.get().isoValueProperty();
	}

	/**
	 * Constructs a new <code>TimerBean</code>.
	 */
	public TimerBean() {
	}

	/**
	 * Constructs a new <code>TimerBean</code> by copying the settings from an
	 * existing instance.
	 */
	public TimerBean(TimerBean src) {
		apply(src);
	}

	/**
	 * Callback invoked when the timer's interval has been changed.
	 * 
	 * @param source   The property that changed.
	 * @param oldValue The previous interval value.
	 * @param newValue The new interval value.
	 */
	private void onIntIntervalChanged(ObservableValue<? extends Number> source, Number oldValue, Number newValue) {
		// FIXME: why are we multiplying by SECONDS_PER_MINUTE?
		setRemainingMillis(newValue.longValue() * MILLISECONDS_PER_SECOND);
	}

	/**
	 * Applies the settings from the specified timer bean onto the receiving bean.
	 * 
	 * @param src The source bean from which settings are copied.
	 */
	public void apply(TimerBean src) {
		setName(src.getName());
		setIntInterval(src.getIntInterval());
		setIntWarnAfter(src.getIntWarnAfter());
		setRepeat(src.getRepeat());
	}

	/**
	 * Cleans up after a scheduled timer run has been cancelled.
	 */
	private void cleanup() {
		if (future != null)
			future.cancel(true);
		TimerUtils.EXECUTOR.remove(ticker);
		warningTime = finishTime = 0;
	}

	/**
	 * Returns the timer interval as a <code>Duration</code> object.
	 * 
	 * @return the timer interval as a <code>Duration</code> object.
	 */
	public Duration getIntervalDuration() {
		return intervalDuration.get();
	}

	/**
	 * Returns the timer interval in milliseconds.
	 * 
	 * @return the timer interval in milliseconds.
	 */
	private long getIntervalMillis() {
		return intInterval.get() * (long) MILLISECONDS_PER_SECOND;
	}

	/**
	 * Returns the timer interval in seconds.
	 * 
	 * @return the timer interval in seconds.
	 */
	public final int getIntInterval() {
		return intInterval.get();
	}

	/**
	 * Returns the 'warn after' interval in seconds.
	 * 
	 * @return the 'warn after' interval in seconds.
	 * @see #warnAfterProperty()
	 */
	public final int getIntWarnAfter() {
		return intWarnAfter.get();
	}

	/**
	 * Returns the timer interval as an ISO-8601 Period string.
	 * 
	 * @return an ISO-8601 Period string.
	 */
	public final String getIsoInterval() {
		return isoInterval.get();
	}

	/**
	 * Returns the timer 'warn after' interval as an ISO-8601 Period string.
	 * 
	 * @return an ISO-8601 Period string.
	 */
	public final String getIsoWarnAfter() {
		return isoWarnAfter.get();
	}

	/**
	 * Returns the timer name.
	 * 
	 * @return the timer name.
	 */
	public final String getName() {
		return name.get();
	}

	/**
	 * Returns the timer's progress as a double between 0 and 1.
	 * 
	 * @return the timer's progress.
	 */
	public final double getProgress() {
		return progress.get();
	}

	/**
	 * Returns the number of milliseconds remaining until the timer is complete.
	 * 
	 * @return the number of milliseconds remaining.
	 */
	public final long getRemainingMillis() {
		return remainingMillis.get();
	}

	/**
	 * Returns the value of the <code>repeat</code> property.
	 * 
	 * @return the value of the repeat property.
	 */
	public final boolean getRepeat() {
		return repeat.get();
	}

	/**
	 * Returns the value of the <code>state</code> property.
	 * 
	 * @return the value of the state property.
	 */
	public final TimerState getState() {
		return state.get();
	}

	/**
	 * Returns the timer 'warn after' interval as a Duration object.
	 * 
	 * @return the timer 'warn after' interval.
	 * @see #warnAfterDurationProperty()
	 */
	public Duration getWarnAfterDuration() {
		return warnAfterDuration.get();
	}

	/**
	 * Returns the timer 'warn after' interval in milliseconds.
	 * 
	 * @return the timer 'warn after' interval.
	 */
	private long getWarnAfterMillis() {
		return intWarnAfter.get() * (long) MILLISECONDS_PER_SECOND;
	}

	/**
	 * Returns the timer interval duration property.
	 * 
	 * @return the timer interval duration property.
	 */
	public ObjectProperty<Duration> intervalDurationProperty() {
		return intervalDuration;
	}

	/**
	 * Returns the timer interval seconds property.
	 * 
	 * @return the timer interval seconds property.
	 */
	public final IntegerProperty intIntervalProperty() {
		return intInterval;
	}

	/**
	 * Returns the timer interval ISO-8601 Period property.
	 * 
	 * @return the timer interval ISO-8601 Period property.
	 */
	public final StringProperty isoIntervalProperty() {
		return isoInterval;
	}

	/**
	 * Returns the timer name property.
	 * 
	 * @return the timer name property.
	 */
	public final StringProperty nameProperty() {
		return name;
	}

	/**
	 * Returns the timer progress property.
	 * 
	 * @return the timer progress property.
	 */
	public final DoubleProperty progressProperty() {
		return progress;
	}

	/**
	 * Returns the remaining time in milliseconds property.
	 * 
	 * @return the remaining time in milliseconds property.
	 */
	public final LongProperty remainingMillisProperty() {
		return remainingMillis;
	}

	/**
	 * Returns the repeat property.
	 * 
	 * @return the repeat property.
	 */
	public final BooleanProperty repeatProperty() {
		return repeat;
	}

	/**
	 * Sets the timer interval in seconds.
	 * 
	 * @param newIntInterval The new timer interval in seconds.
	 */
	public final void setIntInterval(int newIntInterval) {
		intInterval.set(newIntInterval);
	}

	/**
	 * Sets the timer 'warn after' interval in seconds.
	 * 
	 * @param newIntWarnAfter The new timer 'warn after' interval in seconds.
	 * @see #warnAfterProperty()
	 */
	public final void setIntWarnAfter(int newIntWarnAfter) {
		intWarnAfter.set(newIntWarnAfter);
	}

	/**
	 * Sets the timer interval as an ISO-8601 Period string.
	 * 
	 * @param newIsoInterval the timer interval as an ISO-8601 Period string.
	 */
	public final void setIsoInterval(String newIsoInterval) {
		isoInterval.set(newIsoInterval);
	}

	/**
	 * Sets the timer 'warn after' interval as an ISO-8601 Period string.
	 * 
	 * @param newIsoWarnAfter the timer interval as an ISO-8601 Period string.
	 */
	public final void setIsoWarnAfter(String newIsoWarnAfter) {
		isoWarnAfter.set(newIsoWarnAfter);
	}

	/**
	 * Sets the timer name property value.
	 * 
	 * @param newName The new timer name.
	 */
	public final void setName(String newName) {
		name.set(newName);
	}

	/**
	 * Sets the timer progress property value.
	 * 
	 * @param newProgress The timer progress property value as a double between 0
	 *                    and 1.
	 */
	private void setProgress(double newProgress) {
		progress.set(newProgress);
	}

	/**
	 * Sets the remaining milliseconds property value.
	 * 
	 * @param newRemainingMillis The remaining milliseconds.
	 */
	private final void setRemainingMillis(long newRemainingMillis) {
		remainingMillis.set(newRemainingMillis);
	}

	/**
	 * Sets the <code>repeat</code> property value.
	 * 
	 * @param newRepeat The repeat property value.
	 */
	public final void setRepeat(boolean newRepeat) {
		repeat.set(newRepeat);
	}

	/**
	 * Sets the value of the timer state property.
	 * 
	 * @param newState The new timer state value.
	 */
	private void setState(TimerState newState) {
		state.set(newState);
	}

	/**
	 * Sets the timer into the waiting state if the timer is currently stopped or
	 * complete.
	 */
	public void standby() {
		if (getState() != TimerState.STOPPED && getState() != TimerState.COMPLETE)
			return;
		setState(TimerState.WAITING);
	}

	/**
	 * Starts the timer. Has no effect if the timer is already in the
	 * {@link TimerState#RUNNING running} or {@link TimerState#WAITING warning}
	 * states.
	 */
	public void start() {
		if (getState() == TimerState.RUNNING || getState() == TimerState.WARNING)
			return;
		long startTime = System.currentTimeMillis();
		future = TimerUtils.EXECUTOR.scheduleAtFixedRate(ticker, 100, 100, TimeUnit.MILLISECONDS);
		setState(TimerState.RUNNING);
		setProgress(0);
		warningTime = getWarnAfterMillis() != 0 ? startTime + getWarnAfterMillis() : Long.MAX_VALUE;
		finishTime = startTime + getIntervalMillis();
	}

	/**
	 * Returns the timer <code>state</code> property.
	 * 
	 * @return the timer state property.
	 */
	public final ObjectProperty<TimerState> stateProperty() {
		return state;
	}

	/**
	 * Stops the timer. Has no effect if the timer is already
	 * {@link TimerState#STOPPED stopped}
	 */
	public void stop() {
		if (getState() == TimerState.STOPPED)
			return;
		if (future != null)
			future.cancel(true);
		TimerUtils.EXECUTOR.remove(ticker);
		setState(TimerState.STOPPED);
		setProgress(0);
		setRemainingMillis((long) getIntInterval() * 60 * 1000);
		warningTime = finishTime = 0;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "TimerBean [name=" + getName() + ", intInterval=" + getIntInterval() + ", isoInterval="
				+ getIsoInterval() + ", intWarnAfter=" + getIntWarnAfter() + ", isoWarnAfter=" + getIsoWarnAfter()
				+ ", repeat=" + getRepeat() + ", state=" + getState() + ", progress=" + getProgress()
				+ ", remainingMillis=" + getRemainingMillis() + ']';
	}

	/**
	 * Returns the 'warn after' duration property.
	 * 
	 * @return the 'warn after' duration property.
	 */
	public ObjectProperty<Duration> warnAfterDurationProperty() {
		return warnAfterDuration;
	}

	/**
	 * Returns the 'warn after' interval seconds property. When the timer is
	 * running, it enters the {@link TimerState#WARNING warning} state once this
	 * interval has elapsed.
	 * 
	 * @return the 'warn after' interval seconds property.
	 */
	public final IntegerProperty warnAfterProperty() {
		return intWarnAfter;
	}
}
