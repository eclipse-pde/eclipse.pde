package org.eclipse.pde.internal.ui.wizards.target;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetProfileManager;
import org.eclipse.pde.internal.core.itarget.ITargetModel;

public class TargetProfileFromTargetOperation extends
		BaseTargetProfileOperation {
	
	private String fTargetId;

	public TargetProfileFromTargetOperation(IFile file, String id) {
		super(file);
		fTargetId = id;
	}
	
	protected void initializeTarget(ITargetModel model) {
		IConfigurationElement elem = PDECore.getDefault().getTargetProfileManager().getTarget(fTargetId);
		String path = elem.getAttribute("path");  //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getNamespace();
		URL url = TargetProfileManager.getResourceURL(symbolicName, path);
		if (url != null) {
			try {
				model.load(url.openStream(), false);
			} catch (CoreException e) {
			} catch (IOException e) {
			}
		}
	}

}
