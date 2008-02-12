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
package org.eclipse.pde.api.tools.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;

/**
 * Manages a cache of API descriptions for Java projects. Descriptions
 * are re-used between API components for the same project.
 * 
 * @since 1.0
 * 
 * TODO: persist/restore descriptions
 * TODO: remove projects from cache as removed/closed from workspace
 */
public class ApiDescriptionManager {
	
	/**
	 * Singleton
	 */
	private static ApiDescriptionManager fgDefault;
	
	private Map fDescriptions = new HashMap();

	/**
	 * Constructs an API description manager.
	 */
	private ApiDescriptionManager() {
	}
	
	/**
	 * Returns the singleton API description manager.
	 * 
	 * @return API description manager
	 */
	public synchronized static ApiDescriptionManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new ApiDescriptionManager();
		}
		return fgDefault;
	}
	
	/**
	 * Returns an API description for the given Java project.
	 * 
	 * @param project Java project
	 * @return API description
	 */
	public synchronized IApiDescription getApiDescription(IJavaProject project) {
		IApiDescription description = (IApiDescription) fDescriptions.get(project);
		if (description == null) {
			description = new ProjectApiDescription(project);
			fDescriptions.put(project, description);
		}
		return description;
	}

}
