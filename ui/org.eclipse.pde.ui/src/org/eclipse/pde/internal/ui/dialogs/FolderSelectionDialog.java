/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.dialogs;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class FolderSelectionDialog extends ElementTreeSelectionDialog implements ISelectionChangedListener {

	private Button fNewFolderButton;
	private IContainer fSelectedContainer;

	public FolderSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		setComparator(new ResourceComparator(ResourceComparator.NAME));
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.FOLDER_SELECTION_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite result = (Composite) super.createDialogArea(parent);

		getTreeViewer().addSelectionChangedListener(this);
		getTreeViewer().expandToLevel(2);
		fNewFolderButton = new Button(result, SWT.PUSH);
		fNewFolderButton.setText(PDEUIMessages.BuildEditor_SourceFolderSelectionDialog_button);
		fNewFolderButton.addSelectionListener(widgetSelectedAdapter(event -> newFolderButtonPressed()));
		fNewFolderButton.setFont(parent.getFont());
		fNewFolderButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fNewFolderButton);

		applyDialogFont(result);
		return result;
	}

	private void updateNewFolderButtonState() {
		IStructuredSelection selection = getTreeViewer().getStructuredSelection();
		fSelectedContainer = null;
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof IContainer) {
				fSelectedContainer = (IContainer) first;
			}
		}
		fNewFolderButton.setEnabled(fSelectedContainer != null);
	}

	protected void newFolderButtonPressed() {
		NewFolderDialog dialog = new NewFolderDialog(getShell(), fSelectedContainer);
		if (dialog.open() == Window.OK) {
			TreeViewer treeViewer = getTreeViewer();
			treeViewer.refresh(fSelectedContainer);
			Object createdFolder;
			if (dialog.getResult() != null) {
				createdFolder = dialog.getResult()[0];
				treeViewer.reveal(createdFolder);
				treeViewer.setSelection(new StructuredSelection(createdFolder));
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateNewFolderButtonState();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.ManifestEditor_addActionText, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

}
