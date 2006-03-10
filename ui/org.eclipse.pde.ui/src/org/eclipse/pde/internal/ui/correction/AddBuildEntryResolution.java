package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddBuildEntryResolution extends BuildEntryMarkerResolution {

	public AddBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddBuildEntryResolution_add, fToken, fEntry);
	}

	public String getDescription() {
		return getLabel();
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry)build.getEntry(fEntry);
			if (buildEntry == null)
				buildEntry = new BuildEntry(fEntry, build.getModel());
			buildEntry.addToken(fToken);
		} catch (CoreException e) {
		}
	}
}
