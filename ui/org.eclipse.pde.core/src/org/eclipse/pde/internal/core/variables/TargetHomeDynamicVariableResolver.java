/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.pde.core.plugin.TargetPlatform;

public class TargetHomeDynamicVariableResolver implements IDynamicVariableResolver {

	/**
	 * Resolver for ${target_home}
	 *
	 * @since 3.2
	 */
	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		return TargetPlatform.getLocation();
	}

}
