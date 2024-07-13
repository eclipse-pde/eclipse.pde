/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.project;

import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.osgi.framework.VersionRange;

/**
 * Describes a required bundle.
 *
 * @since 3.6
 */
public class RequiredBundleDescription extends RequirementSpecification implements IRequiredBundleDescription {

	/**
	 * Constructs a required bundle description.
	 */
	public RequiredBundleDescription(String name, VersionRange range, boolean reexport, boolean optional) {
		super(name, range, reexport, optional);
	}

}
