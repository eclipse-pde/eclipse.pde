/*******************************************************************************
 *  Copyright (c) 2018 IBM Corporation and others.
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
import java.util.*;
import org.osgi.framework.Bundle;

class MultiBundleClassLoader2 extends ClassLoader {
	private List<Bundle> bundleList;

	public MultiBundleClassLoader2(List<Bundle> platformEngineBundles) {
		this.bundleList = platformEngineBundles;

	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> c = null;
		for (Bundle temp : bundleList) {
			try {
				c = temp.loadClass(name);
				if (c != null)
					return c;
			} catch (ClassNotFoundException e) {
			}
		}
		return c;
	}

	@Override
	protected URL findResource(String name) {
		URL url = null;
		for (Bundle temp : bundleList) {
			url = temp.getResource(name);
			if (url != null)
				return url;
		}
		return url;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		Enumeration<URL> enumFinal = null;
		for (int i = 0; i < bundleList.size(); i++) {
			if (i == 0) {
				enumFinal = bundleList.get(i).getResources(name);
				continue;
			}
			Enumeration<URL> e2 = bundleList.get(i).getResources(name);
			Vector<URL> temp = new Vector<>();
			while (enumFinal != null && enumFinal.hasMoreElements()) {
				temp.add(enumFinal.nextElement());
			}
			while (e2 != null && e2.hasMoreElements()) {
				temp.add(e2.nextElement());
			}
			enumFinal = temp.elements();
		}
		return enumFinal;
	}
}