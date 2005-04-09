/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class FeatureSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public FeatureSelectionDialog(Shell parent, IFeatureModel[] models,
			boolean multiSelect) {
		super(parent, PDEPlugin.getDefault().getLabelProvider());
		setTitle(PDEUIMessages.FeatureSelectionDialog_title); //$NON-NLS-1$
		setMessage(PDEUIMessages.FeatureSelectionDialog_message); //$NON-NLS-1$
		setElements(models);
		setMultipleSelection(multiSelect);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.AbstractElementListSelectionDialog#setValidator(org.eclipse.ui.dialogs.ISelectionStatusValidator)
	 */
	public void setValidator(ISelectionStatusValidator validator) {
		super.setValidator(validator);
	}
}
