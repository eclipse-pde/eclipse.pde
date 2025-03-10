/*******************************************************************************
 *  Copyright (c) 2005, 2022 IBM Corporation and others.
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

import java.util.Arrays;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddBuildEntryResolution extends BuildEntryMarkerResolution {

	public AddBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddBuildEntryResolution_add, fToken, fEntry);
	}

	@Override
	protected void createChange(Build build) {
		try {
			fEntry = (String) this.marker.getAttribute(PDEMarkerFactory.BK_BUILD_ENTRY);
			fToken = (String) this.marker.getAttribute(PDEMarkerFactory.BK_BUILD_TOKEN);
		} catch (CoreException e) {
		}
		try {
			BuildModel buildModel = build.getModel();
			if (buildModel.isStale()) {
				buildModel.reconciled(buildModel.getDocument());
			}
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			if (buildEntry == null) {
				buildEntry = new BuildEntry(fEntry, buildModel);
			}

			if (fToken != null) {
				buildEntry.addToken(fToken);
			}
		} catch (CoreException e) {
		}
	}

	@Override
	public void run(IMarker marker) {
		super.run(marker);
		this.marker = marker;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		return Arrays.stream(markers).filter(m -> !m.equals(this.marker)).filter(
				m -> CompilerFlags.P_BUILD_SOURCE_LIBRARY.equals(m.getAttribute(PDEMarkerFactory.compilerKey, ""))) //$NON-NLS-1$
				.toArray(IMarker[]::new);
	}
}
