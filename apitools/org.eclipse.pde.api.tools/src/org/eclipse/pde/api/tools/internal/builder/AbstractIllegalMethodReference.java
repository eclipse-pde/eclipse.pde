/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Abstract implementation for illegal method references i.e. method calls,
 * constructor invocation, etc
 *
 * @since 1.1
 */
public abstract class AbstractIllegalMethodReference extends AbstractProblemDetector {

	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.model.MethodKey} to
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor}
	 */
	private Map<MethodKey, IMethodDescriptor> fIllegalMethods = new HashMap<>();

	/**
	 * Map of
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor}
	 * to associated component IDs
	 */
	private Map<IMethodDescriptor, String> fMethodComponents = new HashMap<>();

	/**
	 * Adds the given type as not to be extended.
	 *
	 * @param type a type that is marked no extend
	 * @param componentId the component the type is located in
	 */
	void addIllegalMethod(IMethodDescriptor method, String componentId) {
		fIllegalMethods.put(new MethodKey(method.getEnclosingType().getQualifiedName(), method.getName(), method.getSignature(), true), method);
		fMethodComponents.put(method, componentId);
	}

	@Override
	public boolean considerReference(IReference reference) {
		MethodKey key = new MethodKey(reference.getReferencedTypeName(), reference.getReferencedMemberName(), reference.getReferencedSignature(), true);
		if (super.considerReference(reference) && fIllegalMethods.containsKey(key)) {
			retainReference(reference);
			return true;
		}
		if ((reference.getReferenceFlags() & IReference.F_DEFAULT_METHOD) > 0) {
			IApiMember member = reference.getResolvedReference();
			if (member == null) {
				try {
					((Reference) reference).resolve();
					member = reference.getResolvedReference();
				} catch (CoreException ce) {
					// do nothing, skip it
				}
				if (member instanceof IApiMethod) {
					IApiMethod method = (IApiMethod) member;
					if (method.isDefaultMethod()) {
						return considerReference(reference);
					}
				}
			}
		}
		return false;
	}

	@Override
	protected boolean isProblem(IReference reference) {
		if (!super.isProblem(reference)) {
			return false;
		}
		IApiMember method = reference.getResolvedReference();
		String componentId = fMethodComponents.get(method.getHandle());
		// TODO: would it be faster to store component objects and use identity
		// instead of equals?
		return isReferenceFromComponent(reference, componentId);
	}

	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.METHOD;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		if (method.isConstructor()) {
			return IApiProblem.CONSTRUCTOR_METHOD;
		}
		return IApiProblem.METHOD;
	}

}
