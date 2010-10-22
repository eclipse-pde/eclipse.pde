/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 217333
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public abstract class BaseBlock {

	protected AbstractLauncherTab fTab;

	private Button fVariablesButton;
	private Button fFileSystemButton;
	private Button fWorkspaceButton;

	protected Text fLocationText;

	protected Listener fListener = new Listener();

	protected Link fLocationLink;

	class Listener extends SelectionAdapter implements ModifyListener {
		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fFileSystemButton) {
				handleBrowseFileSystem();
			} else if (source == fWorkspaceButton) {
				handleBrowseWorkspace();
			} else if (source == fVariablesButton) {
				handleInsertVariable();
			} else if (fTab != null) {
				fTab.updateLaunchConfigurationDialog();
			}
		}

		public void modifyText(ModifyEvent e) {
			if (fTab != null)
				fTab.scheduleUpdateJob();
		}
	}

	public BaseBlock(AbstractLauncherTab tab) {
		fTab = tab;
	}

	protected void createText(Composite parent, String text, int indent) {
		fLocationLink = new Link(parent, SWT.NONE);
		fLocationLink.setText("<a>" + text + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (indent > 0) {
			GridData gd = new GridData();
			gd.horizontalIndent = indent;
			fLocationLink.setLayoutData(gd);
		}

		fLocationText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		fLocationText.setLayoutData(gd);
		fLocationText.addModifyListener(fListener);

		fLocationLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					String path = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(getLocation(), false);
					File f = new File(path);
					if (f.exists())
						Program.launch(f.getCanonicalPath());
					else
						MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), isFile() ? PDEUIMessages.BaseBlock_fileTitle : PDEUIMessages.BaseBlock_directoryTitle, isFile() ? PDEUIMessages.BaseBlock_fileNotFoundMessage : PDEUIMessages.BaseBlock_directoryNotFoundMessage);
				} catch (Exception ex) {
					MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), isFile() ? PDEUIMessages.BaseBlock_fileTitle : PDEUIMessages.BaseBlock_directoryTitle, isFile() ? PDEUIMessages.BaseBlock_fileErrorMessage : PDEUIMessages.BaseBlock_directoryErrorMessage);
				}
			}
		});

	}

	protected void createButtons(Composite parent, String[] buttonLabels) {
		fWorkspaceButton = createButton(parent, buttonLabels[0]);
		fFileSystemButton = createButton(parent, buttonLabels[1]);
		fVariablesButton = createButton(parent, buttonLabels[2]);
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
		DirectoryDialog dialog = new DirectoryDialog(fLocationText.getShell());
		dialog.setFilterPath(getLocation());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection);
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose);
		String result = dialog.open();
		if (result != null)
			fLocationText.setText(result);
	}

	protected void handleBrowseWorkspace() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(fLocationText.getShell(), getContainer(), true, PDEUIMessages.BaseBlock_relative);
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 0)
				return;
			IPath path = (IPath) result[0];
			fLocationText.setText("${workspace_loc:" + path.makeRelative().toString() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Returns the selected workspace container,or <code>null</code>
	 */
	protected IContainer getContainer() {
		String path = getLocation();
		if (path.length() > 0) {
			IResource res = null;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (path.startsWith("${workspace_loc:")) { //$NON-NLS-1$
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				try {
					path = manager.performStringSubstitution(path, false);
					IPath uriPath = new Path(path).makeAbsolute();
					IContainer[] containers = root.findContainersForLocationURI(URIUtil.toURI(uriPath));
					if (containers.length > 0) {
						res = containers[0];
					}
				} catch (CoreException e) {
				}
			} else {
				res = root.findMember(path);
			}
			if (res instanceof IContainer) {
				return (IContainer) res;
			}
		}
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(fLocationText.getShell());
		if (dialog.open() == Window.OK)
			fLocationText.insert(dialog.getVariableExpression());
	}

	protected String getLocation() {
		return fLocationText.getText().trim();
	}

	public String validate() {
		return (fLocationText.isEnabled() && getLocation().length() == 0) ? NLS.bind(PDEUIMessages.BaseBlock_errorMessage, getName()) : null;
	}

	protected abstract String getName();

	/**
	 * @return true if the block edits a file, false otherwise (i.e. directory)
	 */
	protected abstract boolean isFile();

	protected void enableBrowseSection(boolean enabled) {
		fLocationLink.setEnabled(enabled);
		fLocationText.setEnabled(enabled);
		fFileSystemButton.setEnabled(enabled);
		fWorkspaceButton.setEnabled(enabled);
		fVariablesButton.setEnabled(enabled);
		if (fTab != null)
			fTab.updateLaunchConfigurationDialog();
	}

}
