/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.model.*;
import org.eclipse.core.runtime.Path;

public class ConfigurationTab extends AbstractLauncherTab implements ILauncherSettings {
	private Button fClearConfig;
	private Image fImage;
	
	private Button fUseDefaultLocationButton;
	private Label fConfigAreaLabel;
	private Text fConfigAreaText;
	private Button fConfigAreaBrowse;
	
	private Button fGenerateFileButton;
	private Button fUseTemplateButton;
	private Label fTemplateLocationLabel;
	private Text fTemplateLocationText;
	private Button fTemplateLocationBrowse;
	
	private String fLastEnteredConfigArea = ""; //$NON-NLS-1$
	private String fConfigName;
	private boolean fBlockChanges;
	private boolean fJUnitConfig;
	
	
	public ConfigurationTab() {
		this(false);
	}
	
	public ConfigurationTab(boolean isJUnitConfig) {
		fImage = PDEPluginImages.DESC_PLUGIN_CONFIG_OBJ.createImage();
		fJUnitConfig = isJUnitConfig;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createConfigAreaGroup(container);
		createStartingSpace(container, 1);
		createConfigFileGroup(container);
		
		Dialog.applyDialogFont(container);
		setControl(container);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION);
	}
	
	private void createConfigAreaGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ConfigurationTab.configAreaGroup")); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fUseDefaultLocationButton = new Button(group, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fUseDefaultLocationButton.setLayoutData(gd);
		fUseDefaultLocationButton.setText(PDEPlugin.getResourceString("ConfigurationTab.useDefaultLoc")); //$NON-NLS-1$
		fUseDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fUseDefaultLocationButton.getSelection();
				fConfigAreaLabel.setEnabled(!selected);
				fConfigAreaText.setEnabled(!selected);
				fConfigAreaBrowse.setEnabled(!selected);
				if (!selected)
					fConfigAreaText.setText(fLastEnteredConfigArea);
				else
					fConfigAreaText.setText(PDECore.getDefault().getStateLocation().append(fConfigName).toOSString());
				updateStatus();
			}
		});
		
		fConfigAreaLabel = new Label(group, SWT.NONE);
		fConfigAreaLabel.setText(PDEPlugin.getResourceString("ConfigurationTab.configLog")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fConfigAreaLabel.setLayoutData(gd);
		
		fConfigAreaText = new Text(group, SWT.SINGLE|SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fConfigAreaText.setLayoutData(gd);
		fConfigAreaText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					updateStatus();
			}
		});
		
		fConfigAreaBrowse = new Button(group, SWT.PUSH);
		fConfigAreaBrowse.setText(PDEPlugin.getResourceString("ConfigurationTab.configBrowse")); //$NON-NLS-1$
		fConfigAreaBrowse.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fConfigAreaBrowse);
		fConfigAreaBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseDirectory();
			}
		});

		fClearConfig = new Button(group, SWT.CHECK);
		fClearConfig.setText(PDEPlugin.getResourceString("ConfigurationTab.clearArea")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fClearConfig.setLayoutData(gd);
		fClearConfig.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}
	
	protected void handleBrowseDirectory() {
		DirectoryDialog dialog = new DirectoryDialog(getControl().getShell());
		dialog.setFilterPath(fConfigAreaText.getText().trim());
		dialog.setText(PDEPlugin.getResourceString("ConfigurationTab.configLocTitle")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("ConfigurationTab.configLocMessage")); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null)
			fConfigAreaText.setText(res);
	}

	protected void updateStatus() {
		if (!fUseDefaultLocationButton.getSelection() && fConfigAreaText.getText().trim().length() == 0) {
			updateStatus(createStatus(IStatus.ERROR, PDEPlugin.getResourceString("ConfigurationTab.noConfigLoc"))); //$NON-NLS-1$
			return;
		}
		
		if (fUseTemplateButton.getSelection()) {
			String location = fTemplateLocationText.getText().trim();
			if (location.length() == 0) {
				updateStatus(createStatus(IStatus.ERROR, PDEPlugin.getResourceString("ConfigurationTab.noTemplateLoc"))); //$NON-NLS-1$
				return;
			}
			File file = new File(location);
			if (!file.exists() || !file.isFile()) {
				updateStatus(createStatus(IStatus.ERROR, PDEPlugin.getResourceString("ConfigurationTab.templateNotExists"))); //$NON-NLS-1$
				return;
			}
		}
		updateStatus(createStatus(IStatus.OK, "")); //$NON-NLS-1$
	}

	private void createConfigFileGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ConfigurationTab.configFileGroup")); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fGenerateFileButton = new Button(group, SWT.RADIO);
		fGenerateFileButton.setText(PDEPlugin.getResourceString("ConfigurationTab.defaultConfigIni")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fGenerateFileButton.setLayoutData(gd);
		fGenerateFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fGenerateFileButton.getSelection();
				fTemplateLocationLabel.setEnabled(!selected);
				fTemplateLocationText.setEnabled(!selected);
				fTemplateLocationBrowse.setEnabled(!selected);
				updateStatus();
			}
		});
		
		fUseTemplateButton = new Button(group, SWT.RADIO);
		fUseTemplateButton.setText(PDEPlugin.getResourceString("ConfigurationTab.existingConfigIni")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fUseTemplateButton.setLayoutData(gd);
		
		fTemplateLocationLabel = new Label(group, SWT.NONE);
		fTemplateLocationLabel.setText(PDEPlugin.getResourceString("ConfigurationTab.templateLoc")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fTemplateLocationLabel.setLayoutData(gd);
		
		fTemplateLocationText = new Text(group, SWT.SINGLE|SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		fTemplateLocationText.setLayoutData(gd);
		fTemplateLocationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					updateStatus();
			}
		});
		
		fTemplateLocationBrowse = new Button(group, SWT.PUSH);
		fTemplateLocationBrowse.setText(PDEPlugin.getResourceString("ConfigurationTab.templateBrowse")); //$NON-NLS-1$
		fTemplateLocationBrowse.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fTemplateLocationBrowse);		
		fTemplateLocationBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseWorkspaceFile();
			}
		});
	}
	
	protected void handleBrowseWorkspaceFile() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getControl().getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		
		IFile file = PDEPlugin.getWorkspace().getRoot().getFileForLocation(new Path(fTemplateLocationText.getText()));
		if (file != null)
			dialog.setInitialSelection(file);
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile)
					return ((IFile)element).getName().equals("config.ini"); //$NON-NLS-1$
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("ConfigurationTab.fileSelection")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("ConfigurationTab.fileDialogMessage")); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0
						&& selection[0] instanceof IFile)
					return new Status(IStatus.OK, PDEPlugin.getPluginId(),
							IStatus.OK, "", null); //$NON-NLS-1$
				
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			file = (IFile) dialog.getFirstResult();
			fTemplateLocationText.setText(file.getLocation().toOSString());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIG_USE_DEFAULT_AREA, true);
		configuration.setAttribute(CONFIG_LOCATION, ""); //$NON-NLS-1$
		configuration.setAttribute(CONFIG_CLEAR_AREA, fJUnitConfig);
		configuration.setAttribute(CONFIG_GENERATE_DEFAULT, true);
		configuration.setAttribute(CONFIG_TEMPLATE_LOCATION, ""); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fBlockChanges = true;
			boolean useDefaultArea = configuration.getAttribute(CONFIG_USE_DEFAULT_AREA, true);
			fUseDefaultLocationButton.setSelection(useDefaultArea);
			fConfigAreaLabel.setEnabled(!useDefaultArea);
			fConfigAreaText.setEnabled(!useDefaultArea);
			fConfigAreaBrowse.setEnabled(!useDefaultArea);
			fClearConfig.setSelection(configuration.getAttribute(CONFIG_CLEAR_AREA, false));
			fConfigName = configuration.getName();
			fLastEnteredConfigArea = configuration.getAttribute(CONFIG_LOCATION, ""); //$NON-NLS-1$
			if (useDefaultArea)
				fConfigAreaText.setText(PDECore.getDefault().getStateLocation().append(configuration.getName()).toOSString());
			else
				fConfigAreaText.setText(fLastEnteredConfigArea);
			
			boolean generateDefault = configuration.getAttribute(CONFIG_GENERATE_DEFAULT, true);
			fGenerateFileButton.setSelection(generateDefault);
			fUseTemplateButton.setSelection(!generateDefault);
			fTemplateLocationLabel.setEnabled(!generateDefault);
			fTemplateLocationText.setEnabled(!generateDefault);
			fTemplateLocationBrowse.setEnabled(!generateDefault);
			fTemplateLocationText.setText(configuration.getAttribute(CONFIG_TEMPLATE_LOCATION, "")); //$NON-NLS-1$
		} catch (CoreException e) {
		} finally {
			fBlockChanges = false;
		}
		updateStatus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIG_USE_DEFAULT_AREA, fUseDefaultLocationButton.getSelection());
		if (!fUseDefaultLocationButton.getSelection()) {
			fLastEnteredConfigArea = fConfigAreaText.getText().trim();
			configuration.setAttribute(CONFIG_LOCATION, fLastEnteredConfigArea);
		}
		configuration.setAttribute(CONFIG_CLEAR_AREA, fClearConfig.getSelection());
		configuration.setAttribute(CONFIG_GENERATE_DEFAULT, fGenerateFileButton.getSelection());
		if (!fGenerateFileButton.getSelection())
			configuration.setAttribute(CONFIG_TEMPLATE_LOCATION, fTemplateLocationText.getText().trim());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEPlugin.getResourceString("ConfigurationTab.name"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		if (fImage != null)
			fImage.dispose();
	}
}
