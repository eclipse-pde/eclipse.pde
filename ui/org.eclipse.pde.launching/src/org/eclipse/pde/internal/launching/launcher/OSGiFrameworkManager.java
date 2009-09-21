/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import org.eclipse.pde.launching.OSGiLaunchConfigurationInitializer;

import org.eclipse.pde.internal.launching.PDELaunchingPlugin;




import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.pde.internal.core.PDEPreferencesManager;

public class OSGiFrameworkManager implements IRegistryChangeListener {

	public static final String POINT_ID = "org.eclipse.pde.ui.osgiFrameworks"; //$NON-NLS-1$
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
		return (IConfigurationElement[]) fFrameworks.values().toArray(new IConfigurationElement[fFrameworks.size()]);
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
			if (id == null || elements[i].getAttribute(ATT_NAME) == null || elements[i].getAttribute(ATT_DELEGATE) == null)
				continue;
			fFrameworks.put(id, elements[i]);
		}
	}

	private IConfigurationElement[] orderElements(IConfigurationElement[] elems) {
		Arrays.sort(elems, new Comparator() {
			public int compare(Object o1, Object o2) {
				String name1 = ((IConfigurationElement) o1).getAttribute(ATT_NAME);
				String name2 = ((IConfigurationElement) o2).getAttribute(ATT_NAME);
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
		PDEPreferencesManager store = PDELaunchingPlugin.getDefault().getPreferenceManager();
		return store.getString(ILaunchingPreferenceConstants.DEFAULT_OSGI_FRAMEOWRK);
	}

	public OSGiLaunchConfigurationInitializer getDefaultInitializer() {
		return getInitializer(getDefaultFramework());
	}

	public OSGiLaunchConfigurationInitializer getInitializer(String frameworkID) {
		if (fFrameworks == null)
			loadElements();
		if (fFrameworks.containsKey(frameworkID)) {
			try {
				IConfigurationElement element = (IConfigurationElement) fFrameworks.get(frameworkID);
				if (element.getAttribute(ATT_INITIALIZER) != null) {
					Object result = element.createExecutableExtension(ATT_INITIALIZER);
					if (result instanceof OSGiLaunchConfigurationInitializer)
						return (OSGiLaunchConfigurationInitializer) result;
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
				IConfigurationElement element = (IConfigurationElement) fFrameworks.get(frameworkID);
				Object result = element.createExecutableExtension(ATT_DELEGATE);
				if (result instanceof LaunchConfigurationDelegate)
					return (LaunchConfigurationDelegate) result;
			} catch (CoreException e) {
			}
		}
		return null;
	}

	public String getFrameworkName(String frameworkID) {
		if (fFrameworks == null)
			loadElements();
		if (fFrameworks.containsKey(frameworkID)) {
			IConfigurationElement element = (IConfigurationElement) fFrameworks.get(frameworkID);
			return element.getAttribute(ATT_NAME);
		}
		return null;
	}

	/**
	 * Returns the {@link IConfigurationElement} for the framework with the given ID
	 * or <code>null</code> if no element exists with that ID.
	 * @param frameworkId
	 * @return the {@link IConfigurationElement} for the framework with the given ID or <code>null</code>
	 * 
	 * @since 3.5
	 */
	public IConfigurationElement getFramework(String frameworkId) {
		if (fFrameworks == null) {
			loadElements();
		}
		return (IConfigurationElement) fFrameworks.get(frameworkId);
	}

}
