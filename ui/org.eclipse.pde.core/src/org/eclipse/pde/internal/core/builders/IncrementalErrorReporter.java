/*******************************************************************************
 *  Copyright (c) 2018 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;

public class IncrementalErrorReporter {

	private final IResource fResource;
	private final Collection<VirtualMarker> fReportedMarkers = new ArrayList<>();
	private int fErrorCount;

	public IncrementalErrorReporter(IResource file) {
		fResource = file;
	}

	public VirtualMarker addMarker(String message, int lineNumber, int severity, int problemID, String category) {

		if (lineNumber == -1) {
			lineNumber = 1;
		}

		if (severity == IMarker.SEVERITY_ERROR) {
			fErrorCount++;
		}

		VirtualMarker marker = new VirtualMarker();
		marker.setAttribute(PDEMarkerFactory.PROBLEM_ID, problemID);
		marker.setAttribute(PDEMarkerFactory.CAT_ID, category);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);

		fReportedMarkers.add(marker);

		return marker;
	}

	public void applyMarkers() {
		IMarker[] existingMarkers;
		try {
			// This seem to be for compatibility with some legacy code,
			// PDE builders don't create markers with this type anymore
			fResource.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			existingMarkers = fResource.findMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			PDECore.logException(e);
			// If we can't read existing, let delete them before we create new
			existingMarkers = new IMarker[0];
			try {
				fResource.deleteMarkers(PDEMarkerFactory.MARKER_ID, false, IResource.DEPTH_ZERO);
			} catch (CoreException e1) {
				PDECore.logException(e1);
			}
		}

		// iterate over existing markers to check which are resolved now
		for (IMarker marker : existingMarkers) {
			boolean resolved = true;
			Map<String, Object> existingAttributes = null;

			// Iterate over new markers to filter out all we already know
			for (Iterator<VirtualMarker> it = fReportedMarkers.iterator(); it.hasNext();) {
				VirtualMarker reportedMarker = it.next();
				if (existingAttributes == null) {
					try {
						existingAttributes = marker.getAttributes();
					} catch (Exception e) {
						PDECore.logException(e);
						// assume the marker is not accessible, can be deleted
						break;
					}
				}
				// Same marker is found, no need to create again
				if (reportedMarker.getAttributes().equals(existingAttributes)) {
					resolved = false;
					it.remove();
					break;
				}
			}

			// The marker was not reported again, the old one can be deleted
			if (resolved) {
				try {
					marker.delete();
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}

		// Create only new markers
		for (VirtualMarker reportedMarker : fReportedMarkers) {
			try {
				fResource.createMarker(PDEMarkerFactory.MARKER_ID).setAttributes(reportedMarker.getAttributes());
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
	}

	public int getErrorCount() {
		return fErrorCount;
	}

	public static class VirtualMarker {

		private final Map<String, Object> fAttributes = new HashMap<>();

		public void setAttribute(String key, Object value) {
			fAttributes.put(key, value);
		}

		public Map<String, Object> getAttributes() {
			return fAttributes;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("VirtualMarker ["); //$NON-NLS-1$
			if (fAttributes != null) {
				builder.append("attributes="); //$NON-NLS-1$
				builder.append(fAttributes);
			}
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}

	}

}
