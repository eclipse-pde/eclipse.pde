/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.launcher.OSGiLaunchConfigurationInitializer;

public class OSGiFrameworkManager implements IRegistryChangeListener {
	
	public static final String POINT_ID = "org.eclipse.pde.ui.osgiFrameworks";	 //$NON-NLS-1$
	public static final String DEFAULT_FRAMEWORK = "org.eclipse.pde.ui.EquinoxFramework"; //$NON-NLS-1$

	public static final String ATT_ID = "id"; //$NON-NLS-1$
	public static final String ATT_NAME = "name"; //$NON-NLS-1$
	public static final String ATT_DELEGATE = "launcherDelegate"; //$NON-NLS-1$
	public static final String ATT_INITIALIZER = "initializer"; //$NON-NLS-1$
	
	public static final String ELEMENT_FRAMEWORK = "framework"; //$NON-NLS-1$
	
	private Map fFrameworks;
	
	public IConfigurationElement[] getFrameworks() {
		if (fFrameworks == null)
			loadElements();
		return (IConfigurationElement[])fFrameworks.values().toArray(new IConfigurationElement[fFrameworks.size()]);	
	}
	
	public IConfigurationElement[] getSortedFrameworks() {
		IConfigurationElement[] elements = getFrameworks();
		return orderElements(elements);
	}
	
	private void loadElements() {
		fFrameworks = new HashMap();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(POINT_ID);
		for (int i = 0; i < elements.length; i++) {
			String id = elements[i].getAttribute(ATT_ID);
			if (id == null 
					|| elements[i].getAttribute(ATT_NAME) == null 
					|| elements[i].getAttribute(ATT_DELEGATE) == null)
				continue;
			fFrameworks.put(id, elements[i]);
		}
	}
	
	private IConfigurationElement[] orderElements(IConfigurationElement[] elems) {
		Arrays.sort(elems, new Comparator() {
			public int compare(Object o1, Object o2) {
				String name1 = ((IConfigurationElement)o1).getAttribute(ATT_NAME); 
				String name2 = ((IConfigurationElement)o2).getAttribute(ATT_NAME);
				if (name1 != null)
					return name1.compareToIgnoreCase(name2);
				return 1;
			}
		});
		return elems;
	}
	
	
	public void registryChanged(IRegistryChangeEvent event) {
		//TODO implement 
	}
	
	public String getDefaultFramework() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		return store.getString(IPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
	}
	
	public OSGiLaunchConfigurationInitializer getDefaultInitializer() {
		return getInitializer(getDefaultFramework());
	}
	
	public OSGiLaunchConfigurationInitializer getInitializer(String frameworkID) {
		if (fFrameworks == null)
			loadElements();
		if (fFrameworks.containsKey(frameworkID)) {
			try {
				IConfigurationElement element = (IConfigurationElement)fFrameworks.get(frameworkID);
				if (element.getAttribute(ATT_INITIALIZER) != null) {
					Object result = element.createExecutableExtension(ATT_INITIALIZER);
					if (result instanceof OSGiLaunchConfigurationInitializer) 
						return (OSGiLaunchConfigurationInitializer)result;
				}
			} catch (CoreException e) {
			}
		}
		return new OSGiLaunchConfigurationInitializer();		
	}
	
	public LaunchConfigurationDelegate getFrameworkLauncher(String frameworkID) {
		if (fFrameworks == null)
			loadElements();
		if (fFrameworks.containsKey(frameworkID)) {
			try {
				IConfigurationElement element = (IConfigurationElement)fFrameworks.get(frameworkID);
				Object result = element.createExecutableExtension(ATT_DELEGATE);
				if (result instanceof LaunchConfigurationDelegate) 
					return (LaunchConfigurationDelegate)result;
			} catch (CoreException e) {
			}
		}
		return null;
	}
	
	public String getFrameworkName(String frameworkID) {
		if (fFrameworks == null)
			loadElements();
		if (fFrameworks.containsKey(frameworkID)) {
			IConfigurationElement element = (IConfigurationElement)fFrameworks.get(frameworkID);
			return element.getAttribute(ATT_NAME);
		}
		return null;
	}


}
