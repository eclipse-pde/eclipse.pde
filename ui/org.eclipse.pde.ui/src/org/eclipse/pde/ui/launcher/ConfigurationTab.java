/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

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
import org.eclipse.pde.internal.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.internal.ui.launcher.ILauncherSettings;
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
	private Text fConfigAreaText;
	private Button fAreaWorkspace;
	
	private Button fGenerateFileButton;
	private Button fUseTemplateButton;
	private Text fTemplateLocationText;
	private Button fTemplateWorkspace;
	
	private String fLastEnteredConfigArea = ""; //$NON-NLS-1$
	private String fConfigName;
	private boolean fBlockChanges;
	private boolean fJUnitConfig;
	private Button fAreaFileSystem;
	private Button fAreaVariables;
	private Button fTemplateFilesystem;
	private Button fTemplateVariables;
	
	
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
		group.setLayout(new GridLayout(4, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fUseDefaultLocationButton = new Button(group, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 4;
		fUseDefaultLocationButton.setLayoutData(gd);
		fUseDefaultLocationButton.setText(PDEUIMessages.ConfigurationTab_useDefaultLoc); 
		fUseDefaultLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fUseDefaultLocationButton.getSelection();
				fConfigAreaText.setEnabled(!selected);
				fAreaWorkspace.setEnabled(!selected);
				if (!selected)
					fConfigAreaText.setText(fLastEnteredConfigArea);
				else
					fConfigAreaText.setText(PDECore.getDefault().getStateLocation().append(fConfigName).toOSString());
				updateStatus();
			}
		});
		
		fConfigAreaText = new Text(group, SWT.SINGLE|SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 20;
		gd.horizontalSpan = 4;
		fConfigAreaText.setLayoutData(gd);
		fConfigAreaText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					updateStatus();
			}
		});
		
		fClearConfig = new Button(group, SWT.CHECK);
		fClearConfig.setText(PDEUIMessages.ConfigurationTab_clearArea); 
		fClearConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fClearConfig.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		
		fAreaWorkspace = new Button(group, SWT.PUSH);
		fAreaWorkspace.setText("Workspace..."); 
		fAreaWorkspace.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fAreaWorkspace);
		fAreaWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseDirectory();
			}
		});

		fAreaFileSystem = new Button(group, SWT.PUSH);
		fAreaFileSystem.setText("File System..."); 
		fAreaFileSystem.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fAreaFileSystem);
		fAreaFileSystem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseDirectory();
			}
		});

		fAreaVariables = new Button(group, SWT.PUSH);
		fAreaVariables.setText("Variables..."); 
		fAreaVariables.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fAreaVariables);
		fAreaVariables.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseDirectory();
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
		group.setLayout(new GridLayout(4, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fGenerateFileButton = new Button(group, SWT.RADIO);
		fGenerateFileButton.setText(PDEUIMessages.ConfigurationTab_defaultConfigIni); 
		GridData gd = new GridData();
		gd.horizontalSpan = 4;
		fGenerateFileButton.setLayoutData(gd);
		fGenerateFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fGenerateFileButton.getSelection();
				fTemplateLocationText.setEnabled(!selected);
				fTemplateWorkspace.setEnabled(!selected);
				updateStatus();
			}
		});
		
		fUseTemplateButton = new Button(group, SWT.RADIO);
		fUseTemplateButton.setText(PDEUIMessages.ConfigurationTab_existingConfigIni); 
		gd = new GridData();
		gd.horizontalSpan = 4;
		fUseTemplateButton.setLayoutData(gd);
		
		fTemplateLocationText = new Text(group, SWT.SINGLE|SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 20;
		gd.widthHint = 300;
		gd.horizontalSpan = 4;
		fTemplateLocationText.setLayoutData(gd);
		fTemplateLocationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!fBlockChanges)
					updateStatus();
			}
		});
		
		Label label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fTemplateWorkspace = new Button(group, SWT.PUSH);
		fTemplateWorkspace.setText("Workspace..."); 
		fTemplateWorkspace.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fTemplateWorkspace);		
		fTemplateWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseWorkspaceFile();
			}
		});
		
		fTemplateFilesystem = new Button(group, SWT.PUSH);
		fTemplateFilesystem.setText("File System..."); 
		fTemplateFilesystem.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fTemplateFilesystem);		
		fTemplateFilesystem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseWorkspaceFile();
			}
		});
		
		fTemplateVariables = new Button(group, SWT.PUSH);
		fTemplateVariables.setText("Variables..."); 
		fTemplateVariables.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fTemplateVariables);		
		fTemplateVariables.addSelectionListener(new SelectionAdapter() {
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
			fConfigAreaText.setEnabled(!useDefaultArea);
			fAreaWorkspace.setEnabled(!useDefaultArea);
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
			fTemplateLocationText.setEnabled(!generateDefault);
			fTemplateWorkspace.setEnabled(!generateDefault);
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
