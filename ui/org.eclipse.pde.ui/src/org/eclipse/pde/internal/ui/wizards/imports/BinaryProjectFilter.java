package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.WorkspaceModelManager;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class BinaryProjectFilter extends ViewerFilter {

	/**
	 * Constructor for BinaryProjectFilter.
	 */
	public BinaryProjectFilter() {
		super();
	}

	/**
	 * @see ViewerFilter#select(Viewer, Object, Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		IProject project = null;

		if (element instanceof IJavaProject) {
			project = ((IJavaProject) element).getProject();
		} else if (element instanceof IProject) {
			project = (IProject) element;
		}
		if (project != null) {
			if (WorkspaceModelManager.isBinaryPluginProject(project)
				|| WorkspaceModelManager.isBinaryFeatureProject(project))
				return false;
		}
		return true;
	}

}
