/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.io.File;
import java.util.HashSet;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.pde.internal.core.SourceLocation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SourceLocationDialog extends Dialog {
	private Text nameText;
	private Text pathText;
	private Button browseButton;
	private String name;
	private IPath path;
	private Label statusLabel;
	private SourceLocation location;
	private static String previousPath = "";
	private HashSet existingNames = new HashSet();
	/**
	 * Constructor for SourceLocationDialog.
	 * @param parentShell
	 */
	public SourceLocationDialog(Shell parentShell, SourceLocation location) {
		super(parentShell);
		this.location = location;
	}

	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("SourceLocationDialog.locationName")); //$NON-NLS-1$

		nameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);

		new Label(container, SWT.NULL);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("SourceLocationDialog.locationPath")); //$NON-NLS-1$

		pathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		pathText.setText(previousPath);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		pathText.setLayoutData(gd);

		browseButton = new Button(container, SWT.PUSH);
		browseButton.setText(PDEPlugin.getResourceString("SourceLocationDialog.browse")); //$NON-NLS-1$
		browseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		statusLabel = new Label(container, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		statusLabel.setLayoutData(gd);
		
		if (location!=null) {
			nameText.setText(location.getName());
			pathText.setText(location.getPath().toOSString());
		}
		ModifyListener listener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		nameText.addModifyListener(listener);
		pathText.addModifyListener(listener);
		
		Dialog.applyDialogFont(container);
		return container;
	}

	private void handleBrowse() {
		DirectoryDialog dd = new DirectoryDialog(getShell());
		dd.setFilterPath(pathText.getText());
		String path = dd.open();
		if (path != null) {
			pathText.setText(path);
		}
	}
	
	private void dialogChanged() {
		String error = null;
		String name = nameText.getText();
		String path = pathText.getText();
		if (name.length()==0) {
			error = PDEPlugin.getResourceString("SourceLocationDialog.nameNotDefined"); //$NON-NLS-1$
		}
		else if (isInvalidVariable(name)) {
			error = PDEPlugin.getResourceString("SourceLocationDialog.exists"); //$NON-NLS-1$
		}
		else if (path.length()==0) {
			error = PDEPlugin.getResourceString("SourceLocationDialog.pathNotDefined"); //$NON-NLS-1$
		}
		else {
			File file = new File(path);
			if (!file.exists() || !file.isDirectory()) {
				error = PDEPlugin.getResourceString("SourceLocationDialog.locationNotFound"); //$NON-NLS-1$
			}
		}
		setError(error);
	}
	
	private void setError(String error) {
		if (error!=null) {
			statusLabel.setText(error);
			statusLabel.setForeground(JFaceColors.getErrorText(getShell().getDisplay()));
		}
		else {
			statusLabel.setText(""); //$NON-NLS-1$
			statusLabel.setForeground(null);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(error==null);
	}
	
	private boolean isInvalidVariable(String name) {
		return existingNames.contains(name);
	}

	protected void okPressed() {
		name = nameText.getText();
		previousPath = pathText.getText();
		path = new Path(pathText.getText());
		if (location!=null) {
			location.setName(name);
			location.setPath(path);
		}
		super.okPressed();
	}
	
	public String getName() {
		return name;
	}
	public IPath getPath() {
		return path;
	}
	
	public void create() {
		super.create();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	public void setInvalidNames(HashSet names) {
		this.existingNames = names;
	}
		
}
