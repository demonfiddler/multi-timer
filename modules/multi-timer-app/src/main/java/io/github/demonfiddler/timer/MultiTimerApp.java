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

package io.github.demonfiddler.timer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.demonfiddler.timer.util.Constants;
import io.github.demonfiddler.timer.util.Images;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;

/**
 * An application that supports multiple configurable timers.
 * 
 * @since 1.0
 */
public class MultiTimerApp extends Application {
	private static Stage stage;

	/**
	 * Returns the main application window.
	 * 
	 * @return the main application window.
	 */
	public static Stage getStage() {
		return stage;
	}

	/**
	 * The main application entry point.
	 * 
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/** {@inheritDoc} */
	@Override
	public void start(Stage stage) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);

		MultiTimerApp.stage = stage;

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/timer-app.fxml"));
		Parent root = fxmlLoader.load();

		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

		stage.setTitle(Constants.APP_SHORT_NAME);
		stage.getIcons().add(Images.IMG_TIMER);
		stage.setScene(scene);
		stage.show();

		// Open any file specified as a command line parameter.
		List<String> files = getParameters().getUnnamed();
		if (!files.isEmpty()) {
			File file = new File(files.get(0));
			if (file.exists() && file.isFile()) {
				MultiTimerController controller = fxmlLoader.getController();
				controller.open(file);
			}
		}
	}

	/**
	 * Invoked when an uncaught exception is thrown anywhere in the application.
	 * @param t The thread that threw the exception.
	 * @param e The exception that was thrown.
	 */
	private void uncaughtException(Thread t, Throwable e) {
		if (e instanceof Exception) {
			Alert dlg = new Alert(AlertType.ERROR);
			dlg.setTitle("Multi-Timer");
			dlg.setHeaderText("Unexpected Error");
			dlg.setContentText("The application experienced an unexpected error and is unable to complete your request. Please report this to your support person.");
			dlg.getButtonTypes().clear();
			ButtonType btnTypeCopy = new ButtonType("Copy");
			dlg.getButtonTypes().addAll(btnTypeCopy, ButtonType.OK);
			Optional<ButtonType> response = dlg.showAndWait();
			if (response.isPresent() && response.get() == btnTypeCopy) {
				StringWriter stack = new StringWriter();
				e.printStackTrace(new PrintWriter(stack));
				Map<DataFormat, Object> content = Collections.singletonMap(DataFormat.PLAIN_TEXT, stack.toString());
				Clipboard.getSystemClipboard().setContent(content);
			}
		}
	}
}
