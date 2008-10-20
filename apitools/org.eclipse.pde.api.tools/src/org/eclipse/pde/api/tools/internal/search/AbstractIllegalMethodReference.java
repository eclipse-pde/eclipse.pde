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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.api.tools.internal.model.cache.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;


/**
 * @since 1.1
 */
public abstract class AbstractIllegalMethodReference extends AbstractProblemDetector {

	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.model.MethodKey} to
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor} 
	 */
	private Map fIllegalMethods = new HashMap();
	
	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor}
	 * to associated component IDs
	 */
	private Map fMethodComponents = new HashMap();
	
	/**
	 * Adds the given type as not to be extended.
	 * 
	 * @param type a type that is marked no extend
	 * @param componentId the component the type is located in
	 */
	void addIllegalMethod(IMethodDescriptor method, String componentId) {
		fIllegalMethods.put(new MethodKey(method.getName(), method.getSignature()), method);
		fMethodComponents.put(method, componentId);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#considerReference(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	public boolean considerReference(IReference reference) {
		if (fIllegalMethods.containsKey(
				new MethodKey(reference.getReferencedMemberName(), reference.getReferencedSignature()))) {
			retainReference(reference);
			return true;
		}
		return false;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		IApiMember method = reference.getResolvedReference();
		String componentId = (String) fMethodComponents.get(method.getHandle());
		// TODO: would it be faster to store component objects and use identity instead of equals?
		return componentId != null && method.getApiComponent().getId().equals(componentId);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getElementType(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getElementType(IReference reference) {
		return IElementDescriptor.T_METHOD;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		if (method.isConstructor()) {
			return IApiProblem.CONSTRUCTOR_METHOD;
		}
		return IApiProblem.METHOD;
	}
	
}
