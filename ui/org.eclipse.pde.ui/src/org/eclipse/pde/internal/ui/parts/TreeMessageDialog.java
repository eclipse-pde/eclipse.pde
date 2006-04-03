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
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class TreeMessageDialog extends MessageDialog {
	
	private ITreeContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;

	public TreeMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				dialogImageType, dialogButtonLabels, defaultIndex);
	}
	
	protected Control createCustomArea(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		viewer.getTree().setLayoutData(gd);
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		viewer.setInput(fInput);
		applyDialogFont(viewer.getControl());
		return viewer.getControl();
	}
	
	public void setContentProvider(ITreeContentProvider provider) {
		fContentProvider = provider;
	}
	
	public void setLabelProvider(ILabelProvider provider) {
		fLabelProvider = provider;
	}
	
	public void setInput(Object input) {
		fInput = input;
	}

}
