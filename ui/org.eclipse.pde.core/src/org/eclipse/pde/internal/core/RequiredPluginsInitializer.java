package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

/**
 * 
 */
public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	/**
	 * Constructor for RequiredPluginsInitializer.
	 */
	public RequiredPluginsInitializer() {
		super();
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(IPath, IJavaProject)
	 */
	public void initialize(IPath containerPath, IJavaProject javaProject)
		throws CoreException {
		IProject project = javaProject.getProject();
		ModelEntry entry =
			PDECore.getDefault().getModelManager().findEntry(project);
		if (entry == null) {
			ModelEntry.updateUnknownClasspathContainer(javaProject);
		} else {
			entry.updateClasspathContainer(true);
		}
	}
}