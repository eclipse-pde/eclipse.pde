/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
 * Class containing constants and utility methods for visibility modifiers
 * 
 * @since 1.0.0
 */
public class VisibilityModifiers {
	
	/**
	 * Visibility constant indicating an element is public API.
	 */
	public static final int API = 0x0001;
	/**
	 * Visibility constant indicating a element should not be referenced. This 
	 * indicates the element is internal and not intended for general use.
	 */
	public static final int PRIVATE = 0x0002;
	/**
	 * Visibility constant indicating an element is public for a specific
	 * audience (service provider interface).
	 */
	public static final int SPI = 0x0004;
	
	/**
	 * Visibility constant indicating an element is private, but some
	 * clients have been permitted access to the element.
	 */
	public static final int PRIVATE_PERMISSIBLE = 0x0008;
	
	/**
	 * Bit mask of all visibilities.
	 */
	public static final int ALL_VISIBILITIES = 0xFFFF;
	
	/**
	 * Returns if the modifier is 'API'
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'API'
	 */
	public static final boolean isAPI(int modifiers) {
		return (modifiers & API) > 0;
	}
	
	/**
	 * Returns if the modifier is 'SPI'
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'SPI'
	 */
	public static final boolean isSPI(int modifiers) {
		return (modifiers & SPI) > 0;
	}
	
	/**
	 * Returns if the modifier is 'Private'
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'Private'
	 */
	public static final boolean isPrivate(int modifiers) {
		return (modifiers & PRIVATE) > 0;
	}
	
	/**
	 * Returns if the modifier is 'Private Permissible'
	 * @param modifiers the modifiers to resolve
	 * @return if the modifier is 'Private Permissible'
	 */
	public static final boolean isPermissiblePrivate(int modifiers) {
		return (modifiers & PRIVATE_PERMISSIBLE) > 0;
	}	
}
