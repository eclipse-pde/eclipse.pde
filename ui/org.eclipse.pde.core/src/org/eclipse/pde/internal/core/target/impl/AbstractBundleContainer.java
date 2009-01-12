/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.impl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * Common function for bundle containers.
 * 
 * @since 3.5
 */
public abstract class AbstractBundleContainer implements IBundleContainer {

	/**
	 * Resolves any string substitution variables in the given text returning
	 * the result.
	 * 
	 * @param text text to resolve
	 * @return result of the resolution
	 * @throws CoreException if unable to resolve 
	 */
	protected String resolveVariables(String text) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(text);
	}
}
