/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.build;

/**
 * This model factory should be used to
 * create new instances of plugin.jars model
 * objects.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBuildModelFactory {
	/**
	 * Creates a new build entry with
	 * the provided name.
	 * @param name name for the new entry
	 * @return a new build.properties entry instance
	 */
	IBuildEntry createEntry(String name);
}
