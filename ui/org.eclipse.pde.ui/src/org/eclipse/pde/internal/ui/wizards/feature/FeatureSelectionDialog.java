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

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

public class FeatureSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public FeatureSelectionDialog(Shell parent, IFeatureModel[] models) {
		super(parent, PDEPlugin.getDefault().getLabelProvider());
		setTitle(PDEPlugin.getResourceString("FeatureSelectionDialog.title")); //$NON-NLS-1$
		setMessage(PDEPlugin.getResourceString("FeatureSelectionDialog.message")); //$NON-NLS-1$
		setElements(models);
		setMultipleSelection(false);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.AbstractElementListSelectionDialog#setValidator(org.eclipse.ui.dialogs.ISelectionStatusValidator)
	 */
	public void setValidator(ISelectionStatusValidator validator) {
		super.setValidator(validator);
	}
}
