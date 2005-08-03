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
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION);
	}
	
	private void createConfigAreaGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ConfigurationTab_configAreaGroup); 
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fUseDefaultLocationButton = new Button(group, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fUseDefaultLocationButton.setLayoutData(gd);
		fUseDefaultLocationButton.setText(PDEUIMessages.ConfigurationTab_useDefaultLoc); 
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
		fConfigAreaLabel.setText(PDEUIMessages.ConfigurationTab_configLog); 
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
		fConfigAreaBrowse.setText(PDEUIMessages.ConfigurationTab_configBrowse); 
		fConfigAreaBrowse.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fConfigAreaBrowse);
		fConfigAreaBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseDirectory();
			}
		});

		fClearConfig = new Button(group, SWT.CHECK);
		fClearConfig.setText(PDEUIMessages.ConfigurationTab_clearArea); 
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
		dialog.setText(PDEUIMessages.ConfigurationTab_configLocTitle); 
		dialog.setMessage(PDEUIMessages.ConfigurationTab_configLocMessage); 
		String res = dialog.open();
		if (res != null)
			fConfigAreaText.setText(res);
	}

	protected void updateStatus() {
		if (!fUseDefaultLocationButton.getSelection() && fConfigAreaText.getText().trim().length() == 0) {
			updateStatus(createStatus(IStatus.ERROR, PDEUIMessages.ConfigurationTab_noConfigLoc)); 
			return;
		}
		
		if (fUseTemplateButton.getSelection()) {
			String location = fTemplateLocationText.getText().trim();
			if (location.length() == 0) {
				updateStatus(createStatus(IStatus.ERROR, PDEUIMessages.ConfigurationTab_noTemplateLoc)); 
				return;
			}
			File file = new File(location);
			if (!file.exists() || !file.isFile()) {
				updateStatus(createStatus(IStatus.ERROR, PDEUIMessages.ConfigurationTab_templateNotExists)); 
				return;
			}
		}
		updateStatus(createStatus(IStatus.OK, "")); //$NON-NLS-1$
	}

	private void createConfigFileGroup(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ConfigurationTab_configFileGroup); 
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fGenerateFileButton = new Button(group, SWT.RADIO);
		fGenerateFileButton.setText(PDEUIMessages.ConfigurationTab_defaultConfigIni); 
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
		fUseTemplateButton.setText(PDEUIMessages.ConfigurationTab_existingConfigIni); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		fUseTemplateButton.setLayoutData(gd);
		
		fTemplateLocationLabel = new Label(group, SWT.NONE);
		fTemplateLocationLabel.setText(PDEUIMessages.ConfigurationTab_templateLoc); 
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
		fTemplateLocationBrowse.setText(PDEUIMessages.ConfigurationTab_templateBrowse); 
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
		dialog.setTitle(PDEUIMessages.ConfigurationTab_fileSelection); 
		dialog.setMessage(PDEUIMessages.ConfigurationTab_fileDialogMessage); 
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
		return PDEUIMessages.ConfigurationTab_name; 
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
