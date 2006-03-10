package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveSeperatorBuildEntryResolution extends BuildEntryMarkerResolution {

	public RemoveSeperatorBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	protected void createChange(Build build) {
		try {
			BuildEntry buildEntry = (BuildEntry)build.getEntry(fEntry);
			buildEntry.renameToken(fToken, fToken.substring(0, fToken.length() - 1));
		} catch (CoreException e) {
		}
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.RemoveSeperatorBuildEntryResolution_label, fToken, fEntry);
	}

}
