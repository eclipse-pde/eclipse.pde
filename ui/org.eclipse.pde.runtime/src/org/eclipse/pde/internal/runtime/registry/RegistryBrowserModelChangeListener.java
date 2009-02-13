/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.pde.internal.runtime.registry.model.*;

public class RegistryBrowserModelChangeListener implements ModelChangeListener {

	private RegistryBrowser fRegistryBrowser;

	public RegistryBrowserModelChangeListener(RegistryBrowser registryBrowser) {
		fRegistryBrowser = registryBrowser;
	}

	public void modelChanged(final ModelChangeDelta[] delta) {
		fRegistryBrowser.getSite().getWorkbenchWindow().getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				update(delta);
			}
		});
	}

	private boolean topLevelElement(Object object) {
		switch (fRegistryBrowser.getGroupBy()) {
			case (RegistryBrowser.BUNDLES) :
				return object instanceof Bundle;
			case (RegistryBrowser.EXTENSION_REGISTRY) :
				return object instanceof ExtensionPoint;
			case (RegistryBrowser.SERVICES) :
				return object instanceof ServiceName;
		}

		return false;
	}

	/**
	 * TODO FIXME this should be moved to content provider getParent
	 * 
	 * @param object
	 * @return if returns array, then appears under all top level elements of that array
	 */
	private Object getTopLevelElement(Object object) {
		if (fRegistryBrowser.getGroupBy() == RegistryBrowser.BUNDLES) {
			if (object instanceof Bundle) {
				return object;
			} else if (object instanceof ExtensionPoint) {
				ExtensionPoint ext = (ExtensionPoint) object;
				return ext.getContributor();
			} else if (object instanceof Extension) {
				Extension ext = (Extension) object;
				return ext.getContributor();
			} else if (object instanceof ServiceRegistration) {
				ServiceRegistration reg = (ServiceRegistration) object;

				Bundle[] bundles = reg.getUsingBundles();
				if (bundles.length == 0) {
					return reg.getBundle();
				}

				Object[] result = new Object[bundles.length + 1];
				result[0] = reg.getBundle();
				System.arraycopy(bundles, 0, result, 1, bundles.length);

				return result;
			}
		} else if (fRegistryBrowser.getGroupBy() == RegistryBrowser.EXTENSION_REGISTRY) {
			if (object instanceof ExtensionPoint) {
				return object;
			} else if (object instanceof Extension) {
				Extension ext = (Extension) object;
				return ext.getExtensionPoint();
			}
		} else if (fRegistryBrowser.getGroupBy() == RegistryBrowser.SERVICES) {
			if (object instanceof ServiceRegistration) {
				ServiceRegistration service = (ServiceRegistration) object;
				return service.getName();
			} else if (object instanceof Bundle) {
				Object[] services = ((Bundle) object).getServicesInUse();
				for (int i = 0; i < services.length; i++) {
					ServiceRegistration service = ((ServiceRegistration) services[i]);
					services[i] = service.getName();
				}
				return services;
			}
		}

		return null;
	}

	private void refreshTopLevelElements(Object object) {
		Object topLevelElement = getTopLevelElement(object);

		if (topLevelElement == null)
			return;

		if (topLevelElement.getClass().isArray()) {
			Object[] array = (Object[]) topLevelElement;
			fRegistryBrowser.refresh(array);
		} else {
			fRegistryBrowser.refresh(topLevelElement);
		}
	}

	protected void update(ModelChangeDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			ModelObject object = deltas[i].getModelObject();
			int flag = deltas[i].getFlag();

			switch (flag) {
				case ModelChangeDelta.ADDED :
					if (topLevelElement(object)) {
						fRegistryBrowser.add(object);
					} else {
						refreshTopLevelElements(object);
					}
					break;
				case ModelChangeDelta.REMOVED :
					if (topLevelElement(object)) {
						fRegistryBrowser.remove(object);
					} else {
						refreshTopLevelElements(object);
					}
					break;
				case ModelChangeDelta.STARTED :
				case ModelChangeDelta.STOPPED :
				case ModelChangeDelta.RESOLVED :
				case ModelChangeDelta.UNRESOLVED :
				case ModelChangeDelta.UPDATED :
					if (topLevelElement(object)) {
						fRegistryBrowser.refresh(object);
					} else {
						refreshTopLevelElements(object);
					}
					break;
			}
		}
	}
}
