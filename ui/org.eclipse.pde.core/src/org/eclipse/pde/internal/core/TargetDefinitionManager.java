/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class TargetDefinitionManager implements IRegistryChangeListener {

	Map<String, IConfigurationElement> fTargets;
	private static String[] attributes;
	{
		attributes = new String[] {"id", "name"}; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		for (IExtensionDelta delta : deltas) {
			IExtension extension = delta.getExtension();
			String extensionId = extension.getExtensionPointUniqueIdentifier();
			if (extensionId.equals("org.eclipse.pde.core.targets")) { //$NON-NLS-1$
				IConfigurationElement[] elems = extension.getConfigurationElements();
				if (delta.getKind() == IExtensionDelta.ADDED) {
					add(elems);
				} else {
					remove(elems);
				}
			}
		}
	}

	public IConfigurationElement[] getTargets() {
		if (fTargets == null) {
			loadElements();
		}
		return fTargets.values().toArray(new IConfigurationElement[fTargets.size()]);
	}

	public IConfigurationElement[] getSortedTargets() {
		if (fTargets == null) {
			loadElements();
		}
		IConfigurationElement[] result = fTargets.values().toArray(new IConfigurationElement[fTargets.size()]);
		Arrays.sort(result, new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				String value1 = getString((IConfigurationElement) o1);
				String value2 = getString((IConfigurationElement) o2);
				return value1.compareTo(value2);
			}

			private String getString(IConfigurationElement elem) {
				String name = elem.getAttribute("name"); //$NON-NLS-1$
				String id = elem.getAttribute("id"); //$NON-NLS-1$
				name = name + " [" + id + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				return name;
			}

		});
		return result;
	}

	public IConfigurationElement getTarget(String id) {
		if (fTargets == null) {
			loadElements();
		}
		return fTargets.get(id);
	}

	private void loadElements() {
		fTargets = new LinkedHashMap<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.addRegistryChangeListener(this);
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.core.targets"); //$NON-NLS-1$
		add(elements);
	}

	private boolean isValid(IConfigurationElement elem) {
		String value;
		for (String attribute : attributes) {
			value = elem.getAttribute(attribute);
			if (value == null || value.equals("")) { //$NON-NLS-1$
				return false;
			}
		}
		value = elem.getAttribute("definition"); //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getContributor().getName();
		URL url = getResourceURL(symbolicName, value);
		try {
			if (url != null && url.openStream().available() > 0) {
				return true;
			}
		} catch (IOException e) {
			// file does not exist
		}
		return false;
	}

	public static URL getResourceURL(String bundleID, String resourcePath) {
		try {
			Bundle bundle = Platform.getBundle(bundleID);
			if (bundle != null && resourcePath != null) {
				URL entry = bundle.getEntry(resourcePath);
				if (entry != null) {
					return FileLocator.toFileURL(entry);
				}
			}
		} catch (IOException e) {
		}
		return null;
	}

	private void add(IConfigurationElement[] elems) {
		for (IConfigurationElement elem : elems) {
			if (isValid(elem)) {
				String id = elem.getAttribute("id"); //$NON-NLS-1$
				fTargets.put(id, elem);
			}
		}
	}

	private void remove(IConfigurationElement[] elems) {
		for (IConfigurationElement elem : elems) {
			fTargets.remove(elem.getAttribute("id")); //$NON-NLS-1$
		}
	}

	public void shutdown() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.removeRegistryChangeListener(this);
	}

}
