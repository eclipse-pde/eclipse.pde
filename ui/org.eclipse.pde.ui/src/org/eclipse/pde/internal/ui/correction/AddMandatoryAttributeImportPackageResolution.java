/*******************************************************************************
 *  Copyright (c) 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *Qui
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alshama M S <ALSHAMA.M.S@ibm.com> Initial implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Constants;

/**
 * <p>
 * Represents a resolution to the problem of import package resolution failure
 * due to missing mandatory attributes
 * </p>
 */
public class AddMandatoryAttributeImportPackageResolution extends AbstractManifestMarkerResolution {
	/**
	 * Creates a new resolution
	 */
	public AddMandatoryAttributeImportPackageResolution(IMarker marker) {
		super(AbstractPDEMarkerResolution.CREATE_TYPE, marker);
	}

	/**
	 * Resolves the problem by adding mandatory attribute
	 */
	@Override
	protected void createChange(BundleModel model) {
		try {
			String packagename = (String) marker.getAttribute("packageName"); //$NON-NLS-1$
			Bundle bundle = (Bundle) model.getBundle();
			ImportPackageHeader header = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
			if (header != null) {
				ImportPackageObject obj = header.getPackage(packagename);
				obj.setAttribute((String) marker.getAttribute("mandatoryattrkey"), //$NON-NLS-1$
						(String) marker.getAttribute("mandatoryvalue"));//$NON-NLS-1$
				header.removePackage(packagename);
				header.addPackage(obj);
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	public String getDescription() {
		return PDECoreMessages.AddMandatoryAttrResolution_label;
	}

	@Override
	public String getLabel() {
		return PDECoreMessages.AddMandatoryAttrResolution_description;
	}

}