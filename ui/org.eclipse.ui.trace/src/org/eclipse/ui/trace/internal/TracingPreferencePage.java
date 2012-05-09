/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.layout.*;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.trace.internal.datamodel.*;
import org.eclipse.ui.trace.internal.providers.*;
import org.eclipse.ui.trace.internal.utils.*;

/**
 * The 'Product Tracing' workspace preference page.
 */
public class TracingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

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

	/** A {@link Label} for 'Output file:' */
	protected Label tracingFileLabel = null;

	/** A {@link Label} for 'Number of historical files:' */
	protected Label tracingFileMaxCountLabel = null;

	/** A {@link Label} for 'Maximum file size (KB):' */
	protected Label tracingFileMaxSizeLabel = null;

	/**
	 * Constructor for {@link TracingPreferencePage}
	 */
	public TracingPreferencePage() {

		setDescription(Messages.preferencePageDescription);
	}

	public void init(IWorkbench workbench) {

		// empty implementation
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		super.dispose();
		disposeWidget(enableTracingButton);
		disposeWidget(tracingTreeTitleLabel);
		disposeWidget(filterTree);
		disposeWidget(tracingOptionsGroup);
		disposeWidget(tracingFileLabel);
		disposeWidget(tracingFileText);
		disposeWidget(tracingFileBrowseButton);
		disposeWidget(tracingFileMaxCountLabel);
		disposeWidget(maximumFileCountSpinner);
		disposeWidget(tracingFileMaxSizeLabel);
		disposeWidget(maximumFileSizeSpinner);
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
		// apply the font to this page
		applyDialogFont(pageComposite);
		// set focus on the enablement button
		enableTracingButton.setFocus();
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
		getViewer().setUseHashlookup(true);
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

		// add the 'tracing file' label
		tracingFileLabel = new Label(outputComp, SWT.NONE);
		tracingFileLabel.setText(Messages.tracingFileLabel);
		// add the 'tracing file' input field
		tracingFileText = new Text(outputComp, SWT.SINGLE | SWT.BORDER);
		tracingFileText.addListener(SWT.Verify, new Listener() {

			public void handleEvent(Event e) {

				String newInput = TracingPreferencePage.this.getInput(e);
				if ((newInput == null) || newInput.equals(TracingConstants.EMPTY_STRING)) {
					TracingPreferencePage.this.setValid(false);
					TracingPreferencePage.this.setErrorMessage(Messages.tracingFileInvalid);
				} else {
					TracingPreferencePage.this.setValid(true);
					TracingPreferencePage.this.setErrorMessage(null);
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(tracingFileText);
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
		GridDataFactory.fillDefaults().applyTo(tracingFileBrowseButton);

		Composite detailsComp = new Composite(tracingOptionsGroup, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(5).equalWidth(false).margins(0, 0).applyTo(detailsComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(detailsComp);

		// add the 'max count' label
		tracingFileMaxCountLabel = new Label(detailsComp, SWT.NONE);
		tracingFileMaxCountLabel.setText(Messages.tracingFileMaxCountLabel);

		// add the 'max count' input field
		maximumFileCountSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		maximumFileCountSpinner.setValues(10, 10, 100, 0, 5, 10);
		maximumFileCountSpinner.addListener(SWT.Verify, new Listener() {

			public void handleEvent(Event e) {

				TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxCount);
			}
		});
		GridDataFactory.fillDefaults().applyTo(maximumFileCountSpinner);

		Label spacer = new Label(detailsComp, SWT.NONE);
		GridDataFactory.fillDefaults().hint(10, 10).applyTo(spacer);

		// add the 'max size' label
		tracingFileMaxSizeLabel = new Label(detailsComp, SWT.NONE);
		tracingFileMaxSizeLabel.setText(Messages.tracingFileMaxSizeLabel);

		// add the 'max size' input field
		maximumFileSizeSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		maximumFileSizeSpinner.setValues(100, 100, 10000, 0, 100, 1000);
		maximumFileSizeSpinner.addListener(SWT.Verify, new Listener() {

			public void handleEvent(Event e) {

				TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxSize);
			}
		});
		GridDataFactory.fillDefaults().applyTo(maximumFileSizeSpinner);
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

		// set the tracing file name.
		tracingFileText.setText(PreferenceHandler.getFilePath());
		// set the max counter field
		maximumFileCountSpinner.setSelection(PreferenceHandler.getMaxFileCount());
		// set the max file size field
		maximumFileSizeSpinner.setSelection(PreferenceHandler.getMaxFileSize());
		// update the enablement state of all the UI elements
		enableTracingButtonSelected(false);
	}

	/**
	 * Retrieve the input
	 * 
	 * @param ev
	 * @return input
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
	 * @param input
	 *            The user supplied input
	 * @param errorMessage
	 *            The error message to display if the input is not a valid int field
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
		tracingFileLabel.setEnabled(enableTracing);
		tracingFileMaxCountLabel.setEnabled(enableTracing);
		tracingFileMaxSizeLabel.setEnabled(enableTracing);
	}

	/**
	 * This method will build the list of tracing components that will be used as input to the tree viewer.
	 */
	private void buildDisplayableComponents() {
		// ensure the model is destroyed (start from scratch)
		if (displayableTracingComponents != null) {
			purgeModel();
		}
		displayableTracingComponents = new HashMap<String, TracingComponent>();
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
					boolean optionValue = Boolean.valueOf(debugOptionsValue).booleanValue();
					boolean prefValue = Boolean.valueOf(prefDebugOption.getValue()).booleanValue();
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
				for (int identicalOptionsIndex = 0; identicalOptionsIndex < identicalOptions.length; identicalOptionsIndex++) {
					identicalOptions[identicalOptionsIndex].setOptionPathValue(prefDebugOption.getValue());
				}
			}
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		// set the options to be an empty set
		DebugOptionsHandler.getDebugOptions().setOptions(new HashMap<String, String>());
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
		if (enableTracing) {
			Map<String, String> newOptions = new HashMap<String, String>();
			// get the set of options available
			Map<String, String> currentOptions = DebugOptionsHandler.getDebugOptions().getOptions();
			newOptions.putAll(currentOptions);
			// iterate over the list of added debug options and add them
			TracingComponentDebugOption[] optionsToAdd = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToAdd();
			for (int i = 0; i < optionsToAdd.length; i++) {
				newOptions.put(optionsToAdd[i].getOptionPath(), optionsToAdd[i].getOptionPathValue());
			}
			// iterate over the list of removed debug options and remove them
			TracingComponentDebugOption[] optionsToRemove = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToRemove();
			for (int i = 0; i < optionsToRemove.length; i++) {
				newOptions.remove(optionsToRemove[i].getOptionPath());
			}
			// update the debug options
			DebugOptionsHandler.getDebugOptions().setOptions(newOptions);
			TracingCollections.getInstance().getModifiedDebugOptions().clear();
			// save the tracing file options
			String defaultFile = DebugOptionsHandler.getDebugOptions().getFile().getAbsolutePath();
			String newFile = tracingFileText.getText();
			if (!defaultFile.equals(newFile)) {
				DebugOptionsHandler.getDebugOptions().setFile(new File(newFile));
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
		Map<String, String> prefValues = new HashMap<String, String>(5);
		prefValues.put(TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, Boolean.toString(tracingEnabled));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, Integer.toString(maximumFileCountSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, Integer.toString(maximumFileSizeSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_FILE_PATH, tracingFileText.getText());
		// iterate over the displayable components and store their debug options (all trace strings should be saved)
		StringBuffer optionsAsString = new StringBuffer();
		if (displayableTracingComponents != null) {
			Iterator<Map.Entry<String, TracingComponent>> componentIterator = displayableTracingComponents.entrySet().iterator();
			while (componentIterator.hasNext()) {
				TracingComponent component = componentIterator.next().getValue();
				if (component.hasChildren()) {
					StringBuffer result = getAllUniqueDebugOptions(component);
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

	private StringBuffer getAllUniqueDebugOptions(TracingNode node) {
		StringBuffer buffer = null;
		if (node.hasChildren()) {
			buffer = new StringBuffer();
			TracingNode[] children = node.getChildren();
			for (int i = 0; i < children.length; i++) {
				// add this child node (all child nodes will be of type TracingComponentDebugOption)
				String debugOptionAsString = TracingUtils.convertToString((TracingComponentDebugOption) children[i]);
				buffer.append(debugOptionAsString);
				// add all of this childs nodes
				StringBuffer result = getAllUniqueDebugOptions(children[i]);
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