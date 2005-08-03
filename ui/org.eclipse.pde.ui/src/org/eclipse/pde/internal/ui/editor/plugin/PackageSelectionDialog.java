/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

public class PackageSelectionDialog extends ElementListSelectionDialog {

	public PackageSelectionDialog(Shell parent, ILabelProvider renderer, IJavaProject jProject) {
		this(parent, renderer, jProject, new Vector());
	}
	/**
	 * @param parent
	 * @param renderer
	 */
	public PackageSelectionDialog(Shell parent, ILabelProvider renderer, IJavaProject jProject, Vector existingPackages) {
		super(parent, renderer);
		setElements(jProject, existingPackages);
		setMultipleSelection(true);
		setMessage(PDEUIMessages.PackageSelectionDialog_label);
	}
	/**
	 * 
	 */
	private void setElements(IJavaProject jProject, Vector existingPackages) {
		HashMap map = new HashMap();
		try {
			IPackageFragmentRoot[] roots = getRoots(jProject);
			for (int i = 0; i < roots.length; i++) {
				IJavaElement[] children = roots[i].getChildren();
				for (int j = 0; j < children.length; j++) {
					IPackageFragment fragment = (IPackageFragment)children[j];
					if (fragment.hasChildren() && !existingPackages.contains(fragment.getElementName()))
						map.put(fragment.getElementName(), fragment);
				}
			}
		} catch (JavaModelException e) {
		}
		setElements(map.values().toArray());
	}
	
	private IPackageFragmentRoot[] getRoots(IJavaProject jProject) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[])result.toArray(new IPackageFragmentRoot[result.size()]);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ElementListSelectionDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		getShell().setText(PDEUIMessages.PackageSelectionDialog_title); 
		return control;
	}
}
