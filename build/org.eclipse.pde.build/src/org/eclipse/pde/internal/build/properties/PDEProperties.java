/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************/
package org.eclipse.pde.internal.build.properties;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.core.IAntPropertyValueProvider;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;

public class PDEProperties implements IAntPropertyValueProvider {
	static private final String PREFIX = "eclipse.pdebuild"; //$NON-NLS-1$
	static private final String HOME = PREFIX + ".home"; //$NON-NLS-1$
	static private final String SCRIPTS = PREFIX + ".scripts"; //$NON-NLS-1$
	static private final String TEMPLATES = PREFIX + ".templates"; //$NON-NLS-1$
	static private final Map<String, String> cache = new HashMap<>();

	@Override
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
			String result = cache.get(searchedEntry);
			if (result == null) {
				URL foundEntry = Platform.getBundle(IPDEBuildConstants.PI_PDEBUILD).getEntry(searchedEntry);
				if (foundEntry == null) {
					BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.WARNING_PLUGIN_ALTERED, NLS.bind(Messages.exception_missing_pdebuild_folder, antPropertyName), null));
				} else {
					try {
						URL fileURL = FileLocator.toFileURL(foundEntry);
						URI uri = URIUtil.toURI(fileURL);
						File file = URIUtil.toFile(uri);
						if (file == null) {
							BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.WARNING_PLUGIN_ALTERED, NLS.bind(Messages.exception_missing_pdebuild_folder, antPropertyName), null));
							return null;
						}
						result = file.getAbsolutePath();
					} catch (URISyntaxException e) {
						BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_MALFORMED_URL, e.getMessage(), e));
						return null;
					}
					cache.put(searchedEntry, result);
				}
			}
			return result;
		} catch (IOException e) {
			return null;
		}

	}

}
