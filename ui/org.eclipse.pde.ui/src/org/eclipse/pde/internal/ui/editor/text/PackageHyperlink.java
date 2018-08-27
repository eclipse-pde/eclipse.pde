/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BasePackageHeader;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

public class PackageHyperlink extends ManifestElementHyperlink {

	BasePackageHeader fHeader;

	public PackageHyperlink(IRegion region, String pack, BasePackageHeader header) {
		super(region, pack);
		fHeader = header;
	}

	@Override
	protected void open2() {
		IResource res = fHeader.getBundle().getModel().getUnderlyingResource();
		if (res == null)
			return;
		IPackageFragment frag = PDEJavaHelper.getPackageFragment(fElement, null, res.getProject());
		if (frag == null)
			return;
		try {
			IViewPart part = PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
			ShowInPackageViewAction action = new ShowInPackageViewAction(part.getSite());
			action.run(frag);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

}
