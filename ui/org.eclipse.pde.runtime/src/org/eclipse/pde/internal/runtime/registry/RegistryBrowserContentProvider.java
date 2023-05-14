/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 259348
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.internal.runtime.registry.model.Attribute;
import org.eclipse.pde.internal.runtime.registry.model.Bundle;
import org.eclipse.pde.internal.runtime.registry.model.ConfigurationElement;
import org.eclipse.pde.internal.runtime.registry.model.Extension;
import org.eclipse.pde.internal.runtime.registry.model.ExtensionPoint;
import org.eclipse.pde.internal.runtime.registry.model.Folder;
import org.eclipse.pde.internal.runtime.registry.model.ModelObject;
import org.eclipse.pde.internal.runtime.registry.model.RegistryModel;
import org.eclipse.pde.internal.runtime.registry.model.ServiceName;
import org.eclipse.pde.internal.runtime.registry.model.ServiceRegistration;

public class RegistryBrowserContentProvider implements ITreeContentProvider {

	public boolean isInExtensionSet;

	private RegistryBrowser fRegistryBrowser;

	public RegistryBrowserContentProvider(RegistryBrowser registryBrowser) {
		fRegistryBrowser = registryBrowser;
	}

	@Override
	public void dispose() { // nothing to dispose
	}

	@Override
	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof RegistryModel) {
			RegistryModel model = (RegistryModel) element;

			switch (fRegistryBrowser.getGroupBy()) {
				case (RegistryBrowser.BUNDLES) :
					return model.getBundles();
				case (RegistryBrowser.EXTENSION_REGISTRY) :
					return model.getExtensionPoints();
				case (RegistryBrowser.SERVICES) :
					return model.getServiceNames();
			}

			return null;
		}

		if (element instanceof Extension)
			return ((Extension) element).getConfigurationElements();

		isInExtensionSet = false;
		if (element instanceof ExtensionPoint)
			return ((ExtensionPoint) element).getExtensions().toArray();

		if (element instanceof ConfigurationElement)
			return ((ConfigurationElement) element).getElements();

		if (element instanceof Bundle) {
			if (fRegistryBrowser.getGroupBy() != RegistryBrowser.BUNDLES) // expands only in Bundles mode
				return null;

			Bundle bundle = (Bundle) element;

			List<Object> folders = new ArrayList<>(9);

			folders.add(new Attribute(Attribute.F_LOCATION, bundle.getLocation()));
			if (bundle.getImports().length > 0)
				folders.add(new Folder(Folder.F_IMPORTS, bundle));
			if (bundle.getImportedPackages().length > 0)
				folders.add(new Folder(Folder.F_IMPORTED_PACKAGES, bundle));
			if (bundle.getExportedPackages().length > 0)
				folders.add(new Folder(Folder.F_EXPORTED_PACKAGES, bundle));
			if (bundle.getLibraries().length > 0)
				folders.add(new Folder(Folder.F_LIBRARIES, bundle));
			if (bundle.getExtensionPoints().length > 0)
				folders.add(new Folder(Folder.F_EXTENSION_POINTS, bundle));
			if (bundle.getExtensions().length > 0)
				folders.add(new Folder(Folder.F_EXTENSIONS, bundle));
			if (bundle.getRegisteredServices().length > 0)
				folders.add(new Folder(Folder.F_REGISTERED_SERVICES, bundle));
			if (bundle.getServicesInUse().length > 0)
				folders.add(new Folder(Folder.F_SERVICES_IN_USE, bundle));
			if (bundle.getFragments().length > 0)
				folders.add(new Folder(Folder.F_FRAGMENTS, bundle));

			return folders.toArray();
		}

		isInExtensionSet = false;

		if (element instanceof Folder) {
			Folder folder = (Folder) element;
			isInExtensionSet = folder.getId() == Folder.F_EXTENSIONS;
			ModelObject[] objs = folder.getChildren();
			if (folder.getId() == Folder.F_USING_BUNDLES) {
				ModelObject[] result = new ModelObject[objs.length];
				ILabelProvider labelProvider = fRegistryBrowser.getAdapter(ILabelProvider.class);

				for (int i = 0; i < objs.length; i++) {
					result[i] = new Attribute(Attribute.F_BUNDLE, labelProvider.getText(objs[i]));
				}

				objs = result;
			}
			return objs;
		}
		if (element instanceof ConfigurationElement) {
			return ((ConfigurationElement) element).getElements();
		}

		if (element instanceof ExtensionPoint) {
			ExtensionPoint extensionPoint = (ExtensionPoint) element;
			Object[] objs = extensionPoint.getExtensions().toArray();
			return objs;
		}

		if (element instanceof ServiceName) {
			return ((ServiceName) element).getChildren();
		}

		if (element instanceof ServiceRegistration) {
			ServiceRegistration service = (ServiceRegistration) element;

			List<Folder> folders = new ArrayList<>();

			if (service.getProperties().length > 0)
				folders.add(new Folder(Folder.F_PROPERTIES, service));
			if (service.getUsingBundleIds().length > 0)
				folders.add(new Folder(Folder.F_USING_BUNDLES, service));

			return folders.toArray();
		}

		if (element instanceof Object[]) {
			return (Object[]) element;
		}

		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof ModelObject)) {
			return null;
		}

		if (element instanceof Folder) {
			return ((Folder) element).getParent();
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// Bundle and ServiceRegistration always have children
		if (element instanceof Bundle)
			return true;
		if (element instanceof ServiceRegistration)
			return true;

		Object[] children = getChildren(element);
		return children != null && children.length > 0;
	}

}
