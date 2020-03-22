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

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import io.github.demonfiddler.timer.migration.Migration;
import io.github.demonfiddler.timer.model.MultiTimerBean;
import io.github.demonfiddler.timer.model.TimerBean;
import io.github.demonfiddler.timer.model.TimerState;
import io.github.demonfiddler.timer.util.Constants;
import io.github.demonfiddler.timer.util.Images;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.converter.NumberStringConverter;

/**
 * The main application controller.
 * 
 * @since 1.0
 */
public class MultiTimerController implements ListChangeListener<TimerBean> {
	/** The Multi-Timer JSON file extension */
	private static final ExtensionFilter EXTENSION_FILTER = new ExtensionFilter("Multi-Timer files", "*" + Constants.FILE_EXT_DOT_TIMERS);
	/** The singleton Jsonb instance. */
	private static final Jsonb JSONB;

	static {
		JsonbConfig jsonbConfig = new JsonbConfig();
		jsonbConfig.setProperty(JsonbConfig.FORMATTING, true);
		jsonbConfig.setProperty(JsonbConfig.PROPERTY_NAMING_STRATEGY, PropertyNamingStrategy.LOWER_CASE_WITH_DASHES);
		// OOTB, Yasson doesn't deserialize generic list properties.
//		JsonbBuilder builder = JsonbBuilder.newBuilder("org.eclipse.yasson.JsonBindingProvider");
		JsonbBuilder builder = JsonbBuilder.newBuilder("org.apache.johnzon.jsonb.JohnzonProvider");
		JSONB = builder.withConfig(jsonbConfig).build();
	}

	@FXML
	private CheckBox cbxDelayStart;
	@FXML
	private Spinner<Integer> spiMinsOffset;
	@FXML
	private Label lblMinsOffset;
	@FXML
	private Circle shpState;
	@FXML
	private Button btnAdd;
	@FXML
	private Button btnRun;
	@FXML
	private VBox vbxTimers;
	@FXML
	private MenuItem miFileSave;
	@FXML
	private MenuItem miFileSaveAs;
	@FXML
	private MenuItem miFileClose;
	@FXML
	private MenuItem miTimerRun;
	@FXML
	private MenuItem miTimerAdd;
	private MultiTimerBean timersBean;
	private final Map<TimerBean, TimerController> controllers = new HashMap<>();
	private File file;
	private boolean modified;

	/**
	 * Displays the About dialogue.
	 */
	public void about() {
		Alert dlg = new Alert(AlertType.INFORMATION);
		dlg.setTitle("About");
		dlg.setHeaderText(Constants.APP_LONG_NAME + " version " + Constants.APP_VERSION);
		dlg.setContentText("\u00A9 2020 Adrian Price. All Rights Reserved.");
		dlg.showAndWait();
	}

	/**
	 * Adds a new timer and opens the Edit dialog.
	 * 
	 * @throws Exception if unable to load FXML resources from the class path.
	 */
	public void add() throws Exception {
		TimerBean bean = new TimerBean();
		bean.setName("Timer " + timersBean.getTimers().size());
		bean.setIntInterval(10);
		bean.setIntWarnAfter(8);
		bean.setRepeat(false);
		timersBean.getTimers().add(bean);
		setModified(true);
		resize();
		controllers.get(bean).addOrEdit(true);
	}

	/**
	 * Closes the currently open multi-timer instance, querying to save it if
	 * modified.
	 * 
	 * @return <code>true</code> if the user cancelled the Save dialogue.
	 * @throws Exception if unable to save the file.
	 */
	public boolean close() throws Exception {
		if (querySave())
			return true;
		if (timersBean != null) {
			timersBean.run(false);
			timersBean.getTimers().clear();
			unhookTimersBean();
		}
		file = null;
		timersBean = null;
		cbxDelayStart.setSelected(false);
		spiMinsOffset.getEditor().setText("");
		setModified(false);
		updateControls();
		resize();
		return false;
	}

	/**
	 * Enables, disables or otherwise updates all controls as appropriate.
	 */
	private void updateControls() {
		boolean disableRun = timersBean == null || timersBean.getTimers().isEmpty();
		TimerState state = timersBean == null ? null : timersBean.getState();
		boolean disableEdits = timersBean == null || state.isRunning() || state == TimerState.WAITING;
		cbxDelayStart.setDisable(disableEdits);
		spiMinsOffset.setDisable(disableEdits || !timersBean.getDelayStart());
		lblMinsOffset.setDisable(disableEdits);
		shpState.setDisable(disableRun);
		btnRun.setDisable(disableRun);
		btnAdd.setDisable(disableEdits);
		Paint fill = Color.LIGHTGREY;
		if (state != null) {
			switch (state) {
			case COMPLETE:
			case STOPPED:
				fill = Color.RED;
				break;
			case WAITING:
				fill = Color.ORANGE;
				break;
			case RUNNING:
			case WARNING:
				fill = Color.GREEN;
				break;
			}
		}
		shpState.setFill(fill);
		((ImageView) btnRun.getGraphic()).setImage( // split
				state != null && (state.isRunning() || state == TimerState.WAITING) // split
						? Images.IMG_STOP
						: Images.IMG_START);
	}

	/**
	 * Creates a new multi-timer instance, querying to save any currently open,
	 * modified instance.
	 * 
	 * @throws Exception if unable to save the file.
	 */
	public void create() throws Exception {
		if (close())
			return;
		timersBean = new MultiTimerBean();
		hookTimersBean();
		setModified(false);
		updateControls();
		resize();
	}

	/**
	 * Deletes a timer instance.
	 * 
	 * @param bean The timer instance that is to be deleted.
	 */
	public void delete(TimerBean bean) {
		if (timersBean != null) {
			((ObservableList<TimerBean>) timersBean.getTimers()).remove(bean);
			setModified(true);
			updateControls();
			resize();
		}
	}

	/**
	 * Exists the application, querying to save any open multi-timer instance before
	 * doing so.
	 * 
	 * @throws Exception if unable to save the file.
	 */
	public void exit() throws Exception {
		if (!close())
			Platform.exit();
	}

	/**
	 * Handles the addition of new timer beans to the current multi-timer instance.
	 * 
	 * @param list The list of all timer beans.
	 * @param from The inclusive start index of the added timer beans.
	 * @param to   The exclusive end index of the added timer beans.
	 * @throws IOException if unable to load FXML resources from the class path.
	 */
	private void handleTimersAdded(List<? extends TimerBean> list, int from, int to) throws IOException {
		URL timerResource = getClass().getResource("/timer.fxml");
		for (int i = from; i < to; i++) {
			TimerBean bean = list.get(i);
			FXMLLoader fxmlLoader = new FXMLLoader(timerResource);
			Parent timer = fxmlLoader.load();
			TimerController controller = fxmlLoader.getController();
			controller.setTimerAppController(this);
			controller.setBean(bean);
			controllers.put(bean, controller);
			vbxTimers.getChildren().add(i, timer);
		}
	}

	/**
	 * Handles the removal of timer beans from the current multi-timer instance.
	 * 
	 * @param list The list of all timer beans.
	 * @param from The inclusive start index of the removed timer beans.
	 * @param to   The exclusive end index of the removed timer beans.
	 */
	private void handleTimersRemoved(List<? extends TimerBean> list, int from, int count) {
		for (int i = 0; i < count; i++) {
			vbxTimers.getChildren().remove(from);
			controllers.remove(list.get(i));
		}
	}

	/**
	 * Registers listeners on the current mukti-timer bean's properties of interest.
	 */
	private void hookTimersBean() {
		if (timersBean != null) {
			BooleanProperty delayStartProperty = cbxDelayStart.selectedProperty();
			delayStartProperty.bindBidirectional(timersBean.delayStartProperty());
			delayStartProperty.addListener((o, ov, nv) -> updateControls());
			StringProperty minutesOffsetProperty = spiMinsOffset.getEditor().textProperty();
			minutesOffsetProperty.bindBidirectional(timersBean.minutesOffsetProperty(), new NumberStringConverter());
			timersBean.stateProperty().addListener((o, ov, nv) -> {
				updateControls();
			});
			delayStartProperty.addListener((o, ov, nv) -> setModified(true));
			minutesOffsetProperty.addListener((o, ov, nv) -> setModified(true));
			((ObservableList<TimerBean>) timersBean.getTimers()).addListener(this);
		}
	}

	/**
	 * Called by the JavaFX framework to initialise the controller.
	 */
	public void initialize() {
		spiMinsOffset.setValueFactory(new IntegerSpinnerValueFactory(0, 59));
		hookTimersBean();
		MultiTimerApp.getStage().setOnCloseRequest(we -> {
			try {
				if (close())
					we.consume();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		updateControls();
	}

	/**
	 * Navigates to the Multi-Timer project's software licence in the default web
	 * browser application.
	 * 
	 * @throws Exception if unable to open the web browser.
	 */
	public void licence() throws Exception {
		Desktop.getDesktop().browse(URI.create("https://www.gnu.org/licenses/gpl-3.0.html"));
	}

	/** {@inheritDoc} */
	@Override
	public void onChanged(Change<? extends TimerBean> c) {
		while (c.next()) {
			try {
				if (c.wasAdded()) {
					handleTimersAdded(c.getList(), c.getFrom(), c.getTo());
				} else if (c.wasRemoved()) {
					handleTimersRemoved(c.getRemoved(), c.getFrom(), c.getRemovedSize());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setModified(true);
		updateControls();
		resize();
	}

	/**
	 * Displays a File Open dialogue and opens the chosen multi-timer file, if any.
	 * 
	 * @throws Exception if unable to save the file.
	 */
	public void open() throws Exception {
		if (close())
			return;
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(EXTENSION_FILTER);
		fc.setSelectedExtensionFilter(EXTENSION_FILTER);
		fc.setTitle("Open Timers File");
		File file = fc.showOpenDialog(MultiTimerApp.getStage());
		if (file != null)
			open(file);
	}

	/**
	 * Opens the specified multi-timer file.
	 * 
	 * @param file The file to open.
	 * @throws Exception if unable to load the file.
	 */
	public void open(File file) throws Exception {
		this.file = file;
		unhookTimersBean();
		timersBean = JSONB.fromJson(new FileInputStream(file), MultiTimerBean.class);
		if (timersBean.getFormatVersion() > Constants.FORMAT_VERSION) {
			Alert dlg = new Alert(AlertType.ERROR);
			dlg.setTitle("Multi-Timer");
			dlg.setHeaderText("Unsupported file version");
			dlg.setContentText(
					"This Multi-Timer file was written by a later version of the application and cannot be opened by this version. Upgrade to the latest version and try again.");
			dlg.getButtonTypes().clear();
			dlg.getButtonTypes().addAll(ButtonType.OK);
			dlg.showAndWait();
			return;
		} else if (timersBean.getFormatVersion() < Constants.FORMAT_VERSION) {
			Alert dlg = new Alert(AlertType.WARNING);
			dlg.setTitle("Multi-Timer");
			dlg.setHeaderText("Old file version");
			dlg.setContentText(
					"This Multi-Timer file was written by an older version of the application and needs to be migrated to the current version. If you do this, it will no longer be readable by older versions of the application. Proceed with migration?");
			dlg.getButtonTypes().clear();
			dlg.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			Optional<ButtonType> result = dlg.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.OK) {
				timersBean = Migration.migrate(timersBean);
			} else {
				return;
			}
		}
		hookTimersBean();
		List<TimerBean> list = timersBean.getTimers();
		handleTimersAdded(list, 0, list.size());
		setModified(false);
		updateControls();
		resize();
	}

	/**
	 * Displays a query dialogue asking whether to save a modified multi-timer
	 * instance, saving if the answer is yes.
	 * 
	 * @return <code>true</code> if the user pressed cancel.
	 * @throws Exception if unable to save the file.
	 */
	private boolean querySave() throws Exception {
		boolean cancel = false;
		if (modified) {
			cancel = true;
			Alert dlg = new Alert(AlertType.WARNING);
			dlg.setTitle("Save");
			dlg.setHeaderText("Timers have been modified.");
			dlg.setContentText("Do you wish to save your changes?");
			dlg.getButtonTypes().clear();
			dlg.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
			Optional<ButtonType> result = dlg.showAndWait();
			if (result.isPresent()) {
				if (result.get() == ButtonType.YES)
					cancel = save();
				else if (result.get() == ButtonType.NO)
					cancel = false;
			}
		}
		return cancel;
	}

	/**
	 * Resizes the main application window so that all controls can be displayed at
	 * their preferred size. Also forces horizontal alignment of column 2,
	 * containing the timer progress monitors.
	 */
	void resize() {
		Platform.runLater(() -> {
			ObservableList<Node> timers = vbxTimers.getChildren();
			if (!timers.isEmpty()) {
				// Determine the widest timer name.
				Optional<Double> maxWidthOpt = timers.stream().map(n -> {
					GridPane grdTimer = (GridPane) n;
					return grdTimer.getChildren().get(0).prefWidth(-1);
				}).max((w1, w2) -> (int) (w1 - w2));
				double maxWidth = maxWidthOpt.get();
				// Force all timer UIs to use the same width for the name column;
				timers.forEach(n -> ((GridPane) n).getColumnConstraints().get(0).setPrefWidth(maxWidth));
			}
			MultiTimerApp.getStage().sizeToScene();
		});
	}

	/**
	 * Starts or stops the current multi-timer instance.
	 */
	public void run() {
		if (timersBean != null) {
			TimerState state = timersBean.getState();
			boolean stop = state.isRunning() || state == TimerState.WAITING;
			timersBean.run(!stop);
		}
	}

	/**
	 * Saves the current multi-timer instance, if any. If the instance has not yet
	 * been saved, shows a File Save dialogue for the file name.
	 * 
	 * @return <code>true</code> if the user cancelled the File Save dialogue.
	 * @throws Exception if unable to save the file.
	 */
	public boolean save() throws Exception {
		if (file == null) {
			return saveAs();
		} else {
			save0();
			return false;
		}
	}

	/**
	 * Unconditionally saves the current multi-timer instance to its associated
	 * file.
	 * 
	 * @throws Exception if unable to save the file.
	 */
	private void save0() throws Exception {
		if (file != null) {
			JSONB.toJson(timersBean, new FileOutputStream(file));
			setModified(false);
		}
	}

	/**
	 * Shows a File Save dialogue and saves the current multi-timer to the selected
	 * file.
	 * 
	 * @return <code>true</code> if the user cancelled the File Save dialogue.
	 * @throws Exception if unable to save the file.
	 */
	public boolean saveAs() throws Exception {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(EXTENSION_FILTER);
		fc.setSelectedExtensionFilter(EXTENSION_FILTER);
		fc.setTitle("Save Timers File");
		if (file != null) {
			fc.setInitialDirectory(file.getParentFile());
			fc.setInitialFileName(file.getName());
		}
		boolean cancel = false;
		File newFile = fc.showSaveDialog(MultiTimerApp.getStage());
		if (newFile != null) {
			file = newFile;
			save0();
		} else {
			cancel = true;
		}
		return cancel;
	}

	/**
	 * Sets the modified state of the current multi-timer instance.
	 * 
	 * @param modified <code>true</code> if the instance has been modified since
	 *                 last being saved.
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
		setWindowTitle();
	}

	/**
	 * Sets the window title to reflect the name of the currently open file, and
	 * whether it has been modified.
	 */
	private void setWindowTitle() {
		String name;
		if (file == null) {
			name = "";
		} else {
			name = file.getName();
			name = " - " + name.substring(0, name.length() - Constants.FILE_EXT_DOT_TIMERS.length());
		}
		MultiTimerApp.getStage().setTitle(Constants.APP_SHORT_NAME + name + (modified ? "*" : ""));
	}

	/**
	 * Removes listeners from the current multi-timer instance.
	 */
	private void unhookTimersBean() {
		if (timersBean != null)
			((ObservableList<TimerBean>) timersBean.getTimers()).removeListener(this);
	}

	/**
	 * Enables or disables File menu items as appropriate.
	 * 
	 * @param e The corresponding event.
	 */
	public void onFileMenuShowing(Event e) {
		boolean empty = timersBean == null;
		miFileSave.setDisable(empty || !modified);
		miFileSaveAs.setDisable(empty);
		miFileClose.setDisable(empty);
	}

	/**
	 * Enables or disables Timer menu items as appropriate.
	 * 
	 * @param e The corresponding event.
	 */
	public void onTimerMenuShowing(Event e) {
		boolean empty = timersBean == null;
		boolean isRunningOrWaiting = !empty && timersBean.getState().isRunningOrWaiting();
		String runText = isRunningOrWaiting ? "_Stop" : "_Start";
		miTimerRun.setText(runText);
		miTimerRun.setDisable(empty || timersBean.getTimers().isEmpty());
		miTimerAdd.setDisable(empty);
	}

	/**
	 * Enables or disables processing of the Save command when invoked using its
	 * accelerator key combination.
	 * 
	 * @param e The corresponding event.
	 */
	public void validateSave(Event e) {
		if (timersBean == null || !modified)
			e.consume();
	}

	/**
	 * Enables or disables processing of the Save As command when invoked using its
	 * accelerator key combination.
	 * 
	 * @param e The corresponding event.
	 */
	public void validateSaveAs(Event e) {
		if (timersBean == null)
			e.consume();
	}

	/**
	 * Enables or disables processing of the Close command when invoked using its
	 * accelerator key combination.
	 * 
	 * @param e The corresponding event.
	 */
	public void validateClose(Event e) {
		if (timersBean == null)
			e.consume();
	}

	/**
	 * Enables or disables processing of the Start or Stop command when invoked
	 * using its accelerator key combination.
	 * 
	 * @param e The corresponding event.
	 */
	public void validateRun(Event e) {
		if (timersBean == null)
			e.consume();
	}

	/**
	 * Enables or disables processing of the Add command when invoked using its
	 * accelerator key combination.
	 * 
	 * @param e The corresponding event.
	 */
	public void validateAdd(Event e) {
		if (timersBean == null)
			e.consume();
	}

	/**
	 * Navigates to the Multi-Timer project's website in the default web browser
	 * application.
	 * 
	 * @throws Exception if unable to open the web browser.
	 */
	public void website() throws IOException {
		Desktop.getDesktop().browse(URI.create("https://github.com/demonfiddler/multi-timer"));
	}
}