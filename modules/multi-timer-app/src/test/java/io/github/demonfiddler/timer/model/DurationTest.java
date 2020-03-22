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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DurationTest {
	Duration bean;

	private void checkProperties(int expectedHours, int expectedMinutes, int expectedSeconds, int expectedIntValue,
			String expectedIsoValue, String msgPrefix) {

		assertEquals(bean.getHours(), expectedHours, msgPrefix + "hours incorrect;");
		assertEquals(bean.getMinutes(), expectedMinutes, msgPrefix + "minutes incorrect;");
		assertEquals(bean.getSeconds(), expectedSeconds, msgPrefix + "seconds incorrect;");
		assertEquals(bean.getIntValue(), expectedIntValue, msgPrefix + "intValue incorrect;");
		assertEquals(bean.getIsoValue(), expectedIsoValue, msgPrefix + "isoValue incorrect;");
	}

	@BeforeEach
	void setUp() throws Exception {
		bean = new Duration();
	}

	@AfterEach
	void tearDown() throws Exception {
		bean = null;
	}

	@Test
	void testHours() {
		bean.setHours(2);
		checkProperties(2, 0, 0, 7200, "PT2H", "testHours - ");
	}

	@Test
	void testHoursMinutesSeconds() {
		int newHours = 2;
		int newMinutes = 3;
		int newSeconds = 4;
		int newIntValue = newHours * SECONDS_PER_HOUR + newMinutes * SECONDS_PER_MINUTE + newSeconds;
		bean.setHours(newHours);
		bean.setMinutes(newMinutes);
		bean.setSeconds(newSeconds);
		checkProperties(newHours, newMinutes, newSeconds, newIntValue, "PT2H3M4S", "testHoursMinutesSeconds - ");
	}

	@Test
	void testHoursMinutesSecondsWithRollover() {
		int newHours = 1;
		int newMinutes = 2 * MINUTES_PER_HOUR + 3;
		int newSeconds = 3 * SECONDS_PER_HOUR + 4 * SECONDS_PER_MINUTE + 5;
		int newIntValue = newHours * SECONDS_PER_HOUR + newMinutes * SECONDS_PER_MINUTE + newSeconds;
		bean.setHours(newHours);
		bean.setMinutes(newMinutes);
		bean.setSeconds(newSeconds);
		checkProperties(1 + 2 + 3, 3 + 4, 5, newIntValue, "PT6H7M5S", "testHoursMinutesSecondsWithRollover - ");
	}

	@Test
	void testMinutes() {
		bean.setMinutes(3);
		checkProperties(0, 3, 0, 180, "PT3M", "testMinutes - ");
	}

	@Test
	void testMinutesWithRollover() {
		int newMinutes = 1 * MINUTES_PER_HOUR + 3;
		int newValue = newMinutes * SECONDS_PER_MINUTE;
		bean.setMinutes(newMinutes);
		checkProperties(1, 3, 0, newValue, "PT1H3M", "testMinutesWithRollover - ");
	}

	@Test
	void testSeconds() {
		bean.setSeconds(4);
		checkProperties(0, 0, 4, 4, "PT4S", "testSeconds - ");
	}

	@Test
	void testSecondsWithRollover() {
		int newIntValue = 1 * SECONDS_PER_HOUR + 2 * MINUTES_PER_HOUR + 3;
		bean.setSeconds(newIntValue);
		checkProperties(1, 2, 3, newIntValue, "PT1H2M3S", "testSecondsWithRollover - ");
	}

	@Test
	void testIntValue() {
		int newIntValue = 1 * SECONDS_PER_HOUR + 2 * SECONDS_PER_MINUTE + 3;
		bean.setIntValue(newIntValue);
		checkProperties(1, 2, 3, newIntValue, "PT1H2M3S", "testIntValue - ");

		bean.setIntValue(0);
		checkProperties(0, 0, 0, 0, "P", "testValue - ");
	}

	@Test
	void testIsoValue() {
		int newIntValue = 1 * SECONDS_PER_HOUR + 2 * SECONDS_PER_MINUTE + 3;
		bean.setIsoValue("PT1H2M3S");
		checkProperties(1, 2, 3, newIntValue, "PT1H2M3S", "testIsoValue - ");

		// NOTE: setting year/month/day fields throws an internally caught exception.
		bean.setIsoValue("P1Y2M3D");
		checkProperties(1, 2, 3, newIntValue, "PT1H2M3S", "testIsoValue - ");

		bean.setIsoValue("P");
		checkProperties(0, 0, 0, 0, "P", "testValue - ");
	}
}
