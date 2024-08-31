/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP - ongoing enhancements
 *     Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import static org.eclipse.jface.viewers.ColumnViewerEditor.KEYBOARD_ACTIVATION;
import static org.eclipse.jface.viewers.ColumnViewerEditor.TABBING_HORIZONTAL;
import static org.eclipse.jface.viewers.ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR;
import static org.eclipse.jface.viewers.ColumnViewerEditor.TABBING_VERTICAL;
import static org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent.KEY_PRESSED;
import static org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
import static org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent.PROGRAMMATIC;
import static org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent.TRAVERSAL;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.trace.internal.datamodel.TracingCollections;
import org.eclipse.ui.trace.internal.datamodel.TracingComponent;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;
import org.eclipse.ui.trace.internal.datamodel.TracingNode;
import org.eclipse.ui.trace.internal.providers.TracingComponentColumnLabelProvider;
import org.eclipse.ui.trace.internal.providers.TracingComponentContentProvider;
import org.eclipse.ui.trace.internal.providers.TracingComponentLabelProvider;
import org.eclipse.ui.trace.internal.utils.DebugOptionsHandler;
import org.eclipse.ui.trace.internal.utils.PreferenceHandler;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.eclipse.ui.trace.internal.utils.TracingUtils;

/**
 * The 'Tracing' workspace preference page.
 */
public class TracingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/**
	 * The key to activate the value cell editor.
	 * Cannot use the usual enter (CR) key to activate it since this would close the page.  Fix is to use
	 * the space key as replacement.
	 * On Linux, however, space is used as default key on ComboBoxes, leading to the editor being deactivated.
	 * We use F2 instead.
	 */
	private static final int VALUE_EDITOR_ACTIVATION_KEY = (Util.isLinux() || Util.isFreeBSD()) ? SWT.F2 : SWT.SPACE;

	/** A list of {@link TracingComponent} objects to display in the UI */
	private Map<String, TracingComponent> displayableTracingComponents = null;

	// widgets
	/** Enabling Tracing button */
	protected Button enableTracingButton = null;

	/** Tracing tree title*/
	protected Label tracingTreeTitleLabel = null;

	/** A tree that can be filtered based on user input */
	protected FilteredTree filterTree = null;

	/** A {@link Group} containing all of the tracing file options */
	protected Group tracingOptionsGroup = null;

	/** A {@link Text} field for specify the tracing file */
	protected Text tracingFileText = null;

	/** A {@link Text} field for specify the maximum size of the tracing files */
	protected Spinner maximumFileSizeSpinner = null;

	/** A {@link Text} field for specify the maximum number of tracing files */
	protected Spinner maximumFileCountSpinner = null;

	/** A {@link Button} for browsing the file-system for the tracing file */
	protected Button tracingFileBrowseButton = null;

	/** A {@link Button} for 'Output file:' */
	protected Button tracingOutputFileButton = null;

	/** A {@link Label} for 'Number of historical files:' */
	protected Label tracingFileMaxCountLabel = null;

	/** A {@link Label} for 'Maximum file size (KB):' */
	protected Label tracingFileMaxSizeLabel = null;

	/** A {@link Button} for 'Standard output stream:' */
	protected Button standardOutputStreamButton;

	/**
	 * Constructor for {@link TracingPreferencePage}
	 */
	public TracingPreferencePage() {

		setDescription(Messages.preferencePageDescription);
	}

	@Override
	public void init(IWorkbench workbench) {

		// empty implementation
	}

	@Override
	public void dispose() {
		super.dispose();
		disposeWidget(enableTracingButton);
		disposeWidget(tracingTreeTitleLabel);
		disposeWidget(filterTree);
		disposeWidget(tracingOptionsGroup);
		disposeWidget(tracingOutputFileButton);
		disposeWidget(tracingFileText);
		disposeWidget(tracingFileBrowseButton);
		disposeWidget(tracingFileMaxCountLabel);
		disposeWidget(maximumFileCountSpinner);
		disposeWidget(tracingFileMaxSizeLabel);
		disposeWidget(maximumFileSizeSpinner);
		disposeWidget(standardOutputStreamButton);
		purgeModel();
	}

	/**
	 * A utility method to purge all caches used by the preference page
	 */
	private void purgeModel() {

		if (displayableTracingComponents != null) {
			// clear the displayable tracing component cache
			displayableTracingComponents.clear();
			displayableTracingComponents = null;
		}

		// clear the component caches
		TracingCollections.getInstance().clear();
	}

	/**
	 * A utility method to dispose a widget.
	 *
	 * @param widget
	 *            The widget to dispose
	 */
	private void disposeWidget(Widget widget) {

		if (widget != null) {
			if (!widget.isDisposed()) {
				widget.dispose();
			}
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite pageComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(pageComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pageComposite);
		// add the widgets
		addEnableTracingSection(pageComposite);
		addBundleViewerSection(pageComposite);
		addTracingOptionsSection(pageComposite);
		// set the initial values in the widgets
		setUIValuesFromPreferences();
		enableTracingButtonSelected(true);

		// disable after enableTracingButton.
		if (Boolean.parseBoolean(PreferenceHandler.getOutputToStandardStream())) {
			setEnableTracingOutputFile(false);
		}
		// apply the font to this page
		applyDialogFont(pageComposite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(pageComposite, IHelpContextIds.TRACING_PREF_PAGE);

		return pageComposite;
	}

	/**
	 * Add a section for a check button to be displayed like "[] Enable Tracing"
	 *
	 * @param parent
	 *            The parent composite
	 */
	protected void addEnableTracingSection(Composite parent) {
		// If the preferences are being overridden by debug mode at launch time, warn the user
		if (DebugOptionsHandler.isLaunchInDebugMode()) {
			Composite warningComp = new Composite(parent, SWT.NONE);
			GridData gd = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
			warningComp.setLayoutData(gd);
			GridLayout gl = new GridLayout(2, false);
			gl.marginWidth = gl.marginHeight = 0;
			warningComp.setLayout(gl);

			Label warningImage = new Label(warningComp, SWT.NONE);
			warningImage.setImage(JFaceResources.getImage(org.eclipse.jface.dialogs.Dialog.DLG_IMG_MESSAGE_WARNING));
			gd = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
			warningImage.setLayoutData(gd);

			Label warningText = new Label(warningComp, SWT.WRAP);
			warningText.setText(Messages.TracingPreferencePage_applicationLaunchedInDebugModeWarning);
			gd = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
			gd.widthHint = 200;
			warningText.setLayoutData(gd);
		}

		enableTracingButton = new Button(parent, SWT.CHECK);
		enableTracingButton.setText(Messages.enableTracingButtonLabel);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).applyTo(enableTracingButton);
		enableTracingButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableTracingButtonSelected(true);
			}
		});
	}

	/**
	 * Create a viewer for the list of debug options that are traceable in the product.
	 *
	 * @param parent
	 *            The parent composite
	 */
	protected void addBundleViewerSection(Composite parent) {

		tracingTreeTitleLabel = new Label(parent, SWT.NONE);
		tracingTreeTitleLabel.setText(Messages.tracingTreeTile);
		filterTree = new TracingComponentTreeViewer(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(getViewerTree());
		getViewerTree().setHeaderVisible(false);
		getViewer().setLabelProvider(new TracingComponentLabelProvider());
		getViewer().setContentProvider(new TracingComponentContentProvider());
		getViewer().setComparator(new TracingComponentComparator());
		getViewerTree().setLinesVisible(true);
		getViewer().addDoubleClickListener(new TracingDoubleClickListener());
		// change the layout of the tree's composite to TreeColumnLayout so we can setup the columns
		Composite treeViewerComposite = getViewerTree().getParent();
		TreeColumnLayout treeViewerCompositeLayout = new TreeColumnLayout();
		treeViewerComposite.setLayout(treeViewerCompositeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(treeViewerComposite);
		// there will be 2 columns. (1) trace string (2) value (which will only be populated when it is a non-boolean
		// value)
		// [1] Add the label column (75% column width)
		TreeViewerColumn labelColumn = new TreeViewerColumn(getViewer(), SWT.NONE);
		labelColumn.getColumn().setResizable(true);
		labelColumn.getColumn().setText(Messages.columnHeaderTracingString);
		labelColumn.setLabelProvider(new TracingComponentColumnLabelProvider(TracingConstants.LABEL_COLUMN_INDEX));
		treeViewerCompositeLayout.setColumnData(getViewerTree().getColumn(0), new ColumnWeightData(75));
		// [2] Add the value column (25% column width)
		TreeViewerColumn valueColumn = new TreeViewerColumn(getViewer(), SWT.NONE);
		valueColumn.getColumn().setResizable(true);
		valueColumn.getColumn().setText(Messages.columnHeaderTracingValue);
		valueColumn.setLabelProvider(new TracingComponentColumnLabelProvider(TracingConstants.VALUE_COLUMN_INDEX));
		valueColumn.setEditingSupport(new TracingComponentColumnEditingSupport(getViewer(), TracingConstants.VALUE_COLUMN_INDEX));
		treeViewerCompositeLayout.setColumnData(getViewerTree().getColumn(1), new ColumnWeightData(25));

		// install focus cell and cell editor activation for keyboard access (Bug 385100)
		TreeViewerFocusCellManager focusCellManager = new TreeViewerFocusCellManager(getViewer(), new FocusCellOwnerDrawHighlighter(getViewer()));
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(getViewer()) {
			@Override
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return (event.eventType == KEY_PRESSED && event.keyCode == VALUE_EDITOR_ACTIVATION_KEY) //
						|| event.eventType == TRAVERSAL //
						|| event.eventType == MOUSE_CLICK_SELECTION //
						|| event.eventType == PROGRAMMATIC;
			}
		};
		TreeViewerEditor.create(getViewer(), focusCellManager, actSupport, //
				TABBING_HORIZONTAL | TABBING_MOVE_TO_ROW_NEIGHBOR | TABBING_VERTICAL | KEYBOARD_ACTIVATION);
	}

	/**
	 * Create a section for displaying the tracing file options (i.e. maximum size of each file, number of historical
	 * files, etc).
	 *
	 * @param parent
	 *            The parent composite
	 */
	protected void addTracingOptionsSection(Composite parent) {
		tracingOptionsGroup = new Group(parent, SWT.NONE);
		tracingOptionsGroup.setText(Messages.tracingOptionsGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(tracingOptionsGroup);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(tracingOptionsGroup);


		Composite outputComp = new Composite(tracingOptionsGroup, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(outputComp);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).margins(0, 0).applyTo(outputComp);

		// add the 'tracing file' group
		tracingOutputFileButton = new Button(outputComp, SWT.RADIO);
		tracingOutputFileButton.setText(Messages.tracingFileLabel);
		tracingOutputFileButton.setSelection(true); // during creation - select this option
		tracingOutputFileButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnableTracingOutputFile(true);
				standardOutputStreamButton.setSelection(false);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(tracingOutputFileButton);
		// add the 'tracing file' input field
		tracingFileText = new Text(outputComp, SWT.SINGLE | SWT.BORDER);
		tracingFileText.addListener(SWT.Verify, e -> {

			String newInput = TracingPreferencePage.this.getInput(e);
			if ((newInput == null) || newInput.equals(TracingConstants.EMPTY_STRING)) {
				TracingPreferencePage.this.setValid(false);
				TracingPreferencePage.this.setErrorMessage(Messages.tracingFileInvalid);
			} else {
				TracingPreferencePage.this.setValid(true);
				TracingPreferencePage.this.setErrorMessage(null);
			}
		});
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(tracingFileText);
		// add the 'tracing file' browse button
		tracingFileBrowseButton = new Button(outputComp, SWT.PUSH);
		tracingFileBrowseButton.setText(Messages.tracingFileBrowseButton);
		tracingFileBrowseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(tracingOptionsGroup.getShell(), SWT.SAVE);
				dialog.setFileName(tracingFileText.getText());
				String result = dialog.open();
				if (result != null) {
					tracingFileText.setText(result);
				}
			}
		});
		setButtonLayoutData(tracingFileBrowseButton);

		Composite detailsComp = new Composite(tracingOptionsGroup, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(5).equalWidth(false).margins(0, 0).applyTo(detailsComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(detailsComp);

		// add the 'max count' label
		tracingFileMaxCountLabel = new Label(detailsComp, SWT.NONE);
		tracingFileMaxCountLabel.setText(Messages.tracingFileMaxCountLabel);

		// put horizontal indentation
		GridData gridData = new GridData();
		gridData.horizontalIndent = 20;
		tracingFileMaxCountLabel.setLayoutData(gridData);

		// add the 'max count' input field
		maximumFileCountSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		maximumFileCountSpinner.setValues(10, 10, 100, 0, 5, 10);
		maximumFileCountSpinner.addListener(SWT.Verify,
				e -> TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxCount));
		GridDataFactory.fillDefaults().applyTo(maximumFileCountSpinner);

		Label spacer = new Label(detailsComp, SWT.NONE);
		GridDataFactory.fillDefaults().hint(10, 10).applyTo(spacer);

		// add the 'max size' label
		tracingFileMaxSizeLabel = new Label(detailsComp, SWT.NONE);
		tracingFileMaxSizeLabel.setText(Messages.tracingFileMaxSizeLabel);

		// add the 'max size' input field
		maximumFileSizeSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		maximumFileSizeSpinner.setValues(100, 100, 10000, 0, 100, 1000);
		maximumFileSizeSpinner.addListener(SWT.Verify,
				e -> TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxSize));
		GridDataFactory.fillDefaults().applyTo(maximumFileSizeSpinner);


		standardOutputStreamButton = new Button(tracingOptionsGroup, SWT.RADIO);
		standardOutputStreamButton.setText(Messages.TracingPreferencePageStandardOutput);
		standardOutputStreamButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				setEnableTracingOutputFile(false);
				standardOutputStreamButton.setSelection(true);
			}
		});

	}


	/**
	 *	 select the outputFileButton and enable rest of the widgets if enable true
	 *   deselect the outputFileButton and disable rest of the widgets if enable false
	 */
	private void setEnableTracingOutputFile(boolean enable) {
		tracingOutputFileButton.setSelection(enable);
		tracingFileText.setEnabled(enable);
		maximumFileSizeSpinner.setEnabled(enable);
		maximumFileCountSpinner.setEnabled(enable);
		tracingFileBrowseButton.setEnabled(enable);
		tracingFileMaxCountLabel.setEnabled(enable);
		tracingFileMaxSizeLabel.setEnabled(enable);
	}
	/**
	 * Set the default state and value of all the UI elements based on the value in the preferences. This can only be
	 * called after all the UI elements have been constructed.
	 */
	protected void setUIValuesFromPreferences() {
		// If in debug mode, tracing defaults to disabled
		if (!DebugOptionsHandler.isLaunchInDebugMode()) {
			// acquire the preferences service
			boolean tracingEnabled = PreferenceHandler.isTracingEnabled();
			// HACK: because there is no way for me to enable tracing during preference import (??) - I am doing it here.
			if (tracingEnabled && !DebugOptionsHandler.getDebugOptions().isDebugEnabled()) {
				// the preferences say that tracing should be enabled
				DebugOptionsHandler.getDebugOptions().setDebugEnabled(true);
			}
			// set enablement button
			enableTracingButton.setSelection(tracingEnabled);
		} else {
			enableTracingButton.setSelection(false);
		}

		tracingOutputFileButton.setSelection(!Boolean.parseBoolean(PreferenceHandler.getOutputToStandardStream()));
		// set the tracing file name.
		tracingFileText.setText(PreferenceHandler.getFilePath());

		// default location needs to be put in preference store once ( fringe scenario)
		final IEclipsePreferences preferences = PreferenceHandler.getPreferences();
		preferences.put(TracingConstants.PREFERENCE_FILE_PATH, PreferenceHandler.getFilePath());

		// set the max counter field
		maximumFileCountSpinner.setSelection(PreferenceHandler.getMaxFileCount());
		// set the max file size field
		maximumFileSizeSpinner.setSelection(PreferenceHandler.getMaxFileSize());
		// update the enablement state of all the UI elements
		enableTracingButtonSelected(false);
		standardOutputStreamButton.setSelection(Boolean.parseBoolean(PreferenceHandler.getOutputToStandardStream()));
	}

	/**
	 * Retrieve the input
	 */
	protected String getInput(Event ev) {
		String input = null;
		if ((ev.keyCode == SWT.DEL) || (ev.keyCode == SWT.BS)) {
			String currentValue = ((Text) ev.widget).getText();
			String begin = currentValue.substring(0, ev.start);
			String end = currentValue.substring(ev.start + 1, currentValue.length());
			input = begin + end;
		} else {
			input = ev.text;
		}
		return input;
	}

	/**
	 * Verify that the user input is a valid int field
	 *
	 * @param errorMessage to display if the input is not a valid int field
	 */
	protected void verifyIntInput(Event ev, String errorMessage) {
		String input = getInput(ev);
		// value the input
		if (input.length() <= 0) {
			setValid(false);
			setErrorMessage(errorMessage);
			return;
		}
		// ensure the characters in the string are numbers between 0 - 9
		char[] chars = new char[input.length()];
		input.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			if (!(('0' <= chars[i]) && (chars[i] <= '9'))) {
				setValid(false);
				setErrorMessage(errorMessage);
				return;
			}
		}
		// everything is OK!
		setValid(true);
		setErrorMessage(null);
	}

	/**
	 * This method is used to react to the enablement button ('Enable Tracing') state changing.
	 *
	 * @param updateModel
	 *            Should the model be modified for this call? or should only the enablement state of the UI widgets be
	 *            modified?
	 */
	protected void enableTracingButtonSelected(boolean updateModel) {
		boolean enableTracing = enableTracingButton.getSelection();
		if (updateModel) {
			// populate the table
			if (enableTracing) {
				// rebuild the model
				buildDisplayableComponents();
				// set the viewers input
				Collection<TracingComponent> components = displayableTracingComponents.values();
				TracingComponent[] componentsArray = components.toArray(new TracingComponent[components.size()]);
				getViewer().setInput(componentsArray);
			} else {
				// destroy the model
				purgeModel();
				// set the viewers input to null
				getViewer().setInput(null);
			}
		}
		tracingTreeTitleLabel.setEnabled(enableTracing);
		filterTree.setEnabled(enableTracing);
		// only enable the tracing options if tracing is enabled
		tracingOptionsGroup.setEnabled(enableTracing);
		tracingFileText.setEnabled(enableTracing);
		maximumFileSizeSpinner.setEnabled(enableTracing);
		maximumFileCountSpinner.setEnabled(enableTracing);
		tracingFileBrowseButton.setEnabled(enableTracing);
		tracingOutputFileButton.setEnabled(enableTracing);
		tracingFileMaxCountLabel.setEnabled(enableTracing);
		tracingFileMaxSizeLabel.setEnabled(enableTracing);
		standardOutputStreamButton.setEnabled(enableTracing);
	}

	/**
	 * This method will build the list of tracing components that will be used as input to the tree viewer.
	 */
	private void buildDisplayableComponents() {
		// ensure the model is destroyed (start from scratch)
		if (displayableTracingComponents != null) {
			purgeModel();
		}
		displayableTracingComponents = new HashMap<>();
		// look for extension points
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(TracingConstants.BUNDLE_ID, TracingConstants.TRACING_EXTENSION_POINT_NAME);
		TracingComponent[] components = new TracingComponent[cf.length];
		// populate the full list of tracing components
		for (int i = 0; i < cf.length; i++) {
			components[i] = TracingCollections.getInstance().getTracingComponent(cf[i]);
			// if this component already exists or consumed then do not add it to the displayable list
			if (!displayableTracingComponents.containsKey(components[i].getId()) && !components[i].isConsumed()) {
				displayableTracingComponents.put(components[i].getId(), components[i]);
			}
		}
		// now that the components are created - populate the debug options for each component
		Iterator<TracingComponent> componentIterator = displayableTracingComponents.values().iterator();
		while (componentIterator.hasNext()) {
			componentIterator.next().initialize();
		}
		// update any TracingComponentDebugOption entries if their value differs from the preference value
		mergePrefsWithDebugOptions();
	}

	/**
	 * This method will merge the value of the options stored in the preferences with the value in the debug options.
	 */
	private void mergePrefsWithDebugOptions() {
		Map<String, String> prefDebugOptions = PreferenceHandler.getPreferenceProperties();
		// get all debug options (this ensures that the disabled debug options are used when populating)
		Map<String, String> debugOptions = DebugOptionsHandler.getDebugOptions().getOptions();
		Iterator<Map.Entry<String, String>> prefDebugOptionsIterator = prefDebugOptions.entrySet().iterator();
		while (prefDebugOptionsIterator.hasNext()) {
			Map.Entry<String, String> prefDebugOption = prefDebugOptionsIterator.next();
			String debugOptionsValue = debugOptions.get(prefDebugOption.getValue());
			boolean updateDebugOption = true;
			if (debugOptionsValue != null) {
				if (TracingUtils.isValueBoolean(debugOptionsValue) && TracingUtils.isValueBoolean(prefDebugOption.getValue())) {
					// pick the one that is 'true'
					boolean optionValue = Boolean.parseBoolean(debugOptionsValue);
					boolean prefValue = Boolean.parseBoolean(prefDebugOption.getValue());
					if (prefValue != optionValue) {
						// if the preference value is 'true' then use it... otherwise do nothing since the value
						// in the debug options will be used.
						updateDebugOption = prefValue;
					}
				} else {
					// non-boolean values: pick the one in the preferences if they do not equal
					if (!debugOptionsValue.equals(prefDebugOption.getValue())) {
						updateDebugOption = true;
					}
				}
			}
			if (updateDebugOption) {
				// find identical debug options and update them (this will include 'this' debug option that
				// was modified)
				TracingComponentDebugOption[] identicalOptions = TracingCollections.getInstance().getTracingDebugOptions(prefDebugOption.getKey());
				for (TracingComponentDebugOption identicalOption : identicalOptions) {
					identicalOption.setOptionPathValue(prefDebugOption.getValue());
				}
			}
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		// set the options to be an empty set
		DebugOptionsHandler.getDebugOptions().setOptions(new HashMap<>());
		// destroy the model so that it can be re-built.
		purgeModel();
		// set the viewers input to null
		getViewer().setInput(null);
		// set the UI back to the default (tracing off, etc).
		PreferenceHandler.setDefaultPreferences();
		TracingCollections.getInstance().getModifiedDebugOptions().clear();
		setUIValuesFromPreferences();
	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		// get the enablement state
		boolean enableTracing = enableTracingButton.getSelection();

		if (DebugOptionsHandler.isLaunchInDebugMode()) {
			if (enableTracing) {
				// User wants to override current settings
				DebugOptionsHandler.setLaunchInDebugMode(false);
			} else {
				// Don't change tracing options
				return result;
			}
		}

		// save the preferences
		savePreferences(enableTracing);
		DebugOptionsHandler.setDebugEnabled(enableTracing);
		DebugOptionsHandler.getDebugOptions().setFile(standardOutputStreamButton.getSelection() ? null : new File(tracingFileText.getText()));
		if (enableTracing) {
			Map<String, String> newOptions = new HashMap<>();
			// get the set of options available
			Map<String, String> currentOptions = DebugOptionsHandler.getDebugOptions().getOptions();
			newOptions.putAll(currentOptions);
			// iterate over the list of added debug options and add them
			TracingComponentDebugOption[] optionsToAdd = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToAdd();
			for (TracingComponentDebugOption element : optionsToAdd) {
				newOptions.put(element.getOptionPath(), element.getOptionPathValue());
			}
			// iterate over the list of removed debug options and remove them
			TracingComponentDebugOption[] optionsToRemove = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToRemove();
			for (TracingComponentDebugOption element : optionsToRemove) {
				newOptions.remove(element.getOptionPath());
			}
			// update the debug options
			DebugOptionsHandler.getDebugOptions().setOptions(newOptions);
			TracingCollections.getInstance().getModifiedDebugOptions().clear();
			// save the tracing file options
			if (DebugOptionsHandler.getDebugOptions().getFile() != null) {
				String defaultFile = DebugOptionsHandler.getDebugOptions().getFile().getAbsolutePath();
				String newFile = tracingFileText.getText();
				if (!defaultFile.equals(newFile)) {
					DebugOptionsHandler.getDebugOptions().setFile(new File(newFile));
				}
			}
			// maximum file size
			int newMaxSize = maximumFileSizeSpinner.getSelection();
			// property defined in org.eclipse.osgi.framework.debug.EclipseDebugTrace#PROP_TRACE_SIZE_MAX
			System.setProperty(TracingConstants.PROP_TRACE_SIZE_MAX, String.valueOf(newMaxSize));
			// maximum file count
			int newMaxCount = maximumFileCountSpinner.getSelection();
			// property defined in org.eclipse.osgi.framework.debug.EclipseDebugTrace#PROP_TRACE_FILE_MAX
			System.setProperty(TracingConstants.PROP_TRACE_FILE_MAX, String.valueOf(newMaxCount));
		}
		return result;
	}

	/**
	 * Save the property page preferences.
	 */
	protected void savePreferences(boolean tracingEnabled) {
		Map<String, String> prefValues = new HashMap<>(5);
		prefValues.put(TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, Boolean.toString(tracingEnabled));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, Integer.toString(maximumFileCountSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, Integer.toString(maximumFileSizeSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_FILE_PATH, tracingFileText.getText());
		prefValues.put(TracingConstants.PREFERENCE_OUTPUT_STANDARD_STREAM, Boolean.toString(standardOutputStreamButton.getSelection()));
		// iterate over the displayable components and store their debug options (all trace strings should be saved)
		StringBuilder optionsAsString = new StringBuilder();
		if (displayableTracingComponents != null) {
			Iterator<Map.Entry<String, TracingComponent>> componentIterator = displayableTracingComponents.entrySet().iterator();
			while (componentIterator.hasNext()) {
				TracingComponent component = componentIterator.next().getValue();
				if (component.hasChildren()) {
					StringBuilder result = getAllUniqueDebugOptions(component);
					if (result != null) {
						optionsAsString.append(result);
					}
				}
			}
		} else {
			optionsAsString.append(TracingConstants.EMPTY_STRING);
		}
		prefValues.put(TracingConstants.PREFERENCE_ENTRIES_IDENTIFIER, optionsAsString.toString());
		PreferenceHandler.savePreferences(prefValues);
	}

	private StringBuilder getAllUniqueDebugOptions(TracingNode node) {
		StringBuilder buffer = null;
		if (node.hasChildren()) {
			buffer = new StringBuilder();
			TracingNode[] children = node.getChildren();
			for (TracingNode element : children) {
				// add this child node (all child nodes will be of type TracingComponentDebugOption)
				String debugOptionAsString = TracingUtils.convertToString((TracingComponentDebugOption) element);
				buffer.append(debugOptionAsString);
				// add all of this childs nodes
				StringBuilder result = getAllUniqueDebugOptions(element);
				if (result != null) {
					buffer.append(result);
				}
			}
		}
		return buffer;
	}

	/**
	 * The {@link TreeViewer} for the tracing page
	 *
	 * @return The {@link TreeViewer} for the tracing page
	 */
	private TreeViewer getViewer() {
		return filterTree.getViewer();
	}

	/**
	 * Accessor for the {@link Tree} of the {@link CheckboxTreeViewer}
	 *
	 * @return The {@link Tree} of the {@link CheckboxTreeViewer}
	 */
	private Tree getViewerTree() {
		return filterTree.getViewer().getTree();
	}
}