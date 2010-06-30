/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.build.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddBuildEntryResolution extends BuildEntryMarkerResolution {

	public AddBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public AddBuildEntryResolution(int type, IMarker marker, String entry, String value) {
		super(type, marker);
		fEntry = entry;
		fToken = value;
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddBuildEntryResolution_add, fToken, fEntry);
	}

	protected void createChange(Build build) {
		try {
			BuildModel buildModel = build.getModel();
			if (buildModel.isStale()) {
				buildModel.reconciled(buildModel.getDocument());
			}
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			if (buildEntry == null)
				buildEntry = new BuildEntry(fEntry, buildModel);

			if (fToken != null)
				buildEntry.addToken(fToken);
		} catch (CoreException e) {
		}
	}
}
