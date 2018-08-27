/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

/**
 * IDocumentXML
 *
 */
public interface IDocumentXMLNode {

	public static final int F_TYPE_ELEMENT = 0;

	public static final int F_TYPE_ATTRIBUTE = 1;

	public static final int F_TYPE_TEXT = 2;

	public int getXMLType();

}
