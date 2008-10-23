/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;


/**
 * Produces RegistryModels for URLs. Valid URLs:
 * local
 * target
 * remote://host:port
 *
 */
public class RegistryModelFactory {

	/**
	 * 
	 * @param uri
	 * @return never returns null
	 */
	public static RegistryModel getRegistryModel(String uri) {
		return new RegistryModel(new LocalRegistryBackend());
	}
}
