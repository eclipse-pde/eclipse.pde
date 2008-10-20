/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
		return fRegistryBrowser.showExtensionsOnly() ? object instanceof ExtensionPoint : object instanceof Bundle;
	}

	private Object getTopLevelElement(Object object) {
		if (!fRegistryBrowser.showExtensionsOnly()) { // show bundles
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
				//return reg.getBundle(); // TODO FIXME
			}
		} else { // show only extensions
			if (object instanceof ExtensionPoint) {
				return object;
			} else if (object instanceof Extension) {
				Extension ext = (Extension) object;
				return ext.getExtensionPoint();
			}
		}

		return null;
	}

	protected void update(ModelChangeDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			ModelObject object = deltas[i].getModelObject();

			switch (deltas[i].getFlag()) {
				case ModelChangeDelta.ADDED :
					if (topLevelElement(object)) {
						fRegistryBrowser.add(object);
					} else {
						Object topLevelElement = getTopLevelElement(object);
						fRegistryBrowser.refresh(topLevelElement);
					}
					break;
				case ModelChangeDelta.REMOVED :
					if (topLevelElement(object)) {
						fRegistryBrowser.remove(object);
					} else {
						Object topLevelElement = getTopLevelElement(object);
						fRegistryBrowser.refresh(topLevelElement);
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
						Object topLevelElement = getTopLevelElement(object);
						fRegistryBrowser.refresh(topLevelElement);
					}
					break;
			}
		}
	}

}
