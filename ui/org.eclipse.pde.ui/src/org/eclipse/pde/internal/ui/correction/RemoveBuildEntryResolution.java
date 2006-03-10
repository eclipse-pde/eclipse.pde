package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveBuildEntryResolution extends BuildEntryMarkerResolution {

	public RemoveBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getLabel() {
		if (fToken == null)
			return NLS.bind(PDEUIMessages.RemoveBuildEntryResolution_removeEntry, fEntry);
		return NLS.bind(PDEUIMessages.RemoveBuildEntryResolution_removeToken, fToken, fEntry);
	}

	public String getDescription() {
		return getLabel();
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry)build.getEntry(fEntry);
			if (buildEntry == null)
				return;
			if (fToken == null)
				build.remove(buildEntry);
			else
				buildEntry.removeToken(fToken);
		} catch (CoreException e) {
		}
	}

}
