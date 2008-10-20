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
package org.eclipse.pde.api.tools.internal.search;

import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.util.Util;


/**
 * Leak detectors keep track of all pre-requisite non-API package names to weed out
 * public references.
 * 
 * @since 1.1
 */
public abstract class AbstractLeakProblemDetector extends AbstractProblemDetector {

	private Set fNonApiPackageNames;
	
	public AbstractLeakProblemDetector(Set nonApiPackageNames) {
		fNonApiPackageNames = nonApiPackageNames;
	}
	
	/**
	 * Returns whether the referenced type name matches a non-API package.
	 * 
	 * @param reference
	 * @return whether the referenced type name matches a non-API package
	 */
	protected boolean isNonAPIReference(IReference reference) {
		String packageName = Util.getPackageName(reference.getReferencedTypeName());
		return fNonApiPackageNames.contains(packageName);
	}
}
