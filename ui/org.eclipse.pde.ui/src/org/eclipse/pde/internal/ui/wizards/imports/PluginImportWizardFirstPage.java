/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on May 30, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.PluginPathFinder;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.preferences.SourceCodeLocationsPreferenceNode;
import org.eclipse.pde.internal.ui.preferences.TargetEnvironmentPreferenceNode;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

/**
 * @author Wassim Melhem
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
	
	//private String currentLocation;
	public static String TARGET_PLATFORM = "targetPlatform"; //$NON-NLS-1$
	private IPluginModelBase[] models = new IPluginModelBase[0];
	
	public PluginImportWizardFirstPage(String name) {
		super(name);
		setTitle(PDEUIMessages.ImportWizard_FirstPage_title); //$NON-NLS-1$
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
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
	
	private void createImportChoicesGroup(Composite container) {
		Group importChoices = new Group(container, SWT.NONE);
		importChoices.setText(PDEUIMessages.ImportWizard_FirstPage_importGroup); //$NON-NLS-1$
		importChoices.setLayout(new GridLayout());
		importChoices.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		scanButton = new Button(importChoices, SWT.RADIO);
		scanButton.setText(PDEUIMessages.ImportWizard_FirstPage_scanAll);		 //$NON-NLS-1$
		
		importButton = new Button(importChoices, SWT.RADIO);
		importButton.setText(PDEUIMessages.ImportWizard_FirstPage_importPrereqs); //$NON-NLS-1$
		
	}
	
	private void createImportOptionsGroup(Composite container) {
		Group options = new Group(container, SWT.NONE);
		options.setText(PDEUIMessages.ImportWizard_FirstPage_importAs); //$NON-NLS-1$
		options.setLayout(new GridLayout());
		options.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		binaryButton = new Button(options, SWT.RADIO);
		binaryButton.setText(PDEUIMessages.ImportWizard_FirstPage_binary); //$NON-NLS-1$
		
		binaryWithLinksButton = new Button(options, SWT.RADIO);
		binaryWithLinksButton.setText(PDEUIMessages.ImportWizard_FirstPage_binaryLinks); //$NON-NLS-1$
		
		sourceButton = new Button(options, SWT.RADIO);
		sourceButton.setText(PDEUIMessages.ImportWizard_FirstPage_source); //$NON-NLS-1$
	}
	
	
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
	
	private void createDirectoryGroup(Composite parent) {
		Group composite = new Group(parent, SWT.NONE);
		composite.setText(PDEUIMessages.ImportWizard_FirstPage_importFrom); //$NON-NLS-1$

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		runtimeLocationButton = new Button(composite, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		runtimeLocationButton.setLayoutData(gd);
		
		runtimeLocationButton.setText(PDEUIMessages.ImportWizard_FirstPage_target); //$NON-NLS-1$
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

		changeButton = new Button(composite, SWT.PUSH);
		changeButton.setText(PDEUIMessages.ImportWizard_FirstPage_goToTarget); //$NON-NLS-1$
		changeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleChangeTargetPlatform();
			}
		});
		changeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(changeButton);

		otherLocationLabel = new Label(composite, SWT.NULL);
		otherLocationLabel.setText(PDEUIMessages.ImportWizard_FirstPage_otherFolder); //$NON-NLS-1$

		dropLocation = new Combo(composite, SWT.DROP_DOWN);
		dropLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
			}
		});

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(PDEUIMessages.ImportWizard_FirstPage_browse); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					dropLocation.setText(chosen.toOSString());
			}
		});
		browseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(browseButton);

		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.ImportWizard_FirstPage_source_label); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		Button sourceLocations = new Button(composite, SWT.PUSH);
		sourceLocations.setText(PDEUIMessages.ImportWizard_FirstPage_codeLocations); //$NON-NLS-1$
		sourceLocations.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		sourceLocations.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSourceLocations();
			}
		});
		SWTUtil.setButtonDimensionHint(sourceLocations);
		sourceLocations.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		label = new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.ImportWizard_FirstPage_variables); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		Button envButton = new Button(composite, SWT.PUSH);
		envButton.setText(PDEUIMessages.ImportWizard_FirstPage_env); //$NON-NLS-1$
		envButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END| GridData.FILL_HORIZONTAL));
		envButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEnvChange();
			}
		});
		SWTUtil.setButtonDimensionHint(envButton);
		envButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
	}
	
	
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(dropLocation.getText());
		dialog.setText(PDEUIMessages.ImportWizard_messages_folder_title); //$NON-NLS-1$
		dialog.setMessage(PDEUIMessages.ImportWizard_messages_folder_message); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}
	
	private void handleChangeTargetPlatform() {
		IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		if (showPreferencePage(targetNode))
			dropLocation.setText(ExternalModelManager.getEclipseHome().toOSString());
	}
	
	private void handleSourceLocations() {
		showPreferencePage(new SourceCodeLocationsPreferenceNode());
	}
	
	private void handleEnvChange() {
		showPreferencePage(new TargetEnvironmentPreferenceNode());
	}

	private boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog =
			new PreferenceDialog(getControl().getShell(), manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == PreferenceDialog.OK)
					result[0] = true;
			}
		});
		return result[0];
	}
	
	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}
	
	public boolean getScanAllPlugins() {
		return scanButton.getSelection();
	}
	
	public int getImportType() {
		if (binaryButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY;
		}
		
		if (binaryWithLinksButton.getSelection()) {
			return PluginImportOperation.IMPORT_BINARY_WITH_LINKS;
		}
		
		return PluginImportOperation.IMPORT_WITH_SOURCE;
	}
	
	public String getDropLocation() {
		return runtimeLocationButton.getSelection()
			? TARGET_PLATFORM
			: dropLocation.getText().trim();
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean other = !runtimeLocationButton.getSelection();
		if (dropLocation.getText().length() > 0 && other) {
			settings.put(
				SETTINGS_DROPLOCATION + String.valueOf(0),
				dropLocation.getText().trim());
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
	
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	private void validateDropLocation() {
		if (!runtimeLocationButton.getSelection()) {
			IPath curr = new Path(dropLocation.getText());
			if (curr.segmentCount() == 0 && curr.getDevice() == null) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_locationMissing); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}
			if (!Path.ROOT.isValidPath(dropLocation.getText())) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderInvalid); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}

			if (!curr.toFile().isDirectory()) {
				setErrorMessage(PDEUIMessages.ImportWizard_errors_buildFolderMissing); //$NON-NLS-1$
				setPageComplete(false);
				return;
			}
			if (!curr.equals(new Path(getTargetHome()))) {
				setErrorMessage(null);
				setMessage(PDEUIMessages.ImportWizard_FirstPage_warning, DialogPage.WARNING); //$NON-NLS-1$
				setPageComplete(true);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
		setMessage(PDEUIMessages.ImportWizard_FirstPage_desc); //$NON-NLS-1$
	}
	
	private void resolveTargetPlatform() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				models = PDECore.getDefault().getModelManager().getExternalModels();
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
				models = state.getModels();
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
		

	public IPluginModelBase[] getModels() {
		String dropLocation = getDropLocation();
		if (dropLocation.equals(TARGET_PLATFORM)) {
			resolveTargetPlatform();
		} else {
			resolveArbitraryLocation(dropLocation);
		}
		return models;
	}
}
