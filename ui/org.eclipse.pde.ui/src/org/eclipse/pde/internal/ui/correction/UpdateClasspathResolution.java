package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.plugin.ClasspathComputer;

public class UpdateClasspathResolution extends AbstractPDEMarkerResolution {

	public UpdateClasspathResolution(int type) {
		super(type);
	}

	public String getLabel() {
		return PDEUIMessages.UpdateClasspathResolution_label;
	}

	public void run(IMarker marker) {
		IProject project = marker.getResource().getProject();
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
		try {
			ClasspathComputer.setClasspath(project, model);
		} catch (CoreException e) {
		}
	}

	protected void createChange(IBaseModel model) {
		// handled by run
	}

}
