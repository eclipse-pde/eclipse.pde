/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
public class RegistryBrowserContentProvider
		implements
			org.eclipse.jface.viewers.ITreeContentProvider {
	private Hashtable pluginMap = new Hashtable();
	private boolean isFocusedSearch = false;
	private String searchTextName = "";
	private String searchTextId = "";
	private PluginObjectAdapter[] viewerItems;
	
	class PluginFolder implements IPluginFolder {
		private int id;
		IPluginDescriptor pd;
		private Object[] children;
		public PluginFolder(IPluginDescriptor pd, int id) {
			this.pd = pd;
			this.id = id;
		}
		public IPluginDescriptor getPluginDescriptor() {
			return pd;
		}
		public Object[] getChildren() {
			if (children == null)
				children = getFolderChildren(pd, id);
			return children;
		}
		public int getFolderId() {
			return id;
		}
		public Object getAdapter(Class key) {
			return null;
		}
	}
	protected PluginObjectAdapter createAdapter(Object object, int id) {
		if (id == IPluginFolder.F_EXTENSIONS)
			return new ExtensionAdapter(object);
		if (id == IPluginFolder.F_EXTENSION_POINTS)
			return new ExtensionPointAdapter(object);
		return new PluginObjectAdapter(object);
	}
	protected Object[] createPluginFolders(IPluginDescriptor pd) {
		Object[] array = new Object[4];
		array[0] = new PluginFolder(pd, IPluginFolder.F_IMPORTS);
		array[1] = new PluginFolder(pd, IPluginFolder.F_LIBRARIES);
		array[2] = new PluginFolder(pd, IPluginFolder.F_EXTENSION_POINTS);
		array[3] = new PluginFolder(pd, IPluginFolder.F_EXTENSIONS);
		return array;
	}
	public void dispose() {
	}
	public Object[] getElements(Object element) {
		if (viewerItems!=null)
			return viewerItems;
		return getChildren(element);
	}
	public Object[] getChildren(Object element) {
		if (element instanceof ExtensionAdapter) {
			return ((ExtensionAdapter) element).getChildren();
		}
		if (element instanceof ExtensionPointAdapter) {
			ArrayList configElements = new ArrayList();
			Object[] children = ((ExtensionPointAdapter) element).getChildren();
			for (int i = 0; i < children.length; i++) {
				Object[] countChildren = ((ExtensionAdapter) children[i])
						.getChildren();
				if (countChildren != null)
					for (int j = 0; j < countChildren.length; j++) 
						configElements.add(countChildren[j]);
			}
			return configElements.toArray(new Object[configElements.size()]);
		}
		if (element instanceof ConfigurationElementAdapter) {
			return ((ConfigurationElementAdapter) element).getChildren();
		}
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element.equals(Platform.getPluginRegistry())) {
			Object[] plugins = getPlugins(Platform.getPluginRegistry());
			if (isFocusedSearch && plugins != null) {
				ArrayList resultList = new ArrayList();
				for (int i = 0; i < plugins.length; i++) {
					if (plugins[i] instanceof PluginObjectAdapter) {
						Object object = ((PluginObjectAdapter) plugins[i])
								.getObject();
						if (object instanceof IPluginDescriptor) {
							IPluginDescriptor desc = (IPluginDescriptor) object;
							if (searchTextId != null
									&& desc
											.getUniqueIdentifier()
											.toLowerCase()
											.indexOf(searchTextId.toLowerCase()) != -1)
								resultList.add(plugins[i]);
							else if (searchTextName != null
									&& desc.getLabel().toLowerCase().indexOf(
											searchTextName.toLowerCase()) != -1)
								resultList.add(plugins[i]);
						}
					}
				}
				isFocusedSearch = false;
				return resultList.toArray(new Object[resultList.size()]);
			}
			return plugins;
		}
		if (element instanceof IPluginDescriptor) {
			IPluginDescriptor desc = (IPluginDescriptor) element;
			Object[] folders = (Object[]) pluginMap.get(desc
					.getUniqueIdentifier());
			if (folders == null) {
				folders = createPluginFolders(desc);
				pluginMap.put(desc.getUniqueIdentifier(), folders);
			}
			return folders;
		}
		if (element instanceof IPluginFolder) {
			IPluginFolder folder = (IPluginFolder) element;
			return getFolderChildren(folder);
		}
		if (element instanceof IConfigurationElement) {
			return ((IConfigurationElement) element).getChildren();
		}
		return null;
	}
	public Object[] getFolderChildren(IPluginFolder folder) {
		Object[] children = folder.getChildren();
		if (children == null)
			return null;
		if (children[0] instanceof ExtensionAdapter) {
			ArrayList configElements = new ArrayList();
			for (int i = 0; i < children.length; i++) {
				Object[] countChildren = ((ExtensionAdapter) children[i])
						.getChildren();
				if (countChildren != null)
					for (int j = 0; j < countChildren.length; j++) {
						if (((IConfigurationElement) ((ConfigurationElementAdapter) countChildren[j])
								.getObject()).getAttributeAsIs("id") != null)
							configElements.add(countChildren[j]);
					}
			}
			return configElements.toArray(new Object[configElements.size()]);
		}
		return children;
	}
	public Object[] getPlugins(IPluginRegistry registry) {
		IPluginDescriptor[] descriptors = registry.getPluginDescriptors();
		Object[] result = new Object[descriptors.length];
		for (int i = 0; i < descriptors.length; i++) {
			result[i] = new PluginObjectAdapter(descriptors[i]);
		}
		return result;
	}
	private Object[] getFolderChildren(IPluginDescriptor pd, int id) {
		Object[] array = null;
		switch (id) {
			case IPluginFolder.F_EXTENSIONS :
				array = pd.getExtensions();
				break;
			case IPluginFolder.F_EXTENSION_POINTS :
				array = pd.getExtensionPoints();
				break;
			case IPluginFolder.F_IMPORTS :
				array = pd.getPluginPrerequisites();
				break;
			case IPluginFolder.F_LIBRARIES :
				array = pd.getRuntimeLibraries();
				break;
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
	public Object getParent(Object element) {
		return null;
	}
	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		return children != null && children.length > 0;
	}
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	public boolean isDeleted(Object element) {
		return false;
	}
	public void setUniqueIdSearch(String id) {
		if (id == null || id.length() == 0) {
			isFocusedSearch = false;
			return;
		}
		searchTextId = id;
		searchTextName = null;
		isFocusedSearch = true;
	}
	public void setNameSearch(String name) {
		if (name == null || name.length() == 0) {
			isFocusedSearch = false;
			return;
		}
		searchTextName = name;
		searchTextId = null;
		isFocusedSearch = true;
	}
	public void setViewerPlugins(PluginObjectAdapter[] items){
		viewerItems = items;
	}
}
