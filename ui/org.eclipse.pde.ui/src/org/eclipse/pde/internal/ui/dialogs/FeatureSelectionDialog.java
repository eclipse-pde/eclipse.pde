/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.dialogs;

import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class FeatureSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public FeatureSelectionDialog(Shell parent, IFeatureModel[] models, boolean multiSelect) {
		super(parent, PDEPlugin.getDefault().getLabelProvider());
		setTitle(PDEUIMessages.FeatureSelectionDialog_title);
		setMessage(PDEUIMessages.FeatureSelectionDialog_message);
		setElements(models);
		setMultipleSelection(multiSelect);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.FEATURE_SELECTION);
	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

}
