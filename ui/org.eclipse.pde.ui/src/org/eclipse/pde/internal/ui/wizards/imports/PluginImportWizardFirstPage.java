/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * The first page of the import plugins wizard
 */
public class PluginImportWizardFirstPage extends WizardPage {

	private static String SETTINGS_IMPORTTYPE = "importType"; //$NON-NLS-1$
	private static String SETTINGS_DOOTHER = "doother"; //$NON-NLS-1$
	private static String SETTINGS_DROPLOCATION = "droplocation"; //$NON-NLS-1$
	private static String SETTINGS_SCAN_ALL = "scanAll"; //$NON-NLS-1$

	private Button runtimeLocationButton;
	private Button browseButton;
	private Label otherLocationLabel;
	private Combo dropLocation;
	private Button changeButton;

	private Button importButton;
	private Button scanButton;

	private Button binaryButton;
	private Button binaryWithLinksButton;
	private Button sourceButton;

	public static String TARGET_PLATFORM = "targetPlatform"; //$NON-NLS-1$
	private IPluginModelBase[] models = new IPluginModelBase[0];
	private boolean canceled = false;

	public PluginImportWizardFirstPage(String name) {
		super(name);
		setTitle(PDEUIMessages.ImportWizard_FirstPage_title);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);

		createDirectoryGroup(container);
		createImportChoicesGroup(container);
		createImportOptionsGroup(container);

		Dialog.applyDialogFont(container);
		initialize();
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_FIRST_PAGE);
	}

	/**
	 * Create the import choices group
	 * @param container
	 */
	private void createImportChoicesGroup(Composite container) {
		Group importChoices = SWTFactory.createGroup(container, PDEUIMessages.ImportWizard_FirstPage_importGroup, 1, 1, GridData.FILL_HORIZONTAL);
		scanButton = SWTFactory.createRadioButton(importChoices, PDEUIMessages.ImportWizard_FirstPage_scanAll);
		importButton = SWTFactory.createRadioButton(importChoices, PDEUIMessages.ImportWizard_FirstPage_importPrereqs);
	}

	/**
	 * Create the import options group
	 * @param container
	 */
	private void createImportOptionsGroup(Composite container) {
		Group options = SWTFactory.createGroup(container, PDEUIMessages.ImportWizard_FirstPage_importAs, 1, 1, GridData.FILL_HORIZONTAL);
		binaryButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_binary);
		binaryWithLinksButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_binaryLinks);
		sourceButton = SWTFactory.createRadioButton(options, PDEUIMessages.ImportWizard_FirstPage_source);
	}

	/**
	 * Initialize the page with previous choices from dialog settings
	 */
	private void initialize() {
		IDialogSettings settings = getDialogSettings();

		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(SETTINGS_DROPLOCATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		dropLocation.setItems((String[]) items.toArray(new String[items.size()]));

		if (settings.getBoolean(SETTINGS_DOOTHER)) {
			runtimeLocationButton.setSelection(false);
			changeButton.setEnabled(false);
			dropLocation.setText(items.get(0).toString());
		} else {
			runtimeLocationButton.setSelection(true);
			otherLocationLabel.setEnabled(false);
			dropLocation.setEnabled(false);
			browseButton.setEnabled(false);
			dropLocation.setText(getTargetHome());
		}

		int importType = PluginImportOperation.IMPORT_BINARY;
		try {
			importType = settings.getInt(SETTINGS_IMPORTTYPE);
		} catch (NumberFormatException e) {
		}
		if (importType == PluginImportOperation.IMPORT_BINARY) {
			binaryButton.setSelection(true);
		} else if (importType == PluginImportOperation.IMPORT_BINARY_WITH_LINKS) {
			binaryWithLinksButton.setSelection(true);
		} else {
			sourceButton.setSelection(true);
		}

		boolean scan = true;
		if (settings.get(SETTINGS_SCAN_ALL) != null) {
			scan = settings.getBoolean(SETTINGS_SCAN_ALL);
		}
		scanButton.setSelection(scan);
		importButton.setSelection(!scan);

	}

	/**
	 * Creates the directory group
	 * @param parent
	 */
	private void createDirectoryGroup(Composite parent) {
		Group composite = SWTFactory.createGroup(parent, PDEUIMessages.ImportWizard_FirstPage_importFrom, 3, 1, GridData.FILL_HORIZONTAL);

		runtimeLocationButton = SWTFactory.createCheckButton(composite, PDEUIMessages.ImportWizard_FirstPage_target, null, false, 2);
		runtimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = runtimeLocationButton.getSelection();
				if (selected) {
					dropLocation.setText(getTargetHome());
				}
				otherLocationLabel.setEnabled(!selected);
				dropLocation.setEnabled(!selected);
				browseButton.setEnabled(!selected);
				changeButton.setEnabled(selected);
				validateDropLocation();
			}
		});

		changeButton = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_goToTarget, null);
		changeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
				if (PDEPreferencesUtil.showPreferencePage(targetNode, getShell())) {
					dropLocation.setText(TargetPlatform.getLocation());
				}
			}
		});

		otherLocationLabel = SWTFactory.createLabel(composite, PDEUIMessages.ImportWizard_FirstPage_otherFolder, 1);

		dropLocation = SWTFactory.createCombo(composite, SWT.DROP_DOWN, 1, GridData.FILL_HORIZONTAL, null);
		dropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
			}
		});

		browseButton = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_browse, null, GridData.HORIZONTAL_ALIGN_FILL);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});

		SWTFactory.createLabel(composite, PDEUIMessages.ImportWizard_FirstPage_source_label, 2);

		Button button = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_codeLocations, null, GridData.HORIZONTAL_ALIGN_END);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PDEPreferencesUtil.showPreferencePage(new SourceCodeLocationsPreferenceNode(), getShell());
			}
		});

		SWTFactory.createLabel(composite, PDEUIMessages.ImportWizard_FirstPage_variables, 2);

		button = SWTFactory.createPushButton(composite, PDEUIMessages.ImportWizard_FirstPage_env, null, GridData.HORIZONTAL_ALIGN_END);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PDEPreferencesUtil.showPreferencePage(new TargetEnvironmentPreferenceNode(), getShell());
			}
		});
	}

	/**
	 * @return a chosen path from the directory dialog invoked from browse button
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(dropLocation.getText());
		dialog.setText(PDEUIMessages.ImportWizard_messages_folder_title);
		dialog.setMessage(PDEUIMessages.ImportWizard_messages_folder_message);
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	/**
	 * @return the value of the {@link ICoreConstants#PLATFORM_PATH} preference
	 */
	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}

	/**
	 * @return the selection state of the scan all plugins button
	 */
	public boolean getScanAllPlugins() {
		return scanButton.getSelection();
	}

	/**
	 * Returns the type of the import. One of:
	 * <ul>
	 * <li>{@link PluginImportOperation#IMPORT_BINARY}</li>
	 * <li>{@link PluginImportOperation#IMPORT_BINARY_WITH_LINKS}</li>
	 * <li>{@link PluginImportOperation#IMPORT_WITH_SOURCE}</li>
	 * </ul>
	 * @return the type of the import.
	 */
	public int getImportType() {
		if (binaryButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY;
		}

		if (binaryWithLinksButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY_WITH_LINKS;
		}

		return PluginImportOperation.IMPORT_WITH_SOURCE;
	}

	/**
	 * @return the location specified as the drop location for the target platform
	 */
	public String getDropLocation() {
		return runtimeLocationButton.getSelection() ? TARGET_PLATFORM : dropLocation.getText().trim();
	}

	/**
	 * Store all of the dialog settings for this page
	 */
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean other = !runtimeLocationButton.getSelection();
		if (dropLocation.getText().length() > 0 && other) {
			settings.put(SETTINGS_DROPLOCATION + String.valueOf(0), dropLocation.getText().trim());
			String[] items = dropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(SETTINGS_DROPLOCATION + String.valueOf(i + 1), items[i]);
			}
		}
		settings.put(SETTINGS_DOOTHER, other);
		settings.put(SETTINGS_IMPORTTYPE, getImportType());
		settings.put(SETTINGS_SCAN_ALL, getScanAllPlugins());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/**
	 * Validates the drop location
	 */
	private void validateDropLocation() {
		if (!runtimeLocationButton.getSelection()) {
			IPath curr = new Path(dropLocation.getText());
			if (curr.segmentCount() == 0 && curr.getDevice() == null) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_locationMissing);
				setPageComplete(false);
				return;
			}
			if (!Path.ROOT.isValidPath(dropLocation.getText())) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderInvalid);
				setPageComplete(false);
				return;
			}

			if (!curr.toFile().isDirectory()) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderMissing);
				setPageComplete(false);
				return;
			}
			if (!curr.equals(new Path(getTargetHome()))) {
				setErrorMessage(null);
				setMessage(PDEUIMessages.ImportWizard_FirstPage_warning, IMessageProvider.WARNING);
				setPageComplete(true);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc);
	}

	/**
	 * Resolves the target platform
	 */
	private void resolveTargetPlatform() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				models = PluginRegistry.getExternalModels();
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Resolves the plugin locations given the base location. Uses {@link PluginPathFinder#scanLocations(File[])}
	 * to find plugins
	 * @param location
	 */
	private void resolveArbitraryLocation(final String location) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				File[] files = new File[2];
				files[0] = new File(location);
				files[1] = new File(location, "plugins"); //$NON-NLS-1$
				URL[] urls = PluginPathFinder.scanLocations(files);
				URL[] all = new URL[urls.length + 1];
				try {
					all[0] = new URL("file:" + files[0].getAbsolutePath()); //$NON-NLS-1$
					System.arraycopy(urls, 0, all, 1, urls.length);
				} catch (MalformedURLException e) {
					all = urls;
				}
				PDEState state = new PDEState(all, false, monitor);
				models = state.getTargetModels();
				canceled = monitor.isCanceled();
				monitor.done();
			}
		};
		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * @return the complete set of {@link IPluginModelBase}s for the given drop location
	 */
	public IPluginModelBase[] getModels() {
		String dropLocation = getDropLocation();
		if (dropLocation.equals(TARGET_PLATFORM)) {
			resolveTargetPlatform();
		} else {
			resolveArbitraryLocation(dropLocation);
		}
		return models;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isCurrentPage()
	 */
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}

	/**
	 * @return true if the page needs to be refreshed, false otherwise
	 */
	public boolean isRefreshNeeded() {
		if (canceled) {
			canceled = false;
			return true;
		}
		return false;
	}
}
