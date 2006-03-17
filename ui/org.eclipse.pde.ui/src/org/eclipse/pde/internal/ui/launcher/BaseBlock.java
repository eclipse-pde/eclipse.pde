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
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public abstract class BaseBlock {
	
	protected AbstractLauncherTab fTab;

	private Button fVariablesButton;
	private Button fFileSystemButton;
	private Button fWorkspaceButton;
	
	protected Text fLocationText;
	
	protected Listener fListener = new Listener();

	protected Label fLocationLabel;

	class Listener extends SelectionAdapter implements ModifyListener {		
		public void widgetSelected(SelectionEvent e) {
			Object source= e.getSource();
			if (source == fFileSystemButton) { 
				handleBrowseFileSystem();
			} else if (source == fWorkspaceButton) {
				handleBrowseWorkspace();
			} else if (source == fVariablesButton) {
				handleInsertVariable();
			} else {			
				fTab.updateLaunchConfigurationDialog();
			}
		}

		public void modifyText(ModifyEvent e) {
			fTab.updateLaunchConfigurationDialog();
		}
	}
	
	public BaseBlock(AbstractLauncherTab tab) {
		fTab = tab;
	}
	
	protected void createText(Composite parent, String text, int indent) {
		fLocationLabel = new Label(parent, SWT.NONE);
		fLocationLabel.setText(text);
		if (indent > 0) {
			GridData gd = new GridData();
			gd.horizontalIndent = indent;
			fLocationLabel.setLayoutData(gd);
		}

		fLocationText = new Text(parent, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		fLocationText.setLayoutData(gd);
		fLocationText.addModifyListener(fListener);
	}
	
	protected void createButtons(Composite parent) {
		fWorkspaceButton = createButton(parent, PDEUIMessages.BaseBlock_workspace); 
		fFileSystemButton = createButton(parent, PDEUIMessages.BaseBlock_filesystem); 
		fVariablesButton = createButton(parent, PDEUIMessages.BaseBlock_variables); 	
	}
	
	protected Button createButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData());
		button.addSelectionListener(fListener);
		SWTUtil.setButtonDimensionHint(button);
		return button;
	}
		
	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(fTab.getControl().getShell());
		dialog.setFilterPath(getLocation());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null)
			fLocationText.setText(result);
	}
	
	protected void handleBrowseWorkspace() {
		ContainerSelectionDialog dialog = 
			new ContainerSelectionDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					getContainer(), 
					true,
					PDEUIMessages.BaseBlock_relative); 
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 0)
				return;
			IPath path = (IPath)result[0];
			fLocationText.setText("${workspace_loc:" + path.makeRelative().toString() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected IContainer getContainer() {
		String path = getLocation();
		if (path.length() > 0) {
		    IResource res = null;
		    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		    if (path.startsWith("${workspace_loc:")) { //$NON-NLS-1$
		        IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			    try {
                    path = manager.performStringSubstitution(path, false);
                    IContainer[] containers = root.findContainersForLocation(new Path(path));
                    if (containers.length > 0)
                        res = containers[0];
                } catch (CoreException e) {
                }
			} else {	    
				res = root.findMember(path);
			}
			if (res instanceof IContainer) {
				return (IContainer)res;
			}
		}
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
					new StringVariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell());
		if (dialog.open() == Window.OK)
			fLocationText.insert(dialog.getVariableExpression());
	}
	
	protected String getLocation() {
		return fLocationText.getText().trim();
	}
	
	public String validate() {
		return (fLocationText.isEnabled() && getLocation().length() == 0)
					? NLS.bind(PDEUIMessages.BaseBlock_errorMessage, getName())
					: null;
	}
	
	protected abstract String getName();
	
	protected void enableBrowseSection(boolean enabled) {
		fLocationLabel.setEnabled(enabled);
		fLocationText.setEnabled(enabled);
		fFileSystemButton.setEnabled(enabled);
		fWorkspaceButton.setEnabled(enabled);
		fVariablesButton.setEnabled(enabled);
		fTab.updateLaunchConfigurationDialog();
	}
	
}
