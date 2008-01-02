/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

import java.io.File;
import org.eclipse.core.runtime.IPath;

/**
 * Stores JRE information for a product.  Associates an installed JRE
 * or an execution environment with each target platform.
 */
public interface IJREInfo extends IProductObject {

	/**
	 * Returns the JRE container path that describes the JRE or EE associated
	 * with the specified OS or <code>null</code> if a JRE/EE could not be found.
	 * @param os the string describing the target os, must be one of the Platform.OS constants
	 * @return path representing the JRE container or <code>null</code> 
	 */
	public IPath getJREContainerPath(String os);

	/**
	 * Searches for the JRE or EE associated with the specified target OS.  If
	 * one is found the vm install location for the VM is returned.  <code>null</code>
	 * is returned if a JRE or EE is not associated with the specified OS.
	 * @param os the string describing the target os, must be one of the Platform.OS constants
	 * @return File representing the vm install location or <code>null</code>
	 */
	public File getJVMLocation(String os);

	/**
	 * Sets the JRE container path to associate with the given target OS.  The 
	 * JRE container path may be set to <code>null</code> to remove the association.
	 * @param os the string describing the target os, must be one of the Platform.OS constants 
	 * @param JREContainerPath path representing the JRE container path, can be <code>null</code>
	 */
	public void setJREContainerPath(String os, IPath jreContainerPath);

}
