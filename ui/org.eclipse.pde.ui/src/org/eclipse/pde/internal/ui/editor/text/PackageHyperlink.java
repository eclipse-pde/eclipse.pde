/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BasePackageHeader;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

public class PackageHyperlink extends ManifestElementHyperlink {

	BasePackageHeader fHeader;
	
	public PackageHyperlink(IRegion region, String pack, BasePackageHeader header) {
		super(region, pack);
		fHeader = header;
	}

	protected void open2() {
		IResource res = fHeader.getModel().getUnderlyingResource();
		if (res == null)
			return;
		PDEManifestElement[] elements = fHeader.getElements();
		for (int i = 0; i < elements.length; i++) {
			if (!(elements[i] instanceof PackageObject))
				continue;
			if (((PackageObject)elements[i]).getName().equals(fElement)) {
				IProject project = res.getProject();
				openPackage((PackageObject)elements[i], project);
				return;
			}
		}
	}

	private void openPackage(PackageObject object, IProject project) {
		IPackageFragment frag = PDEJavaHelper.getPackageFragment(object, null, project);
		if (frag == null)
			return;
		try {
			IViewPart part = PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
			ShowInPackageViewAction action = new ShowInPackageViewAction(part.getSite());
			action.run(frag);
		} catch (PartInitException e) {
		}
	}

}
