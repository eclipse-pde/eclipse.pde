/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************/
package org.eclipse.pde.internal.build.properties;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.ant.core.IAntPropertyValueProvider;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;

public class PDEProperties implements IAntPropertyValueProvider {
	static private final String PREFIX = "eclipse.pdebuild"; //$NON-NLS-1$
	static private final String HOME = PREFIX + ".home"; //$NON-NLS-1$
	static private final String SCRIPTS = PREFIX + ".scripts"; //$NON-NLS-1$
	static private final String TEMPLATES = PREFIX + ".templates"; //$NON-NLS-1$
	static private final Map cache = new HashMap();

	public String getAntPropertyValue(String antPropertyName) {
		String searchedEntry = null;
		if (HOME.equals(antPropertyName))
			searchedEntry = "."; //$NON-NLS-1$

		if (SCRIPTS.equals(antPropertyName))
			searchedEntry = "scripts"; //$NON-NLS-1$

		if (TEMPLATES.equals(antPropertyName))
			searchedEntry = "templates"; //$NON-NLS-1$

		if (searchedEntry == null)
			return null; //TODO Throw an exception or log an error

		try {
			String result = (String) cache.get(searchedEntry);
			if (result == null) {
				URL foundEntry = Platform.getBundle(IPDEBuildConstants.PI_PDEBUILD).getEntry(searchedEntry);
				if (foundEntry == null) {
					BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.WARNING_PLUGIN_ALTERED, NLS.bind(Messages.exception_missing_pdebuild_folder, antPropertyName), null));
				} else {
					result = FileLocator.toFileURL(foundEntry).getPath();
					cache.put(searchedEntry, result);
				}
			}
			return result;
		} catch (IOException e) {
			return null;
		}

	}

}
