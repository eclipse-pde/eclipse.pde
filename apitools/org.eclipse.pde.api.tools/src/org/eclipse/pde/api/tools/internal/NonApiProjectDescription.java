/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * An API description for a project that does not have an API Tools nature.
 * Keeps track of package visibility.
 *
 * @since 1.1
 */
public class NonApiProjectDescription extends ProjectApiDescription {

	/**
	 * Constructs API description for the given project.
	 *
	 * @param project
	 */
	public NonApiProjectDescription(IJavaProject project) {
		super(project);
	}

	@Override
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		switch (elementDescriptor.getElementType()) {
			case IElementDescriptor.PACKAGE:
				return true;
			default:
				return false;
		}
	}

}
