package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RequiredPluginsClasspathContainer implements IClasspathContainer {
	private IPluginModelBase model;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		this.model = model;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		return BuildPathUtilCore.computePluginEntries(model);
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return "Required plug-in entries (press 'Edit...' for details)";
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		String projectName = model.getUnderlyingResource().getProject().getName();
		return new Path(PDECore.CLASSPATH_CONTAINER_ID).append(projectName);
	}

}
