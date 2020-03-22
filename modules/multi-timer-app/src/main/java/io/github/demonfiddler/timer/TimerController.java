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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import io.github.demonfiddler.timer.model.TimerBean;
import io.github.demonfiddler.timer.util.Images;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

/**
 * The controller that links the view and model for a single timer instance.
 * @since 1.0
 */
public class TimerController {
	private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

	static {
		TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@FXML
	private GridPane grid;
	@FXML
	private Label lblName;
	@FXML
	private ProgressBar prgComplete;
	@FXML
	private Circle shpState;
	@FXML
	private Label lblElapsed;
	@FXML
	private Button btnRun;
	@FXML
	private Button btnEdit;
	@FXML
	private Button btnDelete;
	private MultiTimerController timerAppController;
	private TimerBean bean;

	/**
	 * Shows a dialogue to enable the timer settings to be updated.
	 * @param adding <code>true</code> if adding a new timer; <code>false</code> if editing an existing one.
	 * @throws Exception if unable to load FXML resources from the class path.
	 */
	void addOrEdit(boolean adding) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/timer-edit.fxml"));
		DialogPane dialogPane = fxmlLoader.load();
		TimerEditController controller = fxmlLoader.getController();
		TimerBean tempBean = new TimerBean(bean);
		controller.setBean(tempBean);

		Dialog<TimerBean> dialog = new Dialog<>();
		dialog.setTitle(adding ? "Add Timer" : "Edit Timer");
		dialog.setDialogPane(dialogPane);
		dialog.setResultConverter(bt -> bt == ButtonType.OK ? tempBean : null);
		Optional<TimerBean> result = dialog.showAndWait();
		if (result.isPresent()) {
			bean.apply(result.get());
			timerAppController.setModified(true);
			timerAppController.resize();
		}
	}

	/**
	 * Deletes this timer from its owning multi-timer instance. The bean is stopped if it is waiting or running.
	 */
	public void delete() {
		stop();
		timerAppController.delete(bean);
	}

	/**
	 * Shows a dialogue to enable the timer settings to be updated.
	 * @throws Exception if unable to load FXML resources from the class path.
	 */
	public void edit() throws Exception {
		addOrEdit(false);
	}

	/**
	 * Binds the editable properties of the timer model to the corresponding view elements in the user interface.
	 */
	private void hookTimerBean() {
		lblName.textProperty().bind(bean.nameProperty());
		prgComplete.progressProperty().bind(bean.progressProperty());
		lblElapsed.textProperty().bind(new StringBinding() {
			{
				super.bind(bean.remainingMillisProperty());
			}

			@Override
			protected String computeValue() {
				Date remainingDate = new Date();
				remainingDate.setTime(bean.getRemainingMillis());
				return TIME_FORMAT.format(remainingDate);
			}
		});
		shpState.fillProperty().bind(new ObjectBinding<Paint>() {
			{
				super.bind(bean.stateProperty());
			}

			@Override
			protected Paint computeValue() {
				switch (bean.getState()) {
				case COMPLETE:
				case STOPPED:
					return Color.RED;
				case WAITING:
				case WARNING:
					return Color.ORANGE;
				case RUNNING:
					return Color.GREEN;
				}
				return null;
			}
		});
		((ImageView) btnRun.getGraphic()).imageProperty().bind(new ObjectBinding<Image>() {
			{
				super.bind(bean.stateProperty());
			}

			@Override
			protected Image computeValue() {
				switch (bean.getState()) {
				case STOPPED:
				case COMPLETE:
					btnEdit.setDisable(false);
					btnDelete.setDisable(false);
					return Images.IMG_START;
				case WAITING:
				case RUNNING:
				case WARNING:
					btnEdit.setDisable(true);
					btnDelete.setDisable(true);
					return Images.IMG_STOP;
				default:
					return null;
				}
			}
		});
	}

	/**
	 * Called by the JavaFX framework to initialise the controller.
	 */
	public void initialize() {
	}

	/**
	 * Starts or stops the timer.
	 */
	public void run() {
		switch (bean.getState()) {
		case STOPPED:
		case COMPLETE:
			start();
			break;
		case RUNNING:
		case WARNING:
			stop();
			break;
		case WAITING:
			// Individual timer beans never enter the waiting state.
			// TODO: is this still true?
			break;
		}
	}

	/**
	 * Sets the timer model that this controller will be managing.
	 * @param bean The timer model.
	 */
	public void setBean(TimerBean bean) {
		if (this.bean != null) {
			this.bean.stop();
			unhookTimerBean();
		}
		this.bean = bean;
		hookTimerBean();
	}

	/**
	 * Sets a reference to the parent multi-timer instance's controller.
	 * @param multiTimerController the parent multi-timer instance's controller.
	 */
	public void setTimerAppController(MultiTimerController multiTimerController) {
		this.timerAppController = multiTimerController;
	}

	/**
	 * Starts the timer bean.
	 */
	private void start() {
		bean.start();
	}

	/**
	 * Stops the timer bean.
	 */
	private void stop() {
		bean.stop();
	}

	/**
	 * Unbinds the timer model from the corresponding view elements in the user interface.
	 */
	private void unhookTimerBean() {
		lblName.textProperty().unbind();
		prgComplete.progressProperty().unbind();
		lblElapsed.textProperty().unbind();
		shpState.visibleProperty().unbind();
		((ImageView) btnRun.getGraphic()).imageProperty().unbind();
	}
}
