/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * This capture the context of an <code>@Activate</code> annotated method or
 * field binding
 */
record ComponentActivationAnnotation(String activate, Annotation annotation, MethodDeclaration method,
		IBinding binding) {

	public boolean isMethod() {
		if (binding instanceof IMethodBinding method) {
			return !method.isConstructor();
		}
		return false;
	}

	public boolean isConstructor() {
		if (binding instanceof IMethodBinding method) {
			return method.isConstructor();
		}
		return false;
	}

	public boolean isType() {
		return binding instanceof ITypeBinding;
	}

	public int parameterCount() {
		if (binding instanceof IMethodBinding method) {
			return method.getParameterNames().length;
		}
		return 0;
	}

}
