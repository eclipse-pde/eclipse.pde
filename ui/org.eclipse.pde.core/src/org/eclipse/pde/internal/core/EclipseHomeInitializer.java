/**
 * Created on Apr 10, 2002
 *
 * To change this generated comment edit the template variable "filecomment":
 * Workbench>Preferences>Java>Templates.
 */
package org.eclipse.pde.internal.core;

import org.eclipse.jdt.core.ClasspathVariableInitializer;

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
		// No need to do anything - PDECore will initialize
		// variable anyway
	}
}
