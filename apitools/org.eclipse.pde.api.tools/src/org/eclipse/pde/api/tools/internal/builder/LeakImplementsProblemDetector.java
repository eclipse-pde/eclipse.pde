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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaked implemented interfaces.
 *
 * @since 1.1
 */
public class LeakImplementsProblemDetector extends AbstractTypeLeakDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public LeakImplementsProblemDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_IMPLEMENTS;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_IMPLEMENT;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.LEAK_IMPLEMENTS;
	}

	@Override
	public boolean isProblem(IReference reference) {
		boolean isProb = super.isProblem(reference);
		// check if no implement interface is implemented and thereby leaking api
		// types from noimplement interface
		if (isProb == false) {
			IApiMember member = reference.getResolvedReference();
			IApiMember sourceMember = reference.getMember();
			try {
				IApiAnnotations annotations = member.getApiComponent().getApiDescription().resolveAnnotations(member.getHandle());
				if (annotations != null) {
					if (RestrictionModifiers.isImplementRestriction(annotations.getRestrictions())) {
						IApiAnnotations annotationsSource = member.getApiComponent().getApiDescription().resolveAnnotations(sourceMember.getHandle());
						if (annotationsSource != null && !RestrictionModifiers.isImplementRestriction(annotationsSource.getRestrictions())) {
							isProb= true;
						}
					}
				}
			}
			catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return isProb;
	}
}
