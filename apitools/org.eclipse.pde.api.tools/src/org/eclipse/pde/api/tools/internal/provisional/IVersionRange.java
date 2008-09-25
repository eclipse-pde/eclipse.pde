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
 * Describes an interval or range of versions.
 * 
 * @since 1.0.0
 */
public interface IVersionRange {
	
	/**
	 * Returns the minimum version in this range.
	 * 
	 * @return minimum version 
	 */
	public String getMinimumVersion();
	
	/**
	 * Returns whether the minimum version is included in the range.
	 * 
	 * @return whether the minimum version is included in the range
	 */
	public boolean isIncludeMinimum();
	
	/**
	 * Returns the maximum version in this range.
	 * 
	 * @return maximum version 
	 */	
	public String getMaximumVersion();
	
	/**
	 * Returns whether the maximum version is included in the range.
	 * 
	 * @return whether the maximum version is included in the range
	 */
	public boolean isIncludeMaximum();	
	
	/**
	 * Returns whether the given version is included in this range.
	 * 
	 * @param version version identifier
	 * @return whether included in this version range
	 */
	public boolean isIncluded(String version);

}
