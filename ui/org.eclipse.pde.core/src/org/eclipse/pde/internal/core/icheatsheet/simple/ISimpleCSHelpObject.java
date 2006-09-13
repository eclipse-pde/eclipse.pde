/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.icheatsheet.simple;

/**
 * ISimpleCSHelpObject
 *
 */
public interface ISimpleCSHelpObject extends ISimpleCSObject {

	/**
	 * Attribute:  contextId
	 * @return
	 */
	public String getContextId();
	
	/**
	 * Attribute:  contextId
	 * @param contextId
	 */
	public void setContextId(String contextId);
	
	/**
	 * Attribute:  href
	 * @return
	 */
	public String getHref();
	
	/**
	 * Attribute:  href
	 * @param href
	 */
	public void setHref(String href);	
	
}
