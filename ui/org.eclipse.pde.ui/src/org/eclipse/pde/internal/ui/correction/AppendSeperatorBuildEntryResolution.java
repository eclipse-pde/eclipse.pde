/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AppendSeperatorBuildEntryResolution extends BuildEntryMarkerResolution {

	public AppendSeperatorBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			buildEntry.renameToken(fToken, fToken + '/');
		} catch (CoreException e) {
		}
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AppendSeperatorBuildEntryResolution_label, fToken);
	}

}
