/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.HashSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

public class NewRuntimeLibraryDialog extends SelectionStatusDialog {
	private Text libraryText;
	private IPluginLibrary[] libraries;
	private DuplicateStatusValidator validator;
	private String libraryName;
	private HashSet<Path> librarySet;

	class DuplicateStatusValidator {
		public IStatus validate(String text) {
			if (text.length() == 0)
				return Status.error(PDEUIMessages.AddLibraryDialog_emptyLibraries);

			if (text.indexOf(' ') != -1)
				return Status.error(PDEUIMessages.AddLibraryDialog_nospaces);

			if (libraries == null || libraries.length == 0)
				return Status.OK_STATUS;

			if (librarySet.contains(new Path(ClasspathUtilCore.expandLibraryName(text))))
				return Status.error(PDEUIMessages.ManifestEditor_RuntimeLibraryDialog_validationError);
			return Status.OK_STATUS;
		}
	}

	public NewRuntimeLibraryDialog(Shell parent, IPluginLibrary[] libraries) {
		super(parent);
		this.libraries = libraries;
		this.validator = new DuplicateStatusValidator();
		librarySet = new HashSet<>();
		for (IPluginLibrary library : libraries) {
			librarySet.add(new Path(ClasspathUtilCore.expandLibraryName(library.getName())));
		}
		setStatusLineAboveButtons(true);
	}

	@Override
	protected void computeResult() {
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.NEW_LIBRARY);
	}

	@Override
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
		libraryLabel.setText(PDEUIMessages.ManifestEditor_RuntimeLibraryDialog_label);

		libraryText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		libraryText.setLayoutData(gd);
		libraryText.setText(PDEUIMessages.ManifestEditor_RuntimeLibraryDialog_default);
		libraryText.addModifyListener(e -> updateStatus(validator.validate(libraryText.getText())));
		applyDialogFont(container);
		return container;
	}

	@Override
	public int open() {
		libraryText.setText("library.jar"); //$NON-NLS-1$
		libraryText.setSelection(0, libraryText.getText().length() - 4);
		return super.open();
	}

	public String getLibraryName() {
		return libraryName;
	}

	@Override
	protected void okPressed() {
		libraryName = libraryText.getText();
		super.okPressed();
	}

}
