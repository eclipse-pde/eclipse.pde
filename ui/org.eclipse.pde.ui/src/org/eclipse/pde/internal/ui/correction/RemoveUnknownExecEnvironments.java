package org.eclipse.pde.internal.ui.correction;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RemoveUnknownExecEnvironments extends AbstractManifestMarkerResolution {

	public RemoveUnknownExecEnvironments(int type) {
		super(type);
	}

	protected void createChange(BundleModel model) {
		IManifestHeader header = model.getBundle().getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader) {
			RequiredExecutionEnvironmentHeader reqHeader = (RequiredExecutionEnvironmentHeader)header;
			ExecutionEnvironment[] bundleEnvs = reqHeader.getEnvironments();
			IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			for (int i = 0; i < bundleEnvs.length; i++) {
				boolean found = false;
				for (int j = 0; j < systemEnvs.length; j++) {
					if (bundleEnvs[i].getName().equals(systemEnvs[j].getId())) {
						found = true;
						break;
					}
				}
				if (!found)
					reqHeader.removeExecutionEnvironment(bundleEnvs[i]);
			}
		}
	}

	public String getLabel() {
		return PDEUIMessages.RemoveUnknownExecEnvironments_label;
	}

}
