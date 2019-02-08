/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeRequireBundleResolution extends AbstractManifestMarkerResolution {

	private boolean fRemoveImports;

	public OrganizeRequireBundleResolution(int type, boolean removeImports, IMarker marker) {
		super(type, marker);
		fRemoveImports = removeImports;
	}

	@Override
	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeRequireBundles(model.getBundle(), fRemoveImports);
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.OrganizeRequireBundleResolution_Description;
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.OrganizeRequireBundleResolution_Label;
	}

}
