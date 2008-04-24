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

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.osgi.framework.*;

public class RegistryBrowserContentProvider implements ITreeContentProvider {
	private Hashtable fExtensionPointMap = new Hashtable();
	public boolean isInExtensionSet;

	static class BundleFolder implements IBundleFolder {
		private int id;
		private Bundle bundle;
		private Object[] children;

		public BundleFolder(Bundle pd, int id) {
			this.bundle = pd;
			this.id = id;
		}

		public Bundle getBundle() {
			return bundle;
		}

		public Object[] getChildren() {
			if (children == null) {
				children = getFolderChildren(bundle, id);
			}
			return children;
		}

		/**
		 * Resets folder's previously cached knowledge about it's children. 
		 */
		public void refresh() {
			children = null;
		}

		public int getFolderId() {
			return id;
		}

		public Object getAdapter(Class key) {
			return null;
		}
	}

	static class BundlePrerequisite implements IBundlePrerequisite {
		private ManifestElement underlyingElement;

		public BundlePrerequisite(ManifestElement element) {
			underlyingElement = element;
		}

		public ManifestElement getPrerequisite() {
			return underlyingElement;
		}

		public boolean isExported() {
			String visibility = underlyingElement.getDirective(Constants.VISIBILITY_DIRECTIVE);
			return Constants.VISIBILITY_REEXPORT.equals(visibility);
		}

		public String getLabel() {
			String version = underlyingElement.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
			String value = underlyingElement.getValue();
			if (version == null)
				return value;
			if (Character.isDigit(version.charAt(0)))
				version = '(' + version + ')';
			return value + ' ' + version;
		}
	}

	static class BundleLibrary implements IBundleLibrary {
		private ManifestElement underlyingElement;

		public BundleLibrary(ManifestElement element) {
			underlyingElement = element;
		}

		public String getLibrary() {
			return underlyingElement.getValue();
		}
	}

	/**
	 * Creates contents adapter for given folder id.
	 * @param object Folder contents to be wrapped in adapter
	 * @param id Type of folder
	 * @return Adapter 
	 */
	static protected PluginObjectAdapter createAdapter(Object object, int id) {
		if (id == IBundleFolder.F_EXTENSIONS)
			return new ExtensionAdapter(object);
		if (id == IBundleFolder.F_EXTENSION_POINTS)
			return new ExtensionPointAdapter(object);
		return new PluginObjectAdapter(object);
	}

	public void dispose() { // nothing to dispose
	}

	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	public Object[] getChildren(Object element) {
		if (element == null)
			return null;

		if (element instanceof ExtensionAdapter)
			return ((ExtensionAdapter) element).getChildren();

		isInExtensionSet = false;
		if (element instanceof ExtensionPointAdapter)
			return ((ExtensionPointAdapter) element).getChildren();

		if (element instanceof ConfigurationElementAdapter)
			return ((ConfigurationElementAdapter) element).getChildren();

		if (element instanceof PluginAdapter) {
			PluginAdapter bundle = (PluginAdapter) element;

			Object[] folders = bundle.getChildren();

			// filter out empty folders
			ArrayList folderList = new ArrayList();
			for (int i = 0; i < folders.length; i++) {
				if (folders[i] != null && ((IBundleFolder) folders[i]).getChildren() != null || ((IBundleFolder) folders[i]).getFolderId() == IBundleFolder.F_LOCATION)
					folderList.add(folders[i]);
			}
			folders = folderList.toArray(new Object[folderList.size()]);

			return folders;
		}

		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();

		if (element instanceof IBundleFolder) {
			IBundleFolder folder = (IBundleFolder) element;
			isInExtensionSet = folder.getFolderId() == IBundleFolder.F_EXTENSIONS;
			return ((IBundleFolder) element).getChildren();
		}
		if (element instanceof IConfigurationElement) {
			return ((IConfigurationElement) element).getChildren();
		}
		if (element instanceof Object[]) {
			return (Object[]) element;
		}
		if (element instanceof IExtensionPoint) {
			IExtensionPoint extensionPoint = (IExtensionPoint) element;
			String id = extensionPoint.getUniqueIdentifier();

			Object[] children = (Object[]) fExtensionPointMap.get(id);
			if (children == null) {
				Object[] array = extensionPoint.getExtensions();
				if (array != null && array.length > 0) {
					children = new Object[array.length];
					for (int i = 0; i < array.length; i++) {
						children[i] = createAdapter(array[i], IBundleFolder.F_EXTENSIONS);
					}

					fExtensionPointMap.put(id, children);
				}
			}

			return children;
		}
		return null;
	}

	protected static Object[] getFolderChildren(Bundle bundle, int id) {
		Object[] array = null;
		String bundleId = bundle.getSymbolicName();
		switch (id) {
			case IBundleFolder.F_EXTENSIONS :
				array = Platform.getExtensionRegistry().getExtensions(bundleId);
				break;
			case IBundleFolder.F_EXTENSION_POINTS :
				array = Platform.getExtensionRegistry().getExtensionPoints(bundleId);
				break;
			case IBundleFolder.F_IMPORTS :
				array = getManifestHeaderArray(bundle, Constants.REQUIRE_BUNDLE);
				break;
			case IBundleFolder.F_LIBRARIES :
				array = getManifestHeaderArray(bundle, Constants.BUNDLE_CLASSPATH);
				break;
			case IBundleFolder.F_REGISTERED_SERVICES :
				return getServices(bundle, IBundleFolder.F_REGISTERED_SERVICES);
			case IBundleFolder.F_SERVICES_IN_USE :
				return getServices(bundle, IBundleFolder.F_SERVICES_IN_USE);
		}
		Object[] result = null;
		if (array != null && array.length > 0) {
			result = new Object[array.length];
			for (int i = 0; i < array.length; i++) {
				result[i] = createAdapter(array[i], id);
			}
		}
		return result;
	}

	protected static Object[] getServices(Bundle bundle, int type) {
		Set result = new HashSet();

		try {
			ServiceReference[] references = PDERuntimePlugin.getDefault().getBundleContext().getAllServiceReferences(null, null);

			for (int i = 0; i < references.length; i++) {
				ServiceReference ref = references[i];

				if ((type == IBundleFolder.F_REGISTERED_SERVICES) && (bundle.equals(ref.getBundle()))) {
					result.add(new ServiceReferenceAdapter(ref));
				}

				Bundle[] usingBundles = ref.getUsingBundles();
				if ((type == IBundleFolder.F_SERVICES_IN_USE) && (usingBundles != null && Arrays.asList(usingBundles).contains(bundle))) {
					result.add(new ServiceReferenceAdapter(ref));
				}
			}

		} catch (InvalidSyntaxException e) { // nothing
		}

		if (result.size() == 0)
			return null;

		return result.toArray(new ServiceReferenceAdapter[result.size()]);
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		return children != null && children.length > 0;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { // do nothing
	}

	static private Object[] getManifestHeaderArray(Bundle bundle, String headerKey) {
		String libraries = (String) bundle.getHeaders().get(headerKey);
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(headerKey, libraries);
			if (elements == null)
				return null;
			if (headerKey.equals(Constants.BUNDLE_CLASSPATH)) {
				IBundleLibrary[] array = new IBundleLibrary[elements.length];
				for (int i = 0; i < elements.length; i++)
					array[i] = new BundleLibrary(elements[i]);
				return array;
			} else if (headerKey.equals(Constants.REQUIRE_BUNDLE)) {
				IBundlePrerequisite[] array = new IBundlePrerequisite[elements.length];
				for (int i = 0; i < elements.length; i++)
					array[i] = new BundlePrerequisite(elements[i]);
				return array;
			}
		} catch (BundleException e) { // do nothing
		}
		return null;
	}

}
