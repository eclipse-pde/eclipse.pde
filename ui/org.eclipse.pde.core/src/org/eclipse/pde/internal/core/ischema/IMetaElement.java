/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Objects that implement this interface store metadata about extension point
 * schema elements. This metadata is stored as schema element annotation.
 */
public interface IMetaElement {
	/**
	 * Property that indicates if an element has translatable content
	 */
	public static final String P_TRANSLATABLE = "translatable"; //$NON-NLS-1$

	/**
	 * Property that indicates if an element is deprecated
	 */
	public static final String P_DEPRECATED = "deprecated"; //$NON-NLS-1$

	/**
	 * Returns a property (attribute) name whose value should be used to load
	 * element icon in the UI. For example, if icon property is set to "icon"
	 * and the element has an "icon" attribute that represents icon path
	 * relative to the plug-in, an attempt will be made to load that icon.
	 */
	public String getIconProperty();

	/**
	 * Returns a property (attribute) name whose value should be used to
	 * represent this element in the UI. For example, if this value is
	 * <samp>null </samp> and the name of the element is "wizard", that will be
	 * showing in the UI. However, if label property is set to "name" and the
	 * element has a "name" attribute whose value is "Import Wizard", that value
	 * will be used in the UI instead of "wizard".
	 */
	public String getLabelProperty();

	/**
	 * Returns <samp>true</samp> if the element content is translatable; <samp>false</samp> otherwise.
	 */
	public boolean hasTranslatableContent();

	/**
	 * Returns <samp>true</samp> if the element is deprecated; <samp>false</samp> otherwise.
	 */
	public boolean isDeprecated();
}
