package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.jface.viewers.*;

public class RegistryBrowserContentProvider
	implements org.eclipse.jface.viewers.ITreeContentProvider {
	private Hashtable pluginMap = new Hashtable();

	class PluginFolder implements IPluginFolder {
		private int id;
		IPluginDescriptor pd;
		private Object [] children;

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
	if (id==IPluginFolder.F_EXTENSIONS)
	   return new ExtensionAdapter(object);
	if (id==IPluginFolder.F_EXTENSION_POINTS)
	   return new ExtensionPointAdapter(object);
	return new PluginObjectAdapter(object);
}
protected Object[] createPluginFolders(IPluginDescriptor pd) {
	Object [] array = new Object[4];
	array [0] = new PluginFolder(pd, IPluginFolder.F_IMPORTS);
	array [1] = new PluginFolder(pd, IPluginFolder.F_LIBRARIES);
	array [2] = new PluginFolder(pd, IPluginFolder.F_EXTENSION_POINTS);
	array [3] = new PluginFolder(pd, IPluginFolder.F_EXTENSIONS);
	return array;
}
public void dispose() {}

public Object[] getElements(Object element) {
	return getChildren(element);
}

public Object[] getChildren(Object element) {
	if (element instanceof ExtensionAdapter) {
		return ((ExtensionAdapter)element).getChildren();
	}
	if (element instanceof ExtensionPointAdapter) {
		return ((ExtensionPointAdapter)element).getChildren();
	}
	if (element instanceof ConfigurationElementAdapter) {
		return ((ConfigurationElementAdapter)element).getChildren();
	}
	if (element instanceof PluginObjectAdapter)
		element = ((PluginObjectAdapter)element).getObject();
	if (element.equals(Platform.getPluginRegistry())) {
		return getPlugins(Platform.getPluginRegistry());
	}
	if (element instanceof IPluginDescriptor) {
		IPluginDescriptor desc = (IPluginDescriptor) element;
		Object [] folders = (Object[]) pluginMap.get(desc.getUniqueIdentifier());
		if (folders == null) {
			folders = createPluginFolders(desc);
			pluginMap.put(desc.getUniqueIdentifier(), folders);
		}
		return folders;
	}
	if (element instanceof IPluginFolder) {
		IPluginFolder folder = (IPluginFolder) element;
		return folder.getChildren();
	}
	return null;
}

public Object[] getPlugins(IPluginRegistry registry) {
	IPluginDescriptor [] descriptors = registry.getPluginDescriptors();
	Object [] result = new Object[descriptors.length];

	for (int i=0; i<descriptors.length; i++) {
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
	return children!=null && children.length>0;
}
public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
public boolean isDeleted(Object element) {
	return false;
}
}
