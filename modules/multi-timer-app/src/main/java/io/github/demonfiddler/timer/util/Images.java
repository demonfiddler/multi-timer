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

import javafx.scene.image.Image;

/**
 * Various images used by the application.
 * @since 1.0
 */
public final class Images {
	public static final Image IMG_START;
	public static final Image IMG_STOP;
	public static final Image IMG_TIMER;

	static {
		IMG_START = loadImage("/start.png");
		IMG_STOP = loadImage("/stop.png");
		IMG_TIMER = loadImage("/timer.png");
	}

	/**
	 * Loads an image from the specified path within the application JAR.
	 * @param path The image path.
	 * @return The image.
	 */
	private static Image loadImage(String path) {
		return new Image(Images.class.getResource(path).toExternalForm());
	}

	/** Private ctor prevents instantiation. */
	private Images() {
	}
}
