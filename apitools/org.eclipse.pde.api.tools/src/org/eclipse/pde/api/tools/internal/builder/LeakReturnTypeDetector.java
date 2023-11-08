/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaks in method return types
 *
 * @since 1.1
 */
public class LeakReturnTypeDetector extends MethodLeakDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public LeakReturnTypeDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_METHOD_RETURN_TYPE;
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_RETURNTYPE;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.LEAK_RETURN_TYPE;
	}

	@Override
	protected boolean isProblem(IReference reference, IProgressMonitor monitor) {
		if (super.isProblem(reference, monitor) == true) {
			return true;
		}
		IApiType type = (IApiType) reference.getResolvedReference();
		int modifiers = type.getModifiers();
		if( Flags.isPackageDefault(modifiers) == false) {
			if (reference instanceof Reference) {
				try {
					List<IApiType> parameterList = ((Reference) reference).getParameterList();
					for (IApiType iApiType : parameterList) {
						if (Flags.isPackageDefault(iApiType.getModifiers())) {
							return true;
						}
					}
				} catch (CoreException e) {
					checkIfDisposed(reference.getMember().getApiComponent(), monitor);
					// do nothing, skip it
				}
			}
		}
		return Flags.isPackageDefault(modifiers);
	}

}
