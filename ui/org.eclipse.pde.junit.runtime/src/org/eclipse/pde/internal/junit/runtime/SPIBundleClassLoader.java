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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

/**
 * The classloader wraps the OSGi provided one but gives access for the JUnit
 * runer to any SPI declared services.
 */
public class SPIBundleClassLoader extends ClassLoader {

	private static final String META_INF_SERVICES = "META-INF/services/"; //$NON-NLS-1$
	private final List<Bundle> bundles;
	private final int junitVersion;
	private final Map<String, List<SPIMapping>> mappings = new ConcurrentHashMap<>();

	public SPIBundleClassLoader(List<Bundle> bundles, int junitVersion) {
		super(null);
		this.bundles = bundles;
		this.junitVersion = junitVersion;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Iterator<SPIMapping> spi = mappings.values().stream().flatMap(Collection::stream).filter(mapping -> mapping.hasService(name)).iterator();
		if (spi.hasNext()) {
			Bundle caller = Caller.getBundle(junitVersion);
			while (spi.hasNext()) {
				SPIMapping mapping = spi.next();
				if (mapping.isCompatible(caller)) {
					return mapping.loadImplementation(name);
				}
			}
			throw new ClassNotFoundException(name);
		}
		for (Bundle bundle : bundles) {
			try {
				Class<?> c = bundle.loadClass(name);
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
		try {
			Enumeration<URL> resources = findResources(name);
			if (resources.hasMoreElements()) {
				return resources.nextElement();
			}
		} catch (IOException e) {
		}
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> result = new ArrayList<>();
		if (name.startsWith(META_INF_SERVICES)) {
			String serviceName = name.substring(META_INF_SERVICES.length());
			List<SPIMapping> spis = mappings.computeIfAbsent(name, spi -> {
				List<SPIMapping> list = new ArrayList<>();
				for (Bundle other : bundles) {
					URL entry = other.getEntry(name);
					if (entry != null) {
						try {
							list.add(new SPIMapping(other.loadClass(serviceName), other, entry));
						} catch (ClassNotFoundException e) {
							// should not happen
						}
					}
				}
				return list;
			});
			Bundle caller = Caller.getBundle(junitVersion);
			for (SPIMapping mapping : spis) {
				if (mapping.isCompatible(caller)) {
					result.add(mapping.getUrl());
				}
			}
			return Collections.enumeration(result);
		}
		for (Bundle bundle : bundles) {
			Enumeration<URL> resources = bundle.getResources(name);
			while (resources != null && resources.hasMoreElements()) {
				result.add(FileLocator.resolve(resources.nextElement()));
			}
		}
		return Collections.enumeration(result);
	}

	@Override
	public String toString() {
		return "SPIBundleClassLoader for bundles " + bundles; //$NON-NLS-1$
	}

}
