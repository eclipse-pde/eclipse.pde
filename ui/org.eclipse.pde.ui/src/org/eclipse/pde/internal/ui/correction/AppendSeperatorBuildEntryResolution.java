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
			BuildEntry buildEntry = (BuildEntry)build.getEntry(fEntry);
			buildEntry.renameToken(fToken, fToken + '/');
		} catch (CoreException e) {
		}
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.AppendSeperatorBuildEntryResolution_label, fToken);
	}

}
