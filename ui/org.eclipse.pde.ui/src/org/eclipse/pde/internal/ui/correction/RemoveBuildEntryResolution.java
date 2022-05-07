/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import java.util.HashSet;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveBuildEntryResolution extends BuildEntryMarkerResolution {

	public RemoveBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		if (fToken == null)
			return NLS.bind(PDEUIMessages.RemoveBuildEntryResolution_removeEntry, fEntry);
		return NLS.bind(PDEUIMessages.RemoveBuildEntryResolution_removeToken, fToken, fEntry);
	}

	@Override
	protected void createChange(Build build) {
		try {
			fEntry = (String) this.marker.getAttribute(PDEMarkerFactory.BK_BUILD_ENTRY);
			fToken = (String) this.marker.getAttribute(PDEMarkerFactory.BK_BUILD_TOKEN);
		} catch (CoreException e) {
		}
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			if (buildEntry == null)
				return;
			if (fToken == null)
				build.remove(buildEntry);
			else {
				buildEntry.removeToken(fToken);
				if (buildEntry.getTokens().length == 0) {
					build.remove(buildEntry);
				}
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
return Arrays.stream(markers).filter(m -> !m.equals(this.marker)).filter(m -> {
      String str = m.getAttribute(PDEMarkerFactory.compilerKey, ""); //$NON-NLS-1$
      return str.equals(CompilerFlags.P_BUILD_SOURCE_LIBRARY) || str.equals(CompilerFlags.P_BUILD_SRC_INCLUDES)
          || str.equals(CompilerFlags.P_BUILD_BIN_INCLUDES) || str.equals(CompilerFlags.P_BUILD_OUTPUT_LIBRARY);
    }).toArray(IMarker[]::new);
	}

}
