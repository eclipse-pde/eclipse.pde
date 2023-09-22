/*******************************************************************************
 *  Copyright (c) 2018, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

class MultiBundleClassLoader extends ClassLoader {
	private final List<Bundle> bundleList;

	public MultiBundleClassLoader(List<Bundle> platformEngineBundles) {
		super(null); // never delegate to system classloader, only load classes via given Bundles
		this.bundleList = platformEngineBundles;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		for (Bundle temp : bundleList) {
			try {
				Class<?> c = temp.loadClass(name);
				if (c != null) {
					return c;
				}
			} catch (ClassNotFoundException e) {
			}
		}
		throw new ClassNotFoundException(name);
	}

	@Override
	protected URL findResource(String name) {
		for (Bundle temp : bundleList) {
			URL url = temp.getResource(name);
			if (url != null) {
				try {
					return FileLocator.resolve(url);
				} catch (IOException e) {
					return null;
				}
			}
		}
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> merged = new ArrayList<>();
		for (Bundle bundle : bundleList) {
			Enumeration<URL> resources = bundle.getResources(name);
			while (resources != null && resources.hasMoreElements()) {
				merged.add(FileLocator.resolve(resources.nextElement()));
			}
		}
		return Collections.enumeration(merged);
	}
}