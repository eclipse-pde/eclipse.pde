/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.FileNameFilter;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConfigurationTemplateBlock extends BaseBlock {

	private Button fGenerateFileButton;
	private Button fUseTemplateButton;

	public ConfigurationTemplateBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ConfigurationTab_configFileGroup);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fGenerateFileButton = new Button(group, SWT.RADIO);
		fGenerateFileButton.setText(PDEUIMessages.ConfigurationTab_defaultConfigIni);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fGenerateFileButton.setLayoutData(gd);
		fGenerateFileButton.addSelectionListener(widgetSelectedAdapter(e -> {
			enableBrowseSection(!fGenerateFileButton.getSelection());
			boolean generateConfig = fGenerateFileButton.getSelection();
			fLocationText.setEditable(!generateConfig);
			if (generateConfig)
				fLocationText.setEnabled(true);
		}));

		fUseTemplateButton = new Button(group, SWT.RADIO);
		fUseTemplateButton.setText(PDEUIMessages.ConfigurationTab_existingConfigIni);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fUseTemplateButton.setLayoutData(gd);

		createText(group, PDEUIMessages.ConfigurationTab_templateLoc, 20);

		Composite buttons = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		buttons.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		buttons.setLayoutData(gd);

		Label label = new Label(buttons, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createButtons(buttons, new String[] {PDEUIMessages.BaseBlock_workspaceS, PDEUIMessages.BaseBlock_filesystemS, PDEUIMessages.BaseBlock_variablesS});
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		boolean generateDefault = configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		fGenerateFileButton.setSelection(generateDefault);
		fUseTemplateButton.setSelection(!generateDefault);
		enableBrowseSection(!generateDefault);
		fLocationText.setEditable(!generateDefault);
		if (generateDefault)
			fLocationText.setEnabled(true);
		fLocationText.setText(configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, "")); //$NON-NLS-1$
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, fGenerateFileButton.getSelection());
		if (!fGenerateFileButton.getSelection())
			configuration.setAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, fLocationText.getText().trim());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, "${target_home}" + File.separatorChar + "configuration" + File.separatorChar + "config.ini"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected String getName() {
		return PDEUIMessages.ConfigurationTemplateBlock_name;
	}

	@Override
	protected boolean isFile() {
		return true;
	}

	@Override
	protected void handleBrowseWorkspace() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(fTab.getControl().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		IFile file = getFile();
		if (file != null)
			dialog.setInitialSelection(file);
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.addFilter(new FileNameFilter("config.ini")); //$NON-NLS-1$
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ConfigurationTab_fileSelection);
		dialog.setMessage(PDEUIMessages.ConfigurationTab_fileDialogMessage);
		dialog.setValidator(selection -> {
			if (selection.length > 0 && selection[0] instanceof IFile) {
				return Status.OK_STATUS;
			}
			return Status.error(""); //$NON-NLS-1$
		});
		if (dialog.open() == Window.OK) {
			file = (IFile) dialog.getFirstResult();
			fLocationText.setText("${workspace_loc:" + file.getFullPath().makeRelative() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected IFile getFile() {
		String path = getLocation();
		if (path.length() > 0) {
			IResource res = null;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (path.startsWith("${workspace_loc:")) { //$NON-NLS-1$
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				try {
					path = manager.performStringSubstitution(path, false);
					IPath uriPath = new Path(path).makeAbsolute();
					IFile[] containers = root.findFilesForLocationURI(URIUtil.toURI(uriPath));
					if (containers.length > 0)
						res = containers[0];
				} catch (CoreException e) {
				}
			} else {
				res = root.findMember(path);
			}
			if (res instanceof IFile) {
				return (IFile) res;
			}
		}
		return null;
	}

	@Override
	protected void handleBrowseFileSystem() {
		FileDialog dialog = new FileDialog(fTab.getControl().getShell());
		dialog.setFilterExtensions(new String[] {"*.ini"}); //$NON-NLS-1$
		dialog.setFilterPath(getLocation());
		dialog.setText(PDEUIMessages.ConfigurationTab_configLocMessage);
		String res = dialog.open();
		if (res != null)
			fLocationText.setText(res);
	}

	@Override
	protected String getLocation() {
		String path = fLocationText.getText().trim();
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		try {
			return manager.performStringSubstitution(path, false);
		} catch (CoreException e) {
			return path;
		}
	}

	@Override
	public String validate() {
		if (fGenerateFileButton.getSelection())
			return null;
		return super.validate();
	}

}
