/**
 * Created on Apr 10, 2002
 *
 * To change this generated comment edit the template variable "filecomment":
 * Workbench>Preferences>Java>Templates.
 */
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 *
 */
public class EclipseHomeInitializer extends ClasspathVariableInitializer {

	/**
	 * Constructor for EclipseHomeInitializer.
	 */
	public EclipseHomeInitializer() {
		super();
	}

	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		try {
			Preferences pref = PDECore.getDefault().getPluginPreferences();
			JavaCore.setClasspathVariable(
				variable,
				new Path(pref.getString(ICoreConstants.PLATFORM_PATH)),
				new NullProgressMonitor());
		} catch (JavaModelException e) {
		}
	}
	
}
