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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.Signatures;


/**
 * Leak detectors keep track of all pre-requisite non-API package names to weed out
 * public references.
 * 
 * @since 1.1
 * @noextend This class is not intended to be subclassed by clients.
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
		String packageName = Signatures.getPackageName(reference.getReferencedTypeName());
		if (fNonApiPackageNames.contains(packageName)) {
			return true;
		}
		// could be a reference to a package visible type
		IApiMember member = reference.getMember();
		IApiType type = null;
		if (member.getType() == IApiElement.TYPE) {
			type = (IApiType) member;
		} else {
			type = (IApiType) member.getAncestor(IApiElement.TYPE);
		}
		String origin = Signatures.getPackageName(type.getName());
		if (packageName.equals(origin)) {
			return true; // possible package visible reference
		}
		return false;
	}
	
	/**
	 * Returns whether all enclosing types of the given member are visible.
	 * 
	 * @param member member
	 * @return whether all enclosing types of the given member are visible
	 * @throws CoreException
	 */
	protected boolean isEnclosingTypeVisible(IApiMember member) throws CoreException {
		IApiType type = null;
		if (member.getType() == IApiElement.TYPE) {
			type = (IApiType) member;
		} else {
			type = member.getEnclosingType();
		}
		while (type != null) {
			if (((Flags.AccPublic | Flags.AccProtected) & type.getModifiers()) == 0) {
				// the type is private or default protection, do not retain the reference
				return false;
			}
			type = type.getEnclosingType();
		}
		return true;
	}	
}
