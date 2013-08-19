/*******************************************************************************
 * Copyright (c) Aug 13, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A collection of utility methods to help with handling {@link org.eclipse.jdt.core.IJavaElement}s
 * and {@link org.eclipse.jdt.core.dom.AST} elements
 * 
 * @since 1.0.600
 */
public class JavaUtils {

	private JavaUtils() {}

	/**
	 * Returns if the given {@link IJavaElement} is externally visible 
	 * 
	 * @param element
	 * @return <code>true</code> if the given element is visible <code>false</code> otherwise
	 * @throws JavaModelException if a model lookup fails
	 */
	public static final boolean isVisible(IJavaElement element) throws JavaModelException {
		if(element != null) {
			switch(element.getElementType()) {
				case IJavaElement.FIELD:
				case IJavaElement.METHOD: {
					IMember member = (IMember) element;
					int flags = member.getFlags();
					IType type = member.getDeclaringType();
					if(Flags.isPublic(flags) || Flags.isProtected(flags) || (type != null && type.isInterface())) {
						return isVisible(type);
					}
					break;
				}
				case IJavaElement.TYPE: {
					IType type = (IType) element;
					int flags = type.getFlags();
					if(type.isLocal() && !type.isAnonymous()) {
						return false;
					}
					if(type.isMember()) {
						if((Flags.isPublic(flags) && Flags.isStatic(flags)) || type.isInterface()) {
							return isVisible(type.getDeclaringType());
						}
					}
					else {
						return Flags.isPublic(flags) || type.isInterface();
					}
					break;
				}
				default: {
					break;
				}
					
			}
		}
		return false;
	}
}
