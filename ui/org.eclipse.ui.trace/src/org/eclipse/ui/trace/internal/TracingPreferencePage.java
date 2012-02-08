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
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.trace.internal.datamodel.*;
import org.eclipse.ui.trace.internal.providers.*;
import org.eclipse.ui.trace.internal.utils.*;

/**
 * The 'Product Tracing' workspace preference page.
 */
public class TracingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();

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

		this.setDescription(Messages.preferencePageDescription);
	}

	public void init(final IWorkbench workbench) {

		// empty implementation
	}

	public void dispose() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		super.dispose();
		this.disposeWidget(this.enableTracingButton);
		this.disposeWidget(this.tracingTreeTitleLabel);
		this.disposeWidget(this.filterTree);
		this.disposeWidget(this.tracingOptionsGroup);
		this.disposeWidget(this.tracingFileLabel);
		this.disposeWidget(this.tracingFileText);
		this.disposeWidget(this.tracingFileBrowseButton);
		this.disposeWidget(this.tracingFileMaxCountLabel);
		this.disposeWidget(this.maximumFileCountSpinner);
		this.disposeWidget(this.tracingFileMaxSizeLabel);
		this.disposeWidget(this.maximumFileSizeSpinner);
		this.purgeModel();
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	/**
	 * A utility method to purge all caches used by the preference page
	 */
	private void purgeModel() {

		if (this.displayableTracingComponents != null) {
			// clear the displayable tracing component cache
			this.displayableTracingComponents.clear();
			this.displayableTracingComponents = null;
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
	private void disposeWidget(final Widget widget) {

		if (widget != null) {
			if (TracingUIActivator.DEBUG_UI) {
				TRACE.trace(TracingConstants.TRACE_UI_STRING, "Disposing widget: " + widget); //$NON-NLS-1$
			}
			if (!widget.isDisposed()) {
				widget.dispose();
			}
		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, parent);
		}
		final Composite pageComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(pageComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pageComposite);
		// add the widgets
		this.addEnableTracingSection(pageComposite);
		this.addBundleViewerSection(pageComposite);
		this.addTracingOptionsSection(pageComposite);
		// set the initial values in the widgets
		this.setUIValuesFromPreferences();
		this.enableTracingButtonSelected(true);
		// apply the font to this page
		this.applyDialogFont(pageComposite);
		// set focus on the enablement button
		this.enableTracingButton.setFocus();
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING, parent);
		}
		return pageComposite;
	}

	/**
	 * Add a section for a check button to be displayed like "[] Enable Tracing"
	 * 
	 * @param parent
	 *            The parent composite
	 */
	protected void addEnableTracingSection(final Composite parent) {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, parent);
		}

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

		this.enableTracingButton = new Button(parent, SWT.CHECK);
		this.enableTracingButton.setText(Messages.enableTracingButtonLabel);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).applyTo(this.enableTracingButton);
		this.enableTracingButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (TracingUIActivator.DEBUG_UI_LISTENERS) {
					TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "'Enable Tracing' button was selected"); //$NON-NLS-1$
				}
				enableTracingButtonSelected(true);
			}
		});
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING, parent);
		}
	}

	/**
	 * Create a viewer for the list of debug options that are traceable in the product.
	 * 
	 * @param parent
	 *            The parent composite
	 */
	protected void addBundleViewerSection(final Composite parent) {

		this.tracingTreeTitleLabel = new Label(parent, SWT.NONE);
		this.tracingTreeTitleLabel.setText(Messages.tracingTreeTile);

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, parent);
		}
		this.filterTree = new TracingComponentTreeViewer(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(this.getViewerTree());
		this.getViewer().setUseHashlookup(true);
		this.getViewerTree().setHeaderVisible(false);
		this.getViewer().setLabelProvider(new TracingComponentLabelProvider());
		this.getViewer().setContentProvider(new TracingComponentContentProvider());
		this.getViewer().setComparator(new TracingComponentComparator());
		this.getViewerTree().setLinesVisible(true);
		this.getViewer().addDoubleClickListener(new TracingDoubleClickListener());
		// change the layout of the tree's composite to TreeColumnLayout so we can setup the columns
		final Composite treeViewerComposite = this.getViewerTree().getParent();
		final TreeColumnLayout treeViewerCompositeLayout = new TreeColumnLayout();
		treeViewerComposite.setLayout(treeViewerCompositeLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(treeViewerComposite);
		// there will be 2 columns. (1) trace string (2) value (which will only be populated when it is a non-boolean
		// value)
		// [1] Add the label column (75% column width)
		final TreeViewerColumn labelColumn = new TreeViewerColumn(this.getViewer(), SWT.NONE);
		labelColumn.getColumn().setResizable(true);
		labelColumn.getColumn().setText(Messages.columnHeaderTracingString);
		labelColumn.setLabelProvider(new TracingComponentColumnLabelProvider(TracingConstants.LABEL_COLUMN_INDEX));
		treeViewerCompositeLayout.setColumnData(this.getViewerTree().getColumn(0), new ColumnWeightData(75));
		// [2] Add the value column (25% column width)
		final TreeViewerColumn valueColumn = new TreeViewerColumn(this.getViewer(), SWT.NONE);
		valueColumn.getColumn().setResizable(true);
		valueColumn.getColumn().setText(Messages.columnHeaderTracingValue);
		valueColumn.setLabelProvider(new TracingComponentColumnLabelProvider(TracingConstants.VALUE_COLUMN_INDEX));
		valueColumn.setEditingSupport(new TracingComponentColumnEditingSupport(this.getViewer(), TracingConstants.VALUE_COLUMN_INDEX));
		treeViewerCompositeLayout.setColumnData(this.getViewerTree().getColumn(1), new ColumnWeightData(25));
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	/**
	 * Create a section for displaying the tracing file options (i.e. maximum size of each file, number of historical
	 * files, etc).
	 * 
	 * @param parent
	 *            The parent composite
	 */
	protected void addTracingOptionsSection(final Composite parent) {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, parent);
		}
		this.tracingOptionsGroup = new Group(parent, SWT.NONE);
		this.tracingOptionsGroup.setText(Messages.tracingOptionsGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(this.tracingOptionsGroup);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(this.tracingOptionsGroup);

		Composite outputComp = new Composite(this.tracingOptionsGroup, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(outputComp);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).margins(0, 0).applyTo(outputComp);

		// add the 'tracing file' label
		this.tracingFileLabel = new Label(outputComp, SWT.NONE);
		this.tracingFileLabel.setText(Messages.tracingFileLabel);
		// add the 'tracing file' input field
		this.tracingFileText = new Text(outputComp, SWT.SINGLE | SWT.BORDER);
		this.tracingFileText.addListener(SWT.Verify, new Listener() {

			public void handleEvent(final Event e) {

				final String newInput = TracingPreferencePage.this.getInput(e);
				if ((newInput == null) || newInput.equals(TracingConstants.EMPTY_STRING)) {
					TracingPreferencePage.this.setValid(false);
					TracingPreferencePage.this.setErrorMessage(Messages.tracingFileInvalid);
				} else {
					TracingPreferencePage.this.setValid(true);
					TracingPreferencePage.this.setErrorMessage(null);
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(this.tracingFileText);
		// add the 'tracing file' browse button
		this.tracingFileBrowseButton = new Button(outputComp, SWT.PUSH);
		this.tracingFileBrowseButton.setText(Messages.tracingFileBrowseButton);
		this.tracingFileBrowseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {

				final FileDialog dialog = new FileDialog(TracingPreferencePage.this.tracingOptionsGroup.getShell(), SWT.SAVE);
				dialog.setFileName(TracingPreferencePage.this.tracingFileText.getText());
				final String result = dialog.open();
				if (result != null) {
					TracingPreferencePage.this.tracingFileText.setText(result);
				}
			}
		});
		GridDataFactory.fillDefaults().applyTo(this.tracingFileBrowseButton);

		Composite detailsComp = new Composite(this.tracingOptionsGroup, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(5).equalWidth(false).margins(0, 0).applyTo(detailsComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(detailsComp);

		// add the 'max count' label
		this.tracingFileMaxCountLabel = new Label(detailsComp, SWT.NONE);
		this.tracingFileMaxCountLabel.setText(Messages.tracingFileMaxCountLabel);

		// add the 'max count' input field
		this.maximumFileCountSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		this.maximumFileCountSpinner.setValues(10, 10, 100, 0, 5, 10);
		this.maximumFileCountSpinner.addListener(SWT.Verify, new Listener() {

			public void handleEvent(final Event e) {

				TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxCount);
			}
		});
		GridDataFactory.fillDefaults().applyTo(this.maximumFileCountSpinner);

		Label spacer = new Label(detailsComp, SWT.NONE);
		GridDataFactory.fillDefaults().hint(10, 10).applyTo(spacer);

		// add the 'max size' label
		this.tracingFileMaxSizeLabel = new Label(detailsComp, SWT.NONE);
		this.tracingFileMaxSizeLabel.setText(Messages.tracingFileMaxSizeLabel);

		// add the 'max size' input field
		this.maximumFileSizeSpinner = new Spinner(detailsComp, SWT.SINGLE | SWT.BORDER);
		this.maximumFileSizeSpinner.setValues(100, 100, 10000, 0, 100, 1000);
		this.maximumFileSizeSpinner.addListener(SWT.Verify, new Listener() {

			public void handleEvent(final Event e) {

				TracingPreferencePage.this.verifyIntInput(e, Messages.tracingFileInvalidMaxSize);
			}
		});
		GridDataFactory.fillDefaults().applyTo(this.maximumFileSizeSpinner);
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING, parent);
		}
	}

	/**
	 * Set the default state and value of all the UI elements based on the value in the preferences. This can only be
	 * called after all the UI elements have been constructed.
	 */
	protected void setUIValuesFromPreferences() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}

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
			this.enableTracingButton.setSelection(tracingEnabled);
		} else {
			this.enableTracingButton.setSelection(false);
		}

		// set the tracing file name.
		this.tracingFileText.setText(PreferenceHandler.getFilePath());
		// set the max counter field
		this.maximumFileCountSpinner.setSelection(PreferenceHandler.getMaxFileCount());
		// set the max file size field
		this.maximumFileSizeSpinner.setSelection(PreferenceHandler.getMaxFileSize());
		// update the enablement state of all the UI elements
		this.enableTracingButtonSelected(false);
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	/**
	 * Retrieve the input
	 * 
	 * @param ev
	 * @return input
	 */
	protected final String getInput(final Event ev) {

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
	protected final void verifyIntInput(final Event ev, final String errorMessage) {

		final String input = this.getInput(ev);
		// value the input
		if (input.length() <= 0) {
			this.setValid(false);
			this.setErrorMessage(errorMessage);
			return;
		}
		// ensure the characters in the string are numbers between 0 - 9
		char[] chars = new char[input.length()];
		input.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			if (!(('0' <= chars[i]) && (chars[i] <= '9'))) {
				this.setValid(false);
				this.setErrorMessage(errorMessage);
				return;
			}
		}
		// everything is OK!
		this.setValid(true);
		this.setErrorMessage(null);
	}

	/**
	 * This method is used to react to the enablement button ('Enable Tracing') state changing.
	 * 
	 * @param updateModel
	 *            Should the model be modified for this call? or should only the enablement state of the UI widgets be
	 *            modified?
	 */
	protected void enableTracingButtonSelected(final boolean updateModel) {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		boolean enableTracing = this.enableTracingButton.getSelection();
		if (updateModel) {
			// populate the table
			if (enableTracing) {
				// rebuild the model
				this.buildDisplayableComponents();
				// set the viewers input
				Collection<TracingComponent> components = this.displayableTracingComponents.values();
				TracingComponent[] componentsArray = components.toArray(new TracingComponent[components.size()]);
				this.getViewer().setInput(componentsArray);
			} else {
				// destroy the model
				this.purgeModel();
				// set the viewers input to null
				this.getViewer().setInput(null);
			}
		}
		this.tracingTreeTitleLabel.setEnabled(enableTracing);
		this.filterTree.setEnabled(enableTracing);
		// only enable the tracing options if tracing is enabled
		this.tracingOptionsGroup.setEnabled(enableTracing);
		this.tracingFileText.setEnabled(enableTracing);
		this.maximumFileSizeSpinner.setEnabled(enableTracing);
		this.maximumFileCountSpinner.setEnabled(enableTracing);
		this.tracingFileBrowseButton.setEnabled(enableTracing);
		this.tracingFileLabel.setEnabled(enableTracing);
		this.tracingFileMaxCountLabel.setEnabled(enableTracing);
		this.tracingFileMaxSizeLabel.setEnabled(enableTracing);
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	/**
	 * This method will build the list of tracing components that will be used as input to the tree viewer.
	 */
	private void buildDisplayableComponents() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		// ensure the model is destroyed (start from scratch)
		if (this.displayableTracingComponents != null) {
			this.purgeModel();
		}
		this.displayableTracingComponents = new HashMap<String, TracingComponent>();
		// look for extension points
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Extension registry :" + registry); //$NON-NLS-1$ 
		}
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(TracingConstants.BUNDLE_ID, TracingConstants.TRACING_EXTENSION_POINT_NAME);
		if (TracingUIActivator.DEBUG_UI) {
			TracingUIActivator.getDefault().getTrace().trace(TracingConstants.TRACE_UI_STRING, "Found " + cf.length + " extensions in namespace '" + TracingConstants.BUNDLE_ID + "' with the extension point name '" + TracingConstants.TRACING_EXTENSION_POINT_NAME + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		TracingComponent[] components = new TracingComponent[cf.length];
		// populate the full list of tracing components
		for (int i = 0; i < cf.length; i++) {
			components[i] = TracingCollections.getInstance().getTracingComponent(cf[i]);
			if (TracingUIActivator.DEBUG_UI) {
				TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Built a TracingComponent for traceComponent with id " + components[i].getId()); //$NON-NLS-1$
			}
			// if this component already exists or consumed then do not add it to the displayable list
			if (!this.displayableTracingComponents.containsKey(components[i].getId()) && !components[i].isConsumed()) {
				if (TracingUIActivator.DEBUG_UI) {
					TRACE.trace(TracingConstants.TRACE_UI_STRING, "Adding component '" + components[i] + "' to the list of displayable components."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				this.displayableTracingComponents.put(components[i].getId(), components[i]);
			}
		}
		// now that the components are created - populate the debug options for each component
		final Iterator<TracingComponent> componentIterator = this.displayableTracingComponents.values().iterator();
		while (componentIterator.hasNext()) {
			componentIterator.next().initialize();
		}
		// update any TracingComponentDebugOption entries if their value differs from the preference value
		this.mergePrefsWithDebugOptions();
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	/**
	 * This method will merge the value of the options stored in the preferences with the value in the debug options.
	 */
	private void mergePrefsWithDebugOptions() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		final Map<String, String> prefDebugOptions = PreferenceHandler.getPreferenceProperties();
		// get all debug options (this ensures that the disabled debug options are used when populating)
		final Map<String, String> debugOptions = DebugOptionsHandler.getDebugOptions().getOptions();
		final Iterator<Map.Entry<String, String>> prefDebugOptionsIterator = prefDebugOptions.entrySet().iterator();
		while (prefDebugOptionsIterator.hasNext()) {
			final Map.Entry<String, String> prefDebugOption = prefDebugOptionsIterator.next();
			final String debugOptionsValue = debugOptions.get(prefDebugOption.getValue());
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
				if (TracingUIActivator.DEBUG_UI) {
					TRACE.trace(TracingConstants.TRACE_UI_STRING, "Changing the value of option '" //$NON-NLS-1$ 
							+ prefDebugOption.getKey() + "' from '" + debugOptionsValue + "' to '" //$NON-NLS-1$ //$NON-NLS-2$
							+ prefDebugOption.getValue() + "'."); //$NON-NLS-1$
				}
				TracingComponentDebugOption[] identicalOptions = TracingCollections.getInstance().getTracingDebugOptions(prefDebugOption.getKey());
				for (int identicalOptionsIndex = 0; identicalOptionsIndex < identicalOptions.length; identicalOptionsIndex++) {
					identicalOptions[identicalOptionsIndex].setOptionPathValue(prefDebugOption.getValue());
				}
			}
		}
	}

	@Override
	protected void performDefaults() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		super.performDefaults();
		// set the options to be an empty set
		DebugOptionsHandler.getDebugOptions().setOptions(new HashMap<String, String>());
		// destroy the model so that it can be re-built.
		this.purgeModel();
		// set the viewers input to null
		this.getViewer().setInput(null);
		// set the UI back to the default (tracing off, etc).
		PreferenceHandler.setDefaultPreferences();
		TracingCollections.getInstance().getModifiedDebugOptions().clear();
		this.setUIValuesFromPreferences();
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	@Override
	public boolean performOk() {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		boolean result = super.performOk();
		// get the enablement state
		boolean enableTracing = this.enableTracingButton.getSelection();

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
		this.savePreferences(enableTracing);
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Setting tracing enablement state: " + enableTracing); //$NON-NLS-1$
		}
		DebugOptionsHandler.setDebugEnabled(enableTracing);
		if (enableTracing) {
			final Map<String, String> newOptions = new HashMap<String, String>();
			// get the set of options available
			final Map<String, String> currentOptions = DebugOptionsHandler.getDebugOptions().getOptions();
			newOptions.putAll(currentOptions);
			// iterate over the list of added debug options and add them
			final TracingComponentDebugOption[] optionsToAdd = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToAdd();
			for (int i = 0; i < optionsToAdd.length; i++) {
				if (TracingUIActivator.DEBUG_UI) {
					TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Adding debug option: " + optionsToAdd[i]); //$NON-NLS-1$
				}
				newOptions.put(optionsToAdd[i].getOptionPath(), optionsToAdd[i].getOptionPathValue());
			}
			// iterate over the list of removed debug options and remove them
			final TracingComponentDebugOption[] optionsToRemove = TracingCollections.getInstance().getModifiedDebugOptions().getDebugOptionsToRemove();
			for (int i = 0; i < optionsToRemove.length; i++) {
				if (TracingUIActivator.DEBUG_UI) {
					TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Removing debug option: " + optionsToRemove[i]); //$NON-NLS-1$
				}
				newOptions.remove(optionsToRemove[i].getOptionPath());
			}
			// update the debug options
			DebugOptionsHandler.getDebugOptions().setOptions(newOptions);
			TracingCollections.getInstance().getModifiedDebugOptions().clear();
			// save the tracing file options
			final String defaultFile = DebugOptionsHandler.getDebugOptions().getFile().getAbsolutePath();
			if (TracingUIActivator.DEBUG_UI) {
				TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Current tracing file: " + defaultFile); //$NON-NLS-1$
			}
			final String newFile = this.tracingFileText.getText();
			if (!defaultFile.equals(newFile)) {
				if (TracingUIActivator.DEBUG_UI) {
					TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Setting the tracing file to: " + newFile); //$NON-NLS-1$
				}
				DebugOptionsHandler.getDebugOptions().setFile(new File(newFile));
			}
			// maximum file size
			final int newMaxSize = this.maximumFileSizeSpinner.getSelection();
			// property defined in org.eclipse.osgi.framework.debug.EclipseDebugTrace#PROP_TRACE_SIZE_MAX
			System.setProperty(TracingConstants.PROP_TRACE_SIZE_MAX, String.valueOf(newMaxSize));
			if (TracingUIActivator.DEBUG_UI) {
				TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Setting the maximum file size to: " + newMaxSize); //$NON-NLS-1$
			}
			// maximum file count
			final int newMaxCount = this.maximumFileCountSpinner.getSelection();
			// property defined in org.eclipse.osgi.framework.debug.EclipseDebugTrace#PROP_TRACE_FILE_MAX
			System.setProperty(TracingConstants.PROP_TRACE_FILE_MAX, String.valueOf(newMaxCount));
			if (TracingUIActivator.DEBUG_UI) {
				TRACE.traceEntry(TracingConstants.TRACE_UI_STRING, "Setting the maximum number of backup files to: " + newMaxCount); //$NON-NLS-1$
			}
		}
		// done
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING, Boolean.valueOf(result));
		}
		return result;
	}

	/**
	 * Save the property page preferences.
	 */
	protected void savePreferences(final boolean tracingEnabled) {

		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceEntry(TracingConstants.TRACE_UI_STRING);
		}
		final Map<String, String> prefValues = new HashMap<String, String>(5);
		prefValues.put(TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, Boolean.toString(tracingEnabled));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, Integer.toString(this.maximumFileCountSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, Integer.toString(this.maximumFileSizeSpinner.getSelection()));
		prefValues.put(TracingConstants.PREFERENCE_FILE_PATH, this.tracingFileText.getText());
		// iterate over the displayable components and store their debug options (all trace strings should be saved)
		final StringBuffer optionsAsString = new StringBuffer();
		if (this.displayableTracingComponents != null) {
			final Iterator<Map.Entry<String, TracingComponent>> componentIterator = this.displayableTracingComponents.entrySet().iterator();
			while (componentIterator.hasNext()) {
				TracingComponent component = componentIterator.next().getValue();
				if (component.hasChildren()) {
					StringBuffer result = this.getAllUniqueDebugOptions(component);
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
		if (TracingUIActivator.DEBUG_UI) {
			TRACE.traceExit(TracingConstants.TRACE_UI_STRING);
		}
	}

	private final StringBuffer getAllUniqueDebugOptions(final TracingNode node) {

		StringBuffer buffer = null;
		if (node.hasChildren()) {
			buffer = new StringBuffer();
			final TracingNode[] children = node.getChildren();
			for (int i = 0; i < children.length; i++) {
				// add this child node (all child nodes will be of type TracingComponentDebugOption)
				final String debugOptionAsString = TracingUtils.convertToString((TracingComponentDebugOption) children[i]);
				buffer.append(debugOptionAsString);
				// add all of this childs nodes
				StringBuffer result = this.getAllUniqueDebugOptions(children[i]);
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

		return this.filterTree.getViewer();
	}

	/**
	 * Accessor for the {@link Tree} of the {@link CheckboxTreeViewer}
	 * 
	 * @return The {@link Tree} of the {@link CheckboxTreeViewer}
	 */
	private Tree getViewerTree() {

		return this.filterTree.getViewer().getTree();
	}
}