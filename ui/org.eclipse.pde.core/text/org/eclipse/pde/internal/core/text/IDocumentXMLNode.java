/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
