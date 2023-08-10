/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.runtime.registry.model.Bundle;
import org.eclipse.pde.internal.runtime.registry.model.Extension;
import org.eclipse.pde.internal.runtime.registry.model.ExtensionPoint;
import org.eclipse.pde.internal.runtime.registry.model.ModelChangeDelta;
import org.eclipse.pde.internal.runtime.registry.model.ModelChangeListener;
import org.eclipse.pde.internal.runtime.registry.model.ModelObject;
import org.eclipse.pde.internal.runtime.registry.model.ServiceName;
import org.eclipse.pde.internal.runtime.registry.model.ServiceRegistration;
import org.eclipse.ui.progress.UIJob;

public class RegistryBrowserModelChangeListener implements ModelChangeListener {

	private RegistryBrowser fRegistryBrowser;

	public RegistryBrowserModelChangeListener(RegistryBrowser registryBrowser) {
		fRegistryBrowser = registryBrowser;
	}

	@Override
	public void modelChanged(final ModelChangeDelta[] delta) {
		UIJob.create("Updating Registry", monitor -> { //$NON-NLS-1$
			update(delta);
			return Status.OK_STATUS;
		}).schedule();
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
		switch (fRegistryBrowser.getGroupBy()) {
		case RegistryBrowser.BUNDLES:
			if (object instanceof Bundle) {
				return object;
			} else if (object instanceof ExtensionPoint ext) {
				return ext.getContributor();
			} else if (object instanceof Extension ext) {
				return ext.getContributor();
			} else if (object instanceof ServiceRegistration reg) {
				Bundle[] bundles = reg.getUsingBundles();
				if (bundles.length == 0) {
					return reg.getBundle();
				}

				Object[] result = new Object[bundles.length + 1];
				result[0] = reg.getBundle();
				System.arraycopy(bundles, 0, result, 1, bundles.length);

				return result;
			}
			break;
		case RegistryBrowser.EXTENSION_REGISTRY:
			if (object instanceof ExtensionPoint) {
				return object;
			} else if (object instanceof Extension ext) {
				return ext.getExtensionPoint();
			}
			break;
		case RegistryBrowser.SERVICES:
			if (object instanceof ServiceRegistration service) {
				return service.getName();
			} else if (object instanceof Bundle) {
				return ((Bundle) object).getServicesInUse();
			}
			break;
		default:
			break;
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
		for (ModelChangeDelta delta : deltas) {
			ModelObject object = delta.getModelObject();
			int flag = delta.getFlag();

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
