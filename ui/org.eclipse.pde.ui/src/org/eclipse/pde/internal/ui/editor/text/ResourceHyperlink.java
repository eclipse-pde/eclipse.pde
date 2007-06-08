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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class ResourceHyperlink extends AbstractHyperlink {

	private IResource fResource;
	
	public ResourceHyperlink(IRegion region, String element, IResource res) {
		super(region, element);
		fResource = res;
	}

	public void open() {
		if (fResource == null)
			return;
		if (fElement.indexOf("$nl$/") == 0) //$NON-NLS-1$
			fElement = fElement.substring(5);
		fResource = fResource.getProject().findMember(fElement);
		try {
			if (fResource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)fResource, true);
			else if (fResource != null) {
				IPackagesViewPart part = (IPackagesViewPart)PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
				part.selectAndReveal(fResource);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

}
