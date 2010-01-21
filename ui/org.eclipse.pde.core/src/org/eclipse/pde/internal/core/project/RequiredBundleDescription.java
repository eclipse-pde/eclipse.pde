/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.project.IRequiredBundleDescription;

/**
 * Describes a required bundle.
 * 
 * @since 3.6
 */
public class RequiredBundleDescription extends RequirementSpecification implements IRequiredBundleDescription {

	/**
	 * Constructs a required bundle description.
	 * 
	 * @param name
	 * @param range
	 * @param reexport
	 * @param optional
	 */
	public RequiredBundleDescription(String name, VersionRange range, boolean reexport, boolean optional) {
		super(name, range, reexport, optional);
	}

}
