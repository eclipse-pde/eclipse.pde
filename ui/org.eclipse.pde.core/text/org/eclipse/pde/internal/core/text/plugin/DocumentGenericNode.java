/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.pde.internal.core.text.DocumentElementNode;

/**
 * DocumentGenericNode
 *
 */
public class DocumentGenericNode extends DocumentElementNode {

	private static final long serialVersionUID = 1L;

	/**
	 * @param name
	 */
	public DocumentGenericNode(String name) {
		// NO-OP
		// Used just for generic element type
		setXMLTagName(name);
	}

}
