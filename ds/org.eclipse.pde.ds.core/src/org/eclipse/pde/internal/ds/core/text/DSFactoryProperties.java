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

import org.eclipse.pde.internal.ds.core.IDSFactoryProperties;

/**
 * Represents a set of factory properties from a bundle entry
 *
 * @since 3.4
 * @see DSObject
 * @see DSComponent
 * @see DSModel
 */
public class DSFactoryProperties extends DSEntryProperties implements IDSFactoryProperties {


	private static final long serialVersionUID = 1L;

	public DSFactoryProperties(DSModel model) {
		super(model, ELEMENT_FACTORY_PROPERTIES, TYPE_FACTORY_PROPERTIES);
	}

}
