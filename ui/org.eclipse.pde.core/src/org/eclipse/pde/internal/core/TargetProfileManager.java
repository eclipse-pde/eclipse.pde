package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class TargetProfileManager implements IRegistryChangeListener{
	
	Map fTargets;
	private static String[] attributes;
	{
		attributes = new String[] {"id", "name" }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		for (int i = 0; i < deltas.length; i++) {
			IExtension extension = deltas[i].getExtension();
			String extensionId = extension.getExtensionPointUniqueIdentifier();
			if (extensionId.equals("org.eclipse.pde.core.targetProfiles")) { //$NON-NLS-1$
				IConfigurationElement[] elems = extension.getConfigurationElements();
				if (deltas[i].getKind() == IExtensionDelta.ADDED)
					add(elems);
				else
					remove(elems);
			}
		}
	}
	
	public IConfigurationElement[] getTargets() {
		if (fTargets == null)
			loadElements();
		return (IConfigurationElement[])fTargets.values().toArray(new IConfigurationElement[fTargets.size()]);
	}
	
	public IConfigurationElement[] getSortedTargets() {
		if (fTargets == null)
			loadElements();
		IConfigurationElement[] result = (IConfigurationElement[])fTargets.values().toArray(new IConfigurationElement[fTargets.size()]);
		Arrays.sort(result, new Comparator() {

			public int compare(Object o1, Object o2) {
				String value1 = getString((IConfigurationElement)o1);
				String value2 = getString((IConfigurationElement)o2);
				return value1.compareTo(value2);
			}
			
			private String getString(IConfigurationElement elem){
				String name = elem.getAttribute("name"); //$NON-NLS-1$
				String id = elem.getAttribute("id"); //$NON-NLS-1$
				name = name + " [" + id + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				return name;
			}
			
		});
		return result;
	}
	
	public IConfigurationElement getTarget(String id) {
		if (fTargets == null)
			loadElements();
		return (IConfigurationElement)fTargets.get(id);
	}
	
	private void loadElements() {
		fTargets = new HashMap();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.addRegistryChangeListener(this);
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.pde.core.targetProfiles"); //$NON-NLS-1$
		add(elements);
	}
	
	private boolean isValid (IConfigurationElement elem) {
		String value;
		for (int i = 0; i < attributes.length; i++) {
			value = elem.getAttribute(attributes[i]);
			if (value == null || value.equals("")) //$NON-NLS-1$
				return false;
		}
		value = elem.getAttribute("path"); //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getNamespace();
		URL url = getResourceURL(symbolicName, value);
		try {
			if (url != null && url.openStream().available() > 0)
				return true;
		} catch (IOException e) {
			// file does not exist
		}
		return false;
	}
	
	public static URL getResourceURL(String bundleID, String resourcePath) {
		try {
			Bundle bundle = Platform.getBundle(bundleID);
			if (bundle != null) {
				URL entry = bundle.getEntry(resourcePath);
				if (entry != null)
					return Platform.asLocalURL(entry);
			}
		} catch (IOException e) {
		}
		return null;
	}
	
	private void add(IConfigurationElement[] elems) {
		for (int i = 0; i < elems.length; i++) {
			IConfigurationElement elem = elems[i];
			if (isValid(elem)) {
				String id = elem.getAttribute("id"); //$NON-NLS-1$
				fTargets.put(id, elem);
			}
		}
	}
	
	private void remove(IConfigurationElement[] elems) {
		for (int i = 0 ; i < elems.length; i++) {
			fTargets.remove(elems[i].getAttribute("id")); //$NON-NLS-1$
		}
	}
	
	public void shutdown() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.removeRegistryChangeListener(this);
	}

}
