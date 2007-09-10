/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Aniszczyk <zx@us.ibm.com> - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.runtime.spy;

import java.util.Map;

import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.osgi.framework.Bundle;

public class SpyRCPUtil {

	public static String generateClassString(String title, Map bundleByClassName, Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<p>"); //$NON-NLS-1$
		buffer.append(title);
		buffer.append("</p>"); //$NON-NLS-1$
		buffer.append(generateClassString(bundleByClassName, clazz));
		return buffer.toString();
	}

	public static String generateInterfaceString(Map bundleByClassName, Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<li bindent=\"20\" style=\"image\" value=\"interface\">"); //$NON-NLS-1$

		Bundle bundle = PDERuntimePlugin.HAS_IDE_BUNDLES ? 
				PDERuntimePlugin.getDefault().getPackageAdmin().getBundle(
						clazz) : null;
						if (bundle != null) {
							bundleByClassName.put(clazz.getName(),
									bundle);
							buffer.append("<a href=\"").append( //$NON-NLS-1$
									clazz.getName()).append("\">") //$NON-NLS-1$
									.append(getSimpleName(clazz)).append(
									"</a>"); //$NON-NLS-1$
						} else {
							buffer.append(clazz.getName());
						}

						buffer.append("</li>"); //$NON-NLS-1$
						return buffer.toString();
	}
	
	public static String generateClassString(Map bundleByClassName, Class clazz) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<li bindent=\"20\" style=\"image\" value=\"class\">"); //$NON-NLS-1$

		Bundle bundle = PDERuntimePlugin.HAS_IDE_BUNDLES ? 
				PDERuntimePlugin.getDefault().getPackageAdmin().getBundle(
						clazz) : null;
						if (bundle != null) {
							bundleByClassName.put(clazz.getName(),
									bundle);
							buffer.append("<a href=\"").append( //$NON-NLS-1$
									clazz.getName()).append("\">") //$NON-NLS-1$
									.append(getSimpleName(clazz)).append(
									"</a>"); //$NON-NLS-1$
						} else {
							buffer.append(clazz.getName());
						}

						buffer.append("</li>"); //$NON-NLS-1$
						return buffer.toString();
	}

	private static String getSimpleName(Class clazz) {
		String fullName = clazz.getName();
		int index = fullName.lastIndexOf('.');
		String name = fullName.substring(index + 1, fullName.length());
		if(name != null)
			return name;
		return fullName;
	}

}
