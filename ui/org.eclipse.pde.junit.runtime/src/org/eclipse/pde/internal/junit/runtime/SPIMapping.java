/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *
 */
package org.eclipse.pde.internal.junit.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.framework.Bundle;

final class SPIMapping {

	private final Class<?> serviceClass;
	private final Bundle bundle;
	private final Set<String> classes;
	private final Collection<URL> urls;

	SPIMapping(Class<?> serviceClass, Bundle bundle, Collection<URL> urls) {
		this.serviceClass = serviceClass;
		this.bundle = bundle;
		this.urls = urls;
		this.classes = readClasses(urls);
	}

	private static Set<String> readClasses(Collection<URL> urls) {
		Set<String> result = new LinkedHashSet<>();
		for (URL url : urls) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				reader.lines().forEach(result::add);
			} catch (IOException e) {
			}
		}
		return result;
	}


	boolean isCompatible(Bundle other) {
		try {
			return other.loadClass(serviceClass.getName()) == serviceClass;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	Collection<URL> getUrls() {
		return urls;
	}

	boolean hasService(String implementation) {
		return classes != null && classes.contains(implementation);
	}

	Class<?> loadImplementation(String name) throws ClassNotFoundException {
		return bundle.loadClass(name);
	}

}