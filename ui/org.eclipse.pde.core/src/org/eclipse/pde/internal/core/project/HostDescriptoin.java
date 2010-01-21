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
import org.eclipse.pde.core.project.IHostDescription;

/**
 * Describes a host
 */
public class HostDescriptoin extends RequirementSpecification implements IHostDescription {

	/**
	 * Constructs a host description.
	 * 
	 * @param name
	 * @param range
	 */
	public HostDescriptoin(String name, VersionRange range) {
		super(name, range, false, false);
	}

}
