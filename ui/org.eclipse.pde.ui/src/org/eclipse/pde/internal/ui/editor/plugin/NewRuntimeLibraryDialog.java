/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

public class NewRuntimeLibraryDialog extends SelectionStatusDialog {
	private Text libraryText;
	private IPluginLibrary[] libraries;
	private DuplicateStatusValidator validator;
	private String libraryName;
	private HashSet librarySet;
	
	class DuplicateStatusValidator {
		public IStatus validate (String text){
			if(libraries==null || libraries.length==0)
			return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				"", //$NON-NLS-1$
				null);

			if (librarySet.contains(new Path(ClasspathUtilCore.expandLibraryName(text))))
				return new Status(
					IStatus.ERROR,
					PDEPlugin.getPluginId(),
					IStatus.ERROR,
					PDEPlugin.getResourceString(
						"ManifestEditor.RuntimeLibraryDialog.validationError"), //$NON-NLS-1$
					null);
			return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				"", //$NON-NLS-1$
				null);

		}
	}
	public NewRuntimeLibraryDialog(Shell parent, IPluginLibrary[] libraries) {
		super(parent);
		this.libraries = libraries;
		this.validator = new DuplicateStatusValidator();
		librarySet = new HashSet();
		for (int i = 0; i < libraries.length; i++) {
			librarySet.add(new Path(ClasspathUtilCore.expandLibraryName(libraries[i].getName())));
		}
		setStatusLineAboveButtons(true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 8;
		layout.numColumns = 1;
		
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		Label libraryLabel = new Label(container, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		libraryLabel.setLayoutData(gd);
		libraryLabel
				.setText(PDEPlugin
						.getResourceString("ManifestEditor.RuntimeLibraryDialog.label")); //$NON-NLS-1$
		
		libraryText = new Text(container, SWT.SINGLE|SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		libraryText.setLayoutData(gd);
		libraryText
				.setText(PDEPlugin
						.getResourceString("ManifestEditor.RuntimeLibraryDialog.default")); //$NON-NLS-1$
		libraryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus(validator.validate(libraryText.getText()));
			}
		});
		applyDialogFont(container);
		return container;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
		libraryText.setText("library.jar"); //$NON-NLS-1$
		libraryText.setSelection(0, libraryText.getText().length() - 4);
		return super.open();
	}
	public String getLibraryName(){
		return libraryName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#okPressed()
	 */
	protected void okPressed() {
		libraryName = libraryText.getText();
		super.okPressed();
	}

}
