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

import io.github.demonfiddler.timer.model.Duration;
import io.github.demonfiddler.timer.model.TimerBean;
import javafx.beans.property.IntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.util.converter.NumberStringConverter;

/**
 * The controller that links the edit dialog view and model for a single timer instance.
 * @since 1.0
 */
public class TimerEditController {
	@FXML
	private DialogPane dlgPane;
	@FXML
	private TextField txtName;
	@FXML
	private Spinner<Integer> spiIntervalHours;
	@FXML
	private Spinner<Integer> spiIntervalMinutes;
	@FXML
	private Spinner<Integer> spiIntervalSeconds;
	@FXML
	private Spinner<Integer> spiWarnAfterHours;
	@FXML
	private Spinner<Integer> spiWarnAfterMinutes;
	@FXML
	private Spinner<Integer> spiWarnAfterSeconds;
	@FXML
	private CheckBox cbxRepeat;
	private TimerBean bean;

	/**
	 * Configures a spinner control's editor by binding it to an integer property and setting its text alignment.
	 * @param spinner The spinner to bind.
	 * @param property The property to bind.
	 */
	private void configureSpinner(Spinner<Integer> spinner, IntegerProperty property) {
		TextField editor = spinner.getEditor();
		editor.textProperty().bindBidirectional(property, new NumberStringConverter());
		editor.setAlignment(Pos.CENTER_RIGHT);
	}

	/**
	 * Returns the timer bean that is being updated in the edit dialogue.
	 * @return the timer bean.
	 */
	public TimerBean getBean() {
		return bean;
	}

	/**
	 * Called by the JavaFX framework to initialise the controller.
	 */
	public void initialize() {
		spiIntervalHours.setValueFactory(new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
		spiIntervalMinutes.setValueFactory(new IntegerSpinnerValueFactory(0, 59));
		spiIntervalSeconds.setValueFactory(new IntegerSpinnerValueFactory(0, 59));
		spiWarnAfterHours.setValueFactory(new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
		spiWarnAfterMinutes.setValueFactory(new IntegerSpinnerValueFactory(0, 59));
		spiWarnAfterSeconds.setValueFactory(new IntegerSpinnerValueFactory(0, 59));
	}

	/**
	 * Sets the timer bean to be updated in the edit dialogue.
	 * @param bean the timer bean.
	 */
	public void setBean(TimerBean bean) {
		if (bean == null)
			throw new IllegalArgumentException("bean cannot be null");
		if (this.bean != null)
			throw new IllegalStateException("bean has already been set");
		this.bean = bean;
		txtName.textProperty().bindBidirectional(bean.nameProperty());
		Duration intervalObj = bean.getIntervalDuration();
		configureSpinner(spiIntervalHours, intervalObj.hoursProperty());
		configureSpinner(spiIntervalMinutes, intervalObj.minutesProperty());
		configureSpinner(spiIntervalSeconds, intervalObj.secondsProperty());
		Duration warnAfterObj = bean.getWarnAfterDuration();
		configureSpinner(spiWarnAfterHours, warnAfterObj.hoursProperty());
		configureSpinner(spiWarnAfterMinutes, warnAfterObj.minutesProperty());
		configureSpinner(spiWarnAfterSeconds, warnAfterObj.secondsProperty());
		cbxRepeat.selectedProperty().bindBidirectional(bean.repeatProperty());
	}
}