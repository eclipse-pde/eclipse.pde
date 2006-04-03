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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.OrganizeManifest;

public class OrganizeExportPackageResolution extends
		AbstractManifestMarkerResolution {
	
	private IProject fProject;

	public OrganizeExportPackageResolution(int type, IProject project) {
		super(type);
		fProject = project;
	}

	protected void createChange(BundleModel model) {
		OrganizeManifest.organizeExportPackages(model.getBundle(), fProject, true, true);
	}

	public String getDescription() {
		return PDEUIMessages.OrganizeExportPackageResolution_Description;
	}

	public String getLabel() {
		return PDEUIMessages.OrganizeExportPackageResolution_Label;
	}

}
