/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
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

import java.util.Vector;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class PackageSelectionDialog extends ElementListSelectionDialog {

	/**
	 * @param parent
	 * @param renderer
	 */
	public PackageSelectionDialog(Shell parent, ILabelProvider renderer, IJavaProject jProject, Vector<?> existingPackages, boolean allowJava) {
		super(parent, renderer);
		setElements(PDEJavaHelper.getPackageFragments(jProject, existingPackages, allowJava));
		setMultipleSelection(true);
		setMessage(PDEUIMessages.PackageSelectionDialog_label);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(PDEUIMessages.PackageSelectionDialog_title);
		return control;
	}
}
