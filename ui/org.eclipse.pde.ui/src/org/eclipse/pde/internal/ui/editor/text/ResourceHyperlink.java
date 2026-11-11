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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.util.BundleResourceLocator;
import org.eclipse.swt.widgets.Display;

public class ResourceHyperlink extends AbstractHyperlink {

	private final IResource fResource;

	public ResourceHyperlink(IRegion region, String element, IResource res) {
		super(region, element);
		fResource = res;
	}

	@Override
	public void open() {
		// a model of a bundle outside of the workspace has no resource, a
		// platform: URL can still be resolved without it
		IProject project = fResource == null ? null : fResource.getProject();
		if (!BundleResourceLocator.open(project, fElement)) {
			Display.getDefault().beep();
		}
	}

}
