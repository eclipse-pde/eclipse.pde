/*******************************************************************************
 * Copyright (c) 2015, 2016 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.awt.Component;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.ds.core.IDSConstants;

/**
 * Tests if a type (or any nested type) has a {@link Component} annotation with
 * no <code>name</code> attribute.
 */
@SuppressWarnings("restriction")
public class ComponentPropertyTester extends PropertyTester {

	private static final String COMPONENT_ANNOTATION = DSAnnotationCompilationParticipant.COMPONENT_ANNOTATION;

	private static final String COMPONENT_PACKAGE = DSAnnotationCompilationParticipant.ANNOTATIONS_PACKAGE;

	private static final String COMPONENT_NAME = "Component"; //$NON-NLS-1$

	private static final Debug debug = Debug.getDebug("component-property-tester"); //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!"containsComponentWithImplicitName".equals(property)) //$NON-NLS-1$
			return false;

		if (!(receiver instanceof IType) && !(receiver instanceof IPackageFragment))
			return false;

		IJavaElement element = (IJavaElement) receiver;
		IJavaProject javaProject = element.getJavaProject();

		boolean enabled = Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID, Activator.PREF_ENABLED, false, new IScopeContext[] { new ProjectScope(javaProject.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE });
		if (!enabled)
			return false;

		try {
			return element.getElementType() == IJavaElement.TYPE ? containsImplicitName((IType) receiver) : containsImplicitName((IPackageFragment) receiver);
		} catch (JavaModelException e) {
			if (debug.isDebugging())
				debug.trace(String.format("Error searching for components with implicit names in element: %s", element), e); //$NON-NLS-1$
		}

		return false;
	}

	private boolean containsImplicitName(IPackageFragment fragment) throws JavaModelException {
		if (!fragment.containsJavaResources())
			return false;

		for (ICompilationUnit cu : fragment.getCompilationUnits()) {
			for (IType type : cu.getAllTypes()) {
				if (hasImplicitName(type))
					return true;
			}
		}

		return false;
	}

	private boolean containsImplicitName(IType type) throws JavaModelException {
		if (hasImplicitName(type))
			return true;

		for (IType child : type.getTypes()) {
			if (hasImplicitName(child))
				return true;
		}

		return false;
	}

	public static boolean hasImplicitName(IType type) throws JavaModelException {
		IAnnotation[] annotations = type.getAnnotations();
		for (IAnnotation annotation : annotations) {
			boolean isComponent = COMPONENT_ANNOTATION.equals(annotation.getElementName());
			if (!isComponent) {
				String[][] resolved = type.resolveType(annotation.getElementName());
				if (resolved != null) {
					for (String[] pair : resolved) {
						if (pair.length == 2
								&& COMPONENT_PACKAGE.equals(pair[0])
								&& COMPONENT_NAME.equals(pair[1])) {
							isComponent = true;
							break;
						}
					}
				}
			}

			if (isComponent) {
				IMemberValuePair[] attrs = annotation.getMemberValuePairs();
				for (IMemberValuePair attr : attrs) {
					if (IDSConstants.ATTRIBUTE_COMPONENT_NAME.equals(attr.getMemberName())) {
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}
}
