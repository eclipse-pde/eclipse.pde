/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class TreeMessageDialog extends MessageDialog {

	private ITreeContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;

	public TreeMessageDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
	}

	@Override
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
