/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Leak detectors keep track of all pre-requisite non-API package names to weed
 * out public references.
 *
 * @since 1.1
 * @noextend This class is not intended to be sub-classed by clients.
 */
public abstract class AbstractLeakProblemDetector extends AbstractProblemDetector {

	private Set<String> fNonApiPackageNames;

	public AbstractLeakProblemDetector(Set<String> nonApiPackageNames) {
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
		
		// if reference has noimplement restriction, it could leak non-API types
		if (RestrictionModifiers.isImplementRestriction(member.getModifiers())) {
			return true;
		}
		// if reference has noextend restriction, it could be indirectly be
		// extended bypassing the noextend
		if (RestrictionModifiers.isImplementRestriction(member.getModifiers())) {
			return true;
		}

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
				// the type is private or default protection, do not retain the
				// reference
				return false;
			}
			type = type.getEnclosingType();
		}
		return true;
	}
}
