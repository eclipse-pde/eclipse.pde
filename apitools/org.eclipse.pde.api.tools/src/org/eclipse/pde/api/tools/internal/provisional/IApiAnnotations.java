/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;


/**
 * Describes API usage restrictions, visibility and profiles information of an element contained
 * in an API component. 
 *   
 * @since 1.0.0
 */
public interface IApiAnnotations {
	
	/**
	 * Returns the visibility modifiers annotation.
	 * 
	 * @return a visibility constant defined by {@link VisibilityModifiers} 
	 */
	public int getVisibility();
	
	/**
	 * Returns the restriction modifiers annotation.
	 * 
	 * @return restriction constant defined by {@link RestrictionModifiers}
	 */
	public int getRestrictions();
	
}
