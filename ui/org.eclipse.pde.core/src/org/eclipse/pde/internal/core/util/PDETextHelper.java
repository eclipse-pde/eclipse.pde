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

package org.eclipse.pde.internal.core.util;


/**
 * PDETextHelper
 *
 */
public class PDETextHelper {

	public static final String dots = "..."; //$NON-NLS-1$
	
	/**
	 * @param text
	 * @return
	 */
	public static String truncateAndTrailOffText(String text, int limit) {
		String trimmed = text.trim();
		int dotsLength = dots.length();
		int trimmedLength = trimmed.length();
		int limitWithDots = limit - dotsLength;
		
		if (limit >= trimmedLength) {
			return trimmed;
		}
		// limit <= trimmedLength
		if (limit <= dotsLength) {
			return ""; //$NON-NLS-1$
		}
		// dotsLength < limit < trimmedLength
		return trimmed.substring(0, limitWithDots) + dots;
	}
	
	/**
	 * @param text
	 * @return
	 */
	public static boolean isDefined(String text) {
		if ((text == null) || 
				(text.length() == 0)) {
			return false;
		}
		return true;
	}
	
}
