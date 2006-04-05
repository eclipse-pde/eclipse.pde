/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.util.StringTokenizer;

import org.eclipse.osgi.util.NLS;

public class IdUtil {
	public static boolean isValidCompositeID(String name) {
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c)
					&& (c < '0' || '9' < c) && c != '_') {
				if (i == 0 || i == name.length() - 1 || c != '.') {
					return false;
				}
			}
		}
		return true;
	}

    public static boolean isValidSimpleID(String name) {
        if (name.length() <= 0) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c)
                    && (c < '0' || '9' < c) && c != '_') {
                return false;
            }
        }
        return true;
    }
    
    public static String getValidId(String projectName) {
    	return projectName.replaceAll("[^a-zA-Z0-9\\._]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /*
     * nameFieldQualifier must contain a placeholder variable
     * ie. {0} Plug-in
     */
    public static String getValidName(String id, String nameFieldQualifier) {
		StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			if (!tok.hasMoreTokens()) {
				String name = Character.toUpperCase(token.charAt(0))
							+ ((token.length() > 1) ? token.substring(1) : ""); //$NON-NLS-1$
				return NLS.bind(nameFieldQualifier, name);
			}
		}
		return ""; //$NON-NLS-1$
    }
    
    public static String getValidProvider(String id) {
    	StringTokenizer tok = new StringTokenizer(id, "."); //$NON-NLS-1$
		int count = tok.countTokens();
		if (count > 2 && tok.nextToken().equals("com")) //$NON-NLS-1$
			return tok.nextToken().toUpperCase();
		return ""; //$NON-NLS-1$
    }
}
