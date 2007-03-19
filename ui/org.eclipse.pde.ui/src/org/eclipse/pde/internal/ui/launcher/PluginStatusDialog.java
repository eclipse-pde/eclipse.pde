/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


public class PluginStatusDialog extends TrayDialog {
	
	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			return (Object[])fInput.get(parentElement);
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return fInput.containsKey(element);
		}

		public Object[] getElements(Object inputElement) {
			return ((Map)inputElement).keySet().toArray();
		}
		
	}
	
	private boolean fShowCancelButton;
	private Map fInput;

	public PluginStatusDialog(Shell parentShell, int style) {
		super(parentShell);
		setShellStyle(getShellStyle() | style);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public PluginStatusDialog(Shell parentShell) {
		this(parentShell, SWT.RESIZE);
	}
	
	public void showCancelButton(boolean showCancel) {
		fShowCancelButton = showCancel;
	}
	
	public void setInput(Map input) {
		fInput = input;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		if (fShowCancelButton)
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		container.setLayoutData(gd);

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.PluginStatusDialog_label); 
		
		TreeViewer treeViewer = new TreeViewer(container);
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		treeViewer.setComparator(new ViewerComparator());
		treeViewer.setInput(fInput);
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		getShell().setText(PDEUIMessages.PluginStatusDialog_pluginValidation); 
		Dialog.applyDialogFont(container);
		return container;
	}
	
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	public Object[] getChildren(Object parentElement) {
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}

	public Object[] getElements(Object inputElement) {
		return null;
	}

}
