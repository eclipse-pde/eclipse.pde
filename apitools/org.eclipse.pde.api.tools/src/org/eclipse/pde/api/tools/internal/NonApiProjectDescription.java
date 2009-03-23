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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.model.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * An API description for a project that does not have an API tooling nature.
 * Keeps track of package visibility.
 * 
 * @since 1.1
 */
public class NonApiProjectDescription extends ProjectApiDescription {

	/**
	 * Constructs API description for the given project.
	 * 
	 * @param component
	 */
	public NonApiProjectDescription(PluginProjectApiComponent component) {
		super(component);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.api.tools.internal.model.ProjectApiDescription#isInsertOnResolve(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		switch (elementDescriptor.getElementType()) {
			case IElementDescriptor.PACKAGE:
				return true;
			default:
				return false;
		}
	}

}
