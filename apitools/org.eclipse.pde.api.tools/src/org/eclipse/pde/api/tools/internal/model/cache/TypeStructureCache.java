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
package org.eclipse.pde.api.tools.internal.model.cache;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * LRU Cache of type structures.
 * 
 * @since 1.1
 */
public class TypeStructureCache {
	
	/**
	 * Returns a type model object for the specified type originating from the specified component.
	 * Note that when an API component is not specified, some operations will not be available
	 * on the resulting {@link IApiType} (such as navigating super types, member types, etc).
	 *  
	 * @param classFile class file
	 * @param component API component or <code>null</code>
	 * @return type structure or
	 * @throws CoreException if unable to retrieve build the structure.
	 */
	public static IApiType getTypeStructure(IClassFile classFile, IApiComponent component) throws CoreException {
		return TypeStructureBuilder.buildTypeStructure(classFile.getContents(), component, classFile);
	}

}
