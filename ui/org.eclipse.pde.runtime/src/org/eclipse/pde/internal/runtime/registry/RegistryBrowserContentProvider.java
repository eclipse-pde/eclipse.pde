/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.ArrayList;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.internal.runtime.registry.model.*;

public class RegistryBrowserContentProvider implements ITreeContentProvider {
	public boolean isInExtensionSet;
	private RegistryBrowser fRegistryBrowser;

	public RegistryBrowserContentProvider(RegistryBrowser registryBrowser) {
		fRegistryBrowser = registryBrowser;
	}

	public void dispose() { // nothing to dispose
	}

	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	public Object[] getChildren(Object element) {
		if (element == null)
			return null;

		if (element instanceof Extension)
			return ((Extension) element).getConfigurationElements();

		isInExtensionSet = false;
		if (element instanceof ExtensionPoint)
			return ((ExtensionPoint) element).getExtensions().toArray();

		if (element instanceof ConfigurationElement)
			return ((ConfigurationElement) element).getElements();

		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;

			Folder[] folders = new Folder[6];
			folders[0] = new Folder(Folder.F_IMPORTS, bundle);
			folders[1] = new Folder(Folder.F_LIBRARIES, bundle);
			folders[2] = new Folder(Folder.F_EXTENSION_POINTS, bundle);
			folders[3] = new Folder(Folder.F_EXTENSIONS, bundle);
			folders[4] = new Folder(Folder.F_REGISTERED_SERVICES, bundle);
			folders[5] = new Folder(Folder.F_SERVICES_IN_USE, bundle);

			// filter out empty folders
			ArrayList folderList = new ArrayList();
			folderList.add(new Attribute(bundle.getModel(), Attribute.F_LOCATION, bundle.getLocation()));

			for (int i = 0; i < folders.length; i++) {
				if ((folders[i].getChildren() != null) && (folders[i].getChildren().length > 0))
					folderList.add(folders[i]);
			}
			return folderList.toArray();
		}

		if (element instanceof Folder) {
			Folder folder = (Folder) element;
			isInExtensionSet = folder.getId() == Folder.F_EXTENSIONS;
			Object[] objs = ((Folder) element).getChildren();
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

		if (element instanceof Object[]) {
			return (Object[]) element;
		}

		return null;
	}

	public Object getParent(Object element) {
		if (!(element instanceof ModelObject)) {
			return null;
		}

		ModelObject object = (ModelObject) element;

		boolean extOnly = fRegistryBrowser.showExtensionsOnly();

		if (element instanceof Folder) {
			return ((Folder) element).getParent();
		}

		return null;
	}

	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		return children != null && children.length > 0;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
	}

}
