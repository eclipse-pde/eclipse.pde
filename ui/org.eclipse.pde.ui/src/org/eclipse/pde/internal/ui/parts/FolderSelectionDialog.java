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
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class FolderSelectionDialog extends ElementTreeSelectionDialog implements ISelectionChangedListener {

	private Button fNewFolderButton;
	private IContainer fSelectedContainer;

	public FolderSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		setSorter(new ResourceSorter(ResourceSorter.NAME));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite result= (Composite)super.createDialogArea(parent);
		
		getTreeViewer().addSelectionChangedListener(this);
		getTreeViewer().expandToLevel(2);
		Button button = new Button(result, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("BuildPropertiesEditor.SourceFolderSelectionDialog.button")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				newFolderButtonPressed();
			}
		});
		button.setFont(parent.getFont());
		GridData data= new GridData();
		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		button.setLayoutData(data);
		fNewFolderButton= button;
		
		applyDialogFont(result);		
		return result;
	}

	private void updateNewFolderButtonState() {
		IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
		fSelectedContainer= null;
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IContainer) {
				fSelectedContainer= (IContainer) first;
			}
		}
		fNewFolderButton.setEnabled(fSelectedContainer != null);
	}	
	
	protected void newFolderButtonPressed() {
		NewFolderDialog dialog= new NewFolderDialog(getShell(), fSelectedContainer);
		if (dialog.open() == Window.OK) {
			TreeViewer treeViewer= getTreeViewer();
			treeViewer.refresh(fSelectedContainer);
			Object createdFolder;
			if (dialog.getResult()!=null){
				createdFolder= dialog.getResult()[0];
				treeViewer.reveal(createdFolder);
				treeViewer.setSelection(new StructuredSelection(createdFolder));
			} 
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		updateNewFolderButtonState();
	}
	
	


}
