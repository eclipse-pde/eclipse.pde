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
	private byte showType;
	public boolean isInExtensionSet;
	private TreeViewer viewer;

	
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
	
	public RegistryBrowserContentProvider(TreeViewer viewer){
		super();
		this.viewer = viewer;
		showType = ShowPluginsMenu.SHOW_RUNNING_PLUGINS;
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
		return getChildren(element);
	}
	public Object[] getChildren(Object element) {
		
		if (element instanceof ExtensionAdapter) {
			return ((ExtensionAdapter) element).getChildren();
		}
		isInExtensionSet = false;
		if (element instanceof ExtensionPointAdapter) {
			 return getNonDuplicateLabelChildren(element);
		}
		if (element instanceof ConfigurationElementAdapter) {
			return ((ConfigurationElementAdapter) element).getChildren();
		}
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element.equals(Platform.getPluginRegistry())) {
			Object[] plugins = getPlugins(Platform.getPluginRegistry());
			
			if (plugins == null)
				return new Object[0];
			
			if (showType != ShowPluginsMenu.SHOW_ALL_PLUGINS){ //|| searchType != RegistrySearchMenu.NO_SEARCH){
				boolean matchesShowCriteria = true;
				ArrayList resultList = new ArrayList();
				for (int i = 0; i < plugins.length; i++) {
					if (plugins[i] instanceof PluginObjectAdapter) {
						Object object = ((PluginObjectAdapter) plugins[i])
								.getObject();
						if (object instanceof IPluginDescriptor) {
							IPluginDescriptor desc = (IPluginDescriptor) object;
										
							// handle showing criteria
							if (showType != ShowPluginsMenu.SHOW_ALL_PLUGINS)
								matchesShowCriteria = (showType == ShowPluginsMenu.SHOW_RUNNING_PLUGINS && desc.isPluginActivated()) ||
										(showType == ShowPluginsMenu.SHOW_NON_RUNNING_PLUGINS && !desc.isPluginActivated());
						}
					}
					if (matchesShowCriteria)
						resultList.add(plugins[i]);
				}
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
			} else {
				ArrayList folderList = new ArrayList();
				for (int i = 0; i<folders.length; i++){
					if (folders[i] != null && ((IPluginFolder)folders[i]).getChildren() != null)
						folderList.add(folders[i]);
				}
				folders = folderList.toArray(new Object[folderList.size()]);
			}
			return folders;
		}
		if (element instanceof IPluginFolder) {
			IPluginFolder folder = (IPluginFolder) element;
			isInExtensionSet = folder.getFolderId() == 1;
			return getNonDuplicateLabelChildren(folder);
		}
		if (element instanceof IConfigurationElement) {
			return ((IConfigurationElement) element).getChildren();
		}
		return null;
	}
	public Object[] getNonDuplicateLabelChildren(Object element) {
		ArrayList extList = new ArrayList();
		ArrayList labelList = new ArrayList();
		if (element instanceof IPluginFolder){
			Object[] children = ((IPluginFolder)element).getChildren();
			if (children != null && isInExtensionSet){
				for (int i = 0; i<children.length; i++){
					IExtension ext = (IExtension)((ExtensionAdapter)children[i]).getObject();
					String label = ((RegistryBrowserLabelProvider)viewer.getLabelProvider()).getText(ext);
					if (label == null || label.length() ==0)
						continue;
					if (!labelList.contains(label)){
						labelList.add(label);
						extList.add(children[i]);
					}
				}
				return extList.toArray(new Object[extList.size()]);
			}
			return children;
		} else if (element instanceof ExtensionPointAdapter){
			Object[] children = ((ExtensionPointAdapter) element).getChildren();
			if (children!=null){
				for (int i =0; i<children.length; i++){
					String label = ((RegistryBrowserLabelProvider)viewer.getLabelProvider()).getText(children[i]);
					if (label == null || label.length() ==0)
						continue;
					if (!labelList.contains(label)){
						labelList.add(label);
						extList.add(children[i]);
					}
				}
				return extList.toArray(new Object[extList.size()]);
			}
			return children;
		}
		return new Object[0];
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
	public void setShowType(byte type){
		this.showType = type;
	}
	public byte getShowType(){
		return showType;
	}
}
