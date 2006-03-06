/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.Hashtable;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class RegistryBrowserContentProvider implements ITreeContentProvider {
	private Hashtable fPluginMap = new Hashtable();
	private boolean fShowRunning;
	public boolean isInExtensionSet;
	private TreeViewer fViewer;
	private int fPluginsTotal;
	
	class BundleFolder implements IBundleFolder {
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
			if (children == null)
				children = getFolderChildren(bundle, id);
			return children;
		}
		public int getFolderId() {
			return id;
		}
		public Object getAdapter(Class key) {
			return null;
		}
	}
	
	class BundlePrerequisite implements IBundlePrerequisite {
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
	
	class BundleLibrary implements IBundleLibrary {
		private ManifestElement underlyingElement;
		public BundleLibrary(ManifestElement element) {
			underlyingElement = element;
		}
		public String getLibrary() {
			return underlyingElement.getValue();
		}
	}
	
	public RegistryBrowserContentProvider(TreeViewer viewer, boolean showRunning){
		super();
		this.fViewer = viewer;
		this.fShowRunning = showRunning;
		this.fPluginsTotal = 0;
	}
	
	protected PluginObjectAdapter createAdapter(Object object, int id) {
		if (id == IBundleFolder.F_EXTENSIONS)
			return new ExtensionAdapter(object);
		if (id == IBundleFolder.F_EXTENSION_POINTS)
			return new ExtensionPointAdapter(object);
		return new PluginObjectAdapter(object);
	}
	protected Object[] createPluginFolders(Bundle bundle) {
		Object[] array = new Object[5];
		array[0] = new BundleFolder(bundle, IBundleFolder.F_LOCATION);
		array[1] = new BundleFolder(bundle, IBundleFolder.F_IMPORTS);
		array[2] = new BundleFolder(bundle, IBundleFolder.F_LIBRARIES);
		array[3] = new BundleFolder(bundle, IBundleFolder.F_EXTENSION_POINTS);
		array[4] = new BundleFolder(bundle, IBundleFolder.F_EXTENSIONS);
		return array;
	}
	
	public void dispose() { }
	
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
		
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		
		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;
			Object[] folders = (Object[]) fPluginMap.get(bundle.getSymbolicName());
			if (folders == null) {
				folders = createPluginFolders(bundle);
				fPluginMap.put(bundle.getSymbolicName(), folders);
			} else {
				ArrayList folderList = new ArrayList();
				for (int i = 0; i < folders.length; i++) {
					if (folders[i] != null
							&& ((IBundleFolder)folders[i]).getChildren() != null
							|| ((IBundleFolder)folders[i]).getFolderId() == IBundleFolder.F_LOCATION)
						folderList.add(folders[i]);
				}
				folders = folderList.toArray(new Object[folderList.size()]);
			}
			return folders;
		}
		if (element instanceof IBundleFolder) {
			IBundleFolder folder = (IBundleFolder) element;
			isInExtensionSet = folder.getFolderId() == IBundleFolder.F_EXTENSIONS;
			return ((IBundleFolder) element).getChildren();
		}
		if (element instanceof IConfigurationElement) {
			return ((IConfigurationElement) element).getChildren();
		}
		if (element instanceof Bundle[]) {
			PluginObjectAdapter[] bundles = getPlugins((Bundle[])element);
			fPluginsTotal = bundles.length;
			
			if (fShowRunning){
				ArrayList resultList = new ArrayList();
				for (int i = 0; i < bundles.length; i++)
					if (bundles[i].getObject() instanceof Bundle &&
							((Bundle)bundles[i].getObject()).getState() == Bundle.ACTIVE)
						resultList.add(bundles[i]);
				return resultList.toArray(new Object[resultList.size()]);
			}
			return bundles;
		}
		return null;
	}

	public PluginObjectAdapter[] getPlugins(Bundle[] bundles) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < bundles.length; i++)
			if (bundles[i].getHeaders().get(Constants.FRAGMENT_HOST) == null)
				list.add(new PluginObjectAdapter(bundles[i]));
		return (PluginObjectAdapter[]) list.toArray(new PluginObjectAdapter[list.size()]);
	}
	private Object[] getFolderChildren(Bundle bundle, int id) {
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
	public void setShowRunning(boolean showRunning){
		this.fShowRunning = showRunning;
	}
	public boolean isShowRunning(){
		return fShowRunning;
	}
	
	public String getTitleSummary(){
		if (fViewer == null || fViewer.getTree() == null)
			return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {"0", "0"})); //$NON-NLS-1$ //$NON-NLS-2$
		
		return NLS.bind(PDERuntimeMessages.RegistryView_titleSummary, (new String[] {new Integer(fViewer.getTree().getItemCount()).toString(), new Integer(fPluginsTotal).toString()}));
	}
	
	private Object[] getManifestHeaderArray(Bundle bundle, String headerKey) {
		String libraries = (String)bundle.getHeaders().get(headerKey);
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
		} catch (BundleException e) {
		}
		return null;
	}
	
}
