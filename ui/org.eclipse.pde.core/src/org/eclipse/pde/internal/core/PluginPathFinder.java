/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;

public class PluginPathFinder {
	

	public static File[] getLinkFiles(String platformHome) {
		File file = new File(platformHome + Path.SEPARATOR + "links"); //$NON-NLS-1$
		File[] linkFiles = null;
		if (file.exists() && file.isDirectory()) {
			linkFiles = file.listFiles();
		}
		return linkFiles;
		
	}
	
	public static String getPath(String platformHome, File file) {
		String prefix = new Path(platformHome).removeLastSegments(1).toString();
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(file);
			properties.load(fis);
			fis.close();
			String path = properties.getProperty("path"); //$NON-NLS-1$
			if (path != null) {
				if (!new Path(path).isAbsolute())
					path = prefix + Path.SEPARATOR + path;
				path += Path.SEPARATOR + "eclipse" + Path.SEPARATOR + "plugins"; //$NON-NLS-1$ //$NON-NLS-2$
				if (new File(path).exists()) {
					return path;
				}
			}
		} catch (IOException e) {
		}
		return null;
	}
	
	public static String[] getPluginPaths(String platformHome) {
		ArrayList result = new ArrayList();
		result.add(new Path(platformHome).append("plugins").toOSString()); //$NON-NLS-1$
		File[] linkFiles = getLinkFiles(platformHome);		
		if (linkFiles != null) {
			for (int i = 0; i < linkFiles.length; i++) {
				String path = getPath(platformHome, linkFiles[i]);
				if (path != null)
					result.add(path);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	
}
