/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * Class containing constants and utility methods for restriction modifiers
 * 
 * @since 1.0.0
 */
public class RestrictionModifiers {

	/**
	 * Restriction kind constant indicating there are no restrictions on a type
	 */
	public static final int NO_RESTRICTIONS = 0x0000;
	/**
	 * Restriction kind constant indicating an interface cannot be implemented
	 */
	public static final int NO_IMPLEMENT = 0x0100;
	/**
	 * Restriction kind constant indicating a type or member cannot be extended.
	 */
	public static final int NO_EXTEND = 0x0200;
	/**
	 * Restriction kind constant indicating a class cannot be instantiated.
	 */
	public static final int NO_INSTANTIATE = 0x0400;	
	/**
	 * Restriction kind constant indicating a class cannot have a member referenced
	 */
	public static final int NO_REFERENCE = 0x0800;
	
	/**
	 * Bit mask of all restrictions.
	 */
	public static final int ALL_RESTRICTIONS = 0xFFFF;
	
	/**
	 * Returns if the no_implement modifier has been set in the specified modifiers
	 * @param modifiers the modifiers to resolve
	 * @return if the no_implement modifier has been set in the specified modifiers
	 */
	public static final boolean isImplementRestriction(int modifiers) {
		return (modifiers & NO_IMPLEMENT) > 0;
	}
	
	/**
	 * Returns if the no_subclass modifier has been set in the specified modifiers
	 * @param modifiers the modifiers to resolve
	 * @return if the no_subclass modifier has been set in the specified modifiers
	 */
	public static final boolean isExtendRestriction(int modifiers) {
		return (modifiers & NO_EXTEND) > 0;
	}
	
	/**
	 * Returns if the no_instantiate modifier has been set in the specified modifiers
	 * @param modifiers the modifiers to resolve
	 * @return if the no_instantiate modifier has been set in the specified modifiers
	 */
	public static final boolean isInstantiateRestriction(int modifiers) {
		return (modifiers & NO_INSTANTIATE) > 0;
	}
	
	/**
	 * Returns if the no_reference modifier has been set in the specified modifiers
	 * @param modifiers the modifiers to resolve
	 * @return if the no_reference modifier has been set in the specified modifiers
	 */
	public static final boolean isReferenceRestriction(int modifiers) {
		return (modifiers & NO_REFERENCE) > 0;
	}
	
	/**
	 * Returns if the modifiers indicate no restrictions.
	 * @param modifiers the modifiers to test
	 * @return if the modifiers indicate no restrictions
	 */
	public static final boolean isUnrestricted(int modifiers) {
		return modifiers == NO_RESTRICTIONS;
	}
}
