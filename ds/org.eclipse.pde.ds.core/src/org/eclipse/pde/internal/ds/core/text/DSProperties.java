/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core.text;

import org.eclipse.pde.internal.ds.core.IDSProperties;

/**
 * Represents a set of properties from a bundle entry
 *
 * @since 3.4
 * @see DSObject
 * @see DSComponent
 * @see DSModel
 */
public class DSProperties extends DSEntryProperties implements IDSProperties {

	private static final long serialVersionUID = 1L;

	public DSProperties(DSModel model) {
		super(model, ELEMENT_PROPERTIES, TYPE_PROPERTIES);
	}

}
