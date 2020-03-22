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

package io.github.demonfiddler.timer.migration;

import java.util.Objects;

import io.github.demonfiddler.timer.model.MultiTimerBean;
import io.github.demonfiddler.timer.util.Constants;

/**
 * Handles format version migration.
 * 
 * @since 1.0
 */
public final class Migration {
	/**
	 * Migrates a multi-timer bean to the latest format version.
	 * @param bean The bean to migrate.
	 */
	public static MultiTimerBean migrate(MultiTimerBean bean) {
		Objects.requireNonNull(bean, "bean is required");
		// TODO: implement a proper migration strategy.
		bean.setFormatVersion(Constants.FORMAT_VERSION);
		return bean;
	}

	/** Private ctor prevents instantiation. */
	private Migration() {
	}
}
