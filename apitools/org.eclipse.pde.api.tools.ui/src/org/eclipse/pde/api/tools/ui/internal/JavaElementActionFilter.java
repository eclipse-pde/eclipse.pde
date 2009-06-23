/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.ui.IActionFilter;

/**
 * Define an action filter that can be used on IJavaElement.
 */
public class JavaElementActionFilter implements IActionFilter {

	/**
	 * @see org.eclipse.ui.IActionFilter#testAttribute(Object, String, String)
	 */
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("JavaElementActionFilter")) { //$NON-NLS-1$
			if (target instanceof IJavaElement) {
				IJavaElement javaElement = (IJavaElement) target;
				if (value.equals("isEnabled")) { //$NON-NLS-1$
					while (javaElement != null) {
						switch(javaElement.getElementType()) {
							case IJavaElement.PACKAGE_FRAGMENT_ROOT :
								IPackageFragmentRoot root = (IPackageFragmentRoot) javaElement;
								return !root.isArchive();
							case IJavaElement.PACKAGE_FRAGMENT :
							case IJavaElement.COMPILATION_UNIT :
							case IJavaElement.CLASS_FILE :
							case IJavaElement.TYPE :								
								javaElement = javaElement.getParent();
								break;
							case IJavaElement.ANNOTATION :
							case IJavaElement.FIELD :
							case IJavaElement.IMPORT_CONTAINER :
							case IJavaElement.IMPORT_DECLARATION :
							case IJavaElement.INITIALIZER :
							case IJavaElement.JAVA_MODEL :
							case IJavaElement.LOCAL_VARIABLE :
							case IJavaElement.METHOD :
							case IJavaElement.PACKAGE_DECLARATION :
							case IJavaElement.TYPE_PARAMETER :
								return false;
							case IJavaElement.JAVA_PROJECT :
								return true;
						}
					}
					return true;
				}
			}
		}
		return false;
	}
}
