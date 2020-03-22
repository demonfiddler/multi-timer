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

import static io.github.demonfiddler.timer.util.Constants.MINUTES_PER_HOUR;
import static io.github.demonfiddler.timer.util.Constants.SECONDS_PER_HOUR;
import static io.github.demonfiddler.timer.util.Constants.SECONDS_PER_MINUTE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * A JavaFX bean that holds a temporal duration value.
 * 
 * @since 1.0
 */
public class Duration {
	private static final Pattern ISO8601_DURATION = Pattern.compile(
			"P(?:(\\d+)Y)?(?:(\\d{1,2})M)?(?:(\\d{1,2})D)?(?:T(?:(\\d{1,2})H)?(?:(\\d{1,2})M)?(?:(\\d{1,2})S)?)?");
	private final IntegerProperty hours = new SimpleIntegerProperty();
	private final IntegerProperty minutes = new SimpleIntegerProperty();
	private final IntegerProperty seconds = new SimpleIntegerProperty();
	private final IntegerProperty intValue = new SimpleIntegerProperty();
	private final StringProperty isoValue = new SimpleStringProperty("P");
	private volatile boolean updating;

	{
		hours.addListener(this::onFieldUpdated);
		minutes.addListener(this::onFieldUpdated);
		seconds.addListener(this::onFieldUpdated);
		intValue.addListener(this::onIntValueUpdated);
		isoValue.addListener(this::onIsoValueUpdated);
	}

	/**
	 * Returns the hours component.
	 * 
	 * @return the hours component.
	 */
	public int getHours() {
		return hours.get();
	}

	/**
	 * Returns the total duration in seconds.
	 * 
	 * @return the total duration in seconds.
	 */
	public int getIntValue() {
		return intValue.get();
	}

	/**
	 * Returns the specified field from a matched ISO-8601 duration value.
	 * 
	 * @param matcher The matcher.
	 * @param field   The index of the capture group for the requested field.
	 * @return the requested field value.
	 */
	private int getIsoDurationField(Matcher matcher, int field) {
		String strValue = matcher.group(field);
		return strValue == null ? 0 : Integer.parseInt(strValue);
	}

	/**
	 * Returns the duration as an ISO-8601 Period string.
	 * 
	 * @return the duration as an ISO-8601 Period string.
	 */
	public String getIsoValue() {
		return isoValue.get();
	}

	/**
	 * Returns the minutes component.
	 * 
	 * @return the minutes component.
	 */
	public int getMinutes() {
		return minutes.get();
	}

	/**
	 * Returns the seconds component.
	 * 
	 * @return the seconds component.
	 */
	public int getSeconds() {
		return seconds.get();
	}

	/**
	 * Returns the hours property.
	 * 
	 * @return the hours property.
	 */
	public IntegerProperty hoursProperty() {
		return hours;
	}

	/**
	 * Returns the total duration in seconds property.
	 * 
	 * @return the total duration in seconds property.
	 */
	public IntegerProperty intValueProperty() {
		return intValue;
	}

	/**
	 * Returns the ISO-8601 Period property.
	 * 
	 * @return the ISO-8601 Period property.
	 */
	public StringProperty isoValueProperty() {
		return isoValue;
	}

	/**
	 * Returns the minutes property.
	 * 
	 * @return the minutes property.
	 */
	public IntegerProperty minutesProperty() {
		return minutes;
	}

	/**
	 * Sets the total duration in seconds, hours, minutes and seconds fields when
	 * one of them is updated.
	 * 
	 * @param source   The source observable that has been updated.
	 * @param oldValue The old field value.
	 * @param newValue The updated field value.
	 */
	private void onFieldUpdated(ObservableValue<? extends Number> source, Number oldValue, Number newValue) {
		if (!updating) {
			try {
				updating = true;
				setIntValue(getHours() * SECONDS_PER_HOUR + getMinutes() * SECONDS_PER_MINUTE + getSeconds());
				setFields(getIntValue());
			} finally {
				updating = false;
			}
		}
	}

	/**
	 * Sets the hours, minutes and seconds fields when total duration in seconds has
	 * been updated.
	 * 
	 * @param source   The source observable that has been updated.
	 * @param oldValue The old total duration in seconds field value.
	 * @param newValue The updated total duration in seconds field value.
	 */
	private void onIntValueUpdated(ObservableValue<? extends Number> source, Number oldValue, Number newValue) {
		if (!updating) {
			try {
				updating = true;
				setFields(newValue.intValue());
			} finally {
				updating = false;
			}
		}
	}

	/**
	 * Sets the total duration in seconds, hours, minutes and seconds fields when
	 * the ISO-8601 value is updated.
	 * 
	 * @param source   The source observable that has been updated.
	 * @param oldValue The old ISO-8601 value.
	 * @param newValue The updated ISO-8601 value.
	 */
	private void onIsoValueUpdated(ObservableValue<? extends String> source, String oldValue, String newValue) {
		if (!updating) {
			try {
				updating = true;
				Matcher matcher = ISO8601_DURATION.matcher(newValue);
				if (matcher.matches()) {
					int newYears = getIsoDurationField(matcher, 1);
					int newMonths = getIsoDurationField(matcher, 2);
					int newDays = getIsoDurationField(matcher, 3);
					if (newYears != 0 || newMonths != 0 || newDays != 0) {
						setIsoValue(oldValue);
						throw new IllegalArgumentException("ISO-8601 period Y, M and D fields are not supported");
					}
					int newHours = getIsoDurationField(matcher, 4);
					int newMinutes = getIsoDurationField(matcher, 5);
					int newSeconds = getIsoDurationField(matcher, 6);
					int newIntValue = newHours * SECONDS_PER_HOUR + newMinutes * SECONDS_PER_MINUTE + newSeconds;
					setHours(newHours);
					setMinutes(newMinutes);
					setSeconds(newSeconds);
					setIntValue(newIntValue);
				}
			} finally {
				updating = false;
			}
		}
	}

	/**
	 * Returns the seconds property.
	 * 
	 * @return the seconds property.
	 */
	public IntegerProperty secondsProperty() {
		return seconds;
	}

	/**
	 * Sets the hours, minutes, seconds fields and the ISO-8601 value from the
	 * specified interval in seconds.
	 * 
	 * @param newIntValue the interval in seconds to set.
	 */
	private void setFields(int newIntValue) {
		int newHours = newIntValue / SECONDS_PER_HOUR;
		int newMinutes = newIntValue / SECONDS_PER_MINUTE - newHours * MINUTES_PER_HOUR;
		int newSeconds = newIntValue % SECONDS_PER_MINUTE;
		setHours(newHours);
		setMinutes(newMinutes);
		setSeconds(newSeconds);
		setIsoValue(formatIsoValue(0, 0, 0, newHours, newMinutes, newSeconds));
	}

	/**
	 * Returns an ISO-8601 Period string expressing the specified duration.
	 * 
	 * @param years   The year field.
	 * @param months  The months field.
	 * @param days    The days field.
	 * @param hours   The hours field.
	 * @param minutes The minutes field.
	 * @param seconds The seconds field.
	 * @return the corresponding ISO-8601 Period value.
	 */
	private String formatIsoValue(int years, int months, int days, int hours, int minutes, int seconds) {
		StringBuilder isoValue = new StringBuilder("P");
		if (years != 0)
			isoValue.append(years).append('Y');
		if (months != 0)
			isoValue.append(months).append('M');
		if (days != 0)
			isoValue.append(days).append('D');
		if (hours != 0 || minutes != 0 || seconds != 0) {
			isoValue.append('T');
			if (hours != 0)
				isoValue.append(hours).append('H');
			if (minutes != 0)
				isoValue.append(minutes).append('M');
			if (seconds != 0)
				isoValue.append(seconds).append('S');
		}
		return isoValue.toString();
	}

	/**
	 * Sets the hours field. Adjusts other property values as necessary.
	 * 
	 * @param newHours The hours value to set.
	 */
	public void setHours(int newHours) {
		hours.set(newHours);
	}

	/**
	 * Sets all fields from the total duration in seconds.
	 * 
	 * @param newIntValue The total duration in seconds to set.
	 */
	public void setIntValue(int newIntValue) {
		intValue.set(newIntValue);
	}

	/**
	 * Sets all fields from an ISO-8601 Period value.
	 * 
	 * @param newIsoValue The ISO-8601 Period value to set.
	 */
	public void setIsoValue(String newIsoValue) {
		isoValue.set(newIsoValue);
	}

	/**
	 * Sets the minutes field. Adjusts other property values as necessary.
	 * 
	 * @param newMinutes The minutes value to set.
	 */
	public void setMinutes(int newMinutes) {
		minutes.set(newMinutes);
	}

	/**
	 * Sets the seconds field. Adjusts other property values as necessary.
	 * 
	 * @param newSeconds The seconds value to set.
	 */
	public void setSeconds(int newSeconds) {
		seconds.set(newSeconds);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "Duration[hours=" + getHours() + ", minutes=" + getMinutes() + ", seconds=" + getSeconds()
				+ ", intValue=" + getIntValue() + ", isoValue=" + getIsoValue() + ']';
	}
}
