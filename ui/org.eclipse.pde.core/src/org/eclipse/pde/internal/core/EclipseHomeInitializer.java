/**
 * Created on Apr 10, 2002
 *
 * To change this generated comment edit the template variable "filecomment":
 * Workbench>Preferences>Java>Templates.
 */
package org.eclipse.pde.internal.core;

import java.util.HashMap;

import org.eclipse.core.runtime.Path;
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
	}
	
	public static void resetEclipseHomeVariables(HashMap eclipseHomeVariables) {
		String[] variables = JavaCore.getClasspathVariableNames();
		for (int i = 0; i < variables.length; i++) {
			if (variables[i].startsWith(PDECore.ECLIPSE_HOME_VARIABLE))
				JavaCore.removeClasspathVariable(variables[i], null);
		}

		try {
			Object[] keys = eclipseHomeVariables.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				JavaCore.setClasspathVariable((String)keys[i], new Path(eclipseHomeVariables.get(keys[i]).toString()),null);
			}
		} catch (JavaModelException e) {
		}
	}

	
}
