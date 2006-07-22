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

package org.eclipse.pde.internal.ui.util;

/**
 * PDEHTMLHelper
 *
 */
public class PDEHTMLHelper {
	
	public static String stripTags(String html) {
		int length = html.length();
		boolean write = true;
		char oldChar = ' ';
		StringBuffer sb = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			char curr = html.charAt(i);
			if (curr == '<')
				write = false;
			else if (curr == '>')
				write = true;
			else if (write && curr != '\r' && curr != '\n' && curr != '\t')
				if (!(curr == ' ') || !(oldChar == curr)) { // skip multiple spaces
					sb.append(curr);
					oldChar = curr;
				}
		}
		return sb.toString();
	}	
}
