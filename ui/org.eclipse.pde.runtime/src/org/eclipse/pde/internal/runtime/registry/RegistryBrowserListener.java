/******************************************************************************
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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.registry.RegistryBrowserContentProvider.BundleFolder;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.osgi.framework.*;

public class RegistryBrowserListener implements IRegistryChangeListener, BundleListener, ServiceListener {

	protected RegistryBrowser fBrowser;
	protected boolean fExtOnly;

	protected RegistryBrowserListener(RegistryBrowser browser) {
		fBrowser = browser;
	}

	public void registryChanged(final IRegistryChangeEvent event) {
		final Tree tree = fBrowser.getUndisposedTree();
		if (tree == null)
			return;

		tree.getDisplay().asyncExec(new Runnable() {
			public void run() {
				IExtensionDelta[] deltas = event.getExtensionDeltas();
				for (int i = 0; i < deltas.length; i++) {
					if (fExtOnly)
						handleExtOnlyEvent(deltas[i]);
					else
						handleEvent(deltas[i]);
				}
			}
		});
	}

	public void bundleChanged(final BundleEvent event) {
		final Tree tree = fBrowser.getUndisposedTree();
		if (tree == null)
			return;

		tree.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fExtOnly)
					handleExtOnlyEvent(event.getType(), event.getBundle());
				else
					handleEvent(event.getType(), event.getBundle());
			}
		});
	}

	void handleEvent(IExtensionDelta delta) {
		handleDelta(delta, false);
	}

	void handleExtOnlyEvent(IExtensionDelta delta) {
		handleDelta(delta, true);
	}

	private void handleDelta(IExtensionDelta delta, boolean extOnly) {
		IExtension ext = delta.getExtension();
		IExtensionPoint extPoint = delta.getExtensionPoint();
		// TODO fix this method (and addToTree/removeFromTree)
		// bug 130655
		if (delta.getKind() == IExtensionDelta.ADDED) {
			System.out.println("adding "); //$NON-NLS-1$
			if (ext != null)
				System.out.println("ext: " + ext.getUniqueIdentifier() + "/" + ext.getLabel() + " : " + ext.getExtensionPointUniqueIdentifier()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (extPoint != null)
				System.out.println("extPoint: " + extPoint.getUniqueIdentifier()); //$NON-NLS-1$
			addExtensionObjectToTree(ext, extOnly);
			addExtensionObjectToTree(extPoint, extOnly);
			//			addToTree(ext);
			//			addToTree(extPoint);
		} else if (delta.getKind() == IExtensionDelta.REMOVED) {
			System.out.println("removing "); //$NON-NLS-1$
			if (ext != null)
				System.out.println("ext: " + ext.getUniqueIdentifier() + " : " + ext.getExtensionPointUniqueIdentifier()); //$NON-NLS-1$ //$NON-NLS-2$
			if (extPoint != null)
				System.out.println("extPoint: " + extPoint.getUniqueIdentifier()); //$NON-NLS-1$
			removeFromTree(ext);
			removeFromTree(extPoint);
		}
	}

	//	private void addToTree(Object object) {
	//		String namespace = getNamespaceIdentifier(object);
	//		if (namespace == null)
	//			return;
	//		TreeItem[] items = fTreeViewer.getTree().getItems();
	//		for (int i = 0; i < items.length; i++) {
	//			Object data = items[i].getData();
	//			Object adapted = null;
	//			if (data instanceof PluginObjectAdapter)
	//				adapted = ((PluginObjectAdapter)data).getObject();
	//			if (adapted instanceof Bundle && ((Bundle)adapted).getSymbolicName().equals(namespace)) {
	//				addBundleToTree(items[i], data, object);
	//			}
	//		}
	//	}

	private void addExtensionObjectToTree(Object child, boolean extOnly) {
		Object parent = null;
		if (!extOnly)
			parent = getAdoptingBundleParent(child);
		else if (child instanceof IExtensionPoint) {
			// add to root
			fBrowser.add(child);
			return;
		} else if (child instanceof IExtension) {
			// search all extensionPoints and return the correct one
			String extPoint = ((IExtension) child).getExtensionPointUniqueIdentifier();
			TreeItem[] items = fBrowser.getTreeItems();
			for (int i = 0; i < items.length; i++) {
				Object data = items[i].getData();
				if (data instanceof PluginObjectAdapter)
					data = ((PluginObjectAdapter) data).getObject();
				if (data instanceof IExtensionPoint && ((IExtensionPoint) data).getUniqueIdentifier().equals(extPoint)) {
					parent = items[i].getData();
					break;
				}
			}
		}
		if (parent != null)
			fBrowser.add(parent, child);
	}

	private Object getAdoptingBundleParent(Object child) {
		TreeItem bundleItem = findBundleItem(getNamespaceIdentifier(child));
		if (bundleItem != null) {
			// TODO fix this
			// remove this if (true) clause and return the proper parent
			if (true) {
				fBrowser.refresh(bundleItem.getData());
				fBrowser.updateItems(false);
				return null;
			}
			TreeItem[] folders = bundleItem.getItems();
			for (int j = 0; j < folders.length; j++) {
				// make sure to check extensionsOnlyMode()
				// and add to root/proper extension if true
				IBundleFolder folder = (IBundleFolder) folders[j].getData();
				if (correctFolder(folder, child))
					return folder;
			}
			// folder not found - 1st extension - refresh bundle item
			// to rebuild folders
			fBrowser.refresh(bundleItem.getData());
		}
		return null;
	}

	private TreeItem findBundleItem(String namespace) {
		if (namespace == null)
			return null;
		TreeItem[] items = fBrowser.getTreeItems();
		for (int i = 0; i < items.length; i++) {
			Object data = items[i].getData();
			if (data instanceof PluginObjectAdapter)
				data = ((PluginObjectAdapter) data).getObject();
			if (data instanceof Bundle && ((Bundle) data).getSymbolicName().equals(namespace))
				return items[i];
		}
		return null;
	}

	//	private void addBundleToTree(TreeItem item, Object data, Object object) {
	//		// TODO fix this method
	//		if (true) {
	//			fTreeViewer.refresh(data);
	//			updateItems(false);
	//			return;
	//		}
	//		TreeItem[] folders = item.getItems();
	//		for (int j = 0; j < folders.length; j++) {
	//			// make sure to check extensionsOnlyMode()
	//			// and add to root/proper extension if true
	//			IBundleFolder folder = (IBundleFolder)folders[j].getData();
	//			if (correctFolder(folder, object)) {
	//				fTreeViewer.add(folder, object);
	//				return;
	//			}
	//		}
	//		// folder not found - 1st extension - refresh bundle item
	//		fTreeViewer.refresh(data);
	//	}

	private String getNamespaceIdentifier(Object object) {
		if (object instanceof IExtensionPoint)
			return ((IExtensionPoint) object).getNamespaceIdentifier();
		if (object instanceof IExtension)
			return ((IExtension) object).getContributor().getName();
		return null;
	}

	private boolean correctFolder(IBundleFolder folder, Object child) {
		if (folder == null)
			return false;
		if (child instanceof IExtensionPoint)
			return folder.getFolderId() == IBundleFolder.F_EXTENSION_POINTS;
		if (child instanceof IExtension)
			return folder.getFolderId() == IBundleFolder.F_EXTENSIONS;
		return false;
	}

	private void removeFromTree(Object object) {
		String namespace = getNamespaceIdentifier(object);
		if (namespace == null)
			return;
		TreeItem[] bundles = fBrowser.getTreeItems();
		for (int i = 0; i < bundles.length; i++) {
			Object data = bundles[i].getData();
			Object adapted = null;
			if (data instanceof PluginObjectAdapter)
				adapted = ((PluginObjectAdapter) data).getObject();
			if (adapted instanceof Bundle && ((Bundle) adapted).getSymbolicName().equals(namespace)) {
				TreeItem[] folders = bundles[i].getItems();
				// TODO fix this method
				if (true) {
					fBrowser.refresh(data);
					fBrowser.updateItems(false);
					return;
				}
				for (int j = 0; j < folders.length; j++) {
					IBundleFolder folder = (IBundleFolder) folders[j].getData();
					if (correctFolder(folder, object)) {
						fBrowser.remove(object);
						return;
					}
				}
				// folder not found - 1st extension - refresh bundle item
				fBrowser.refresh(data);
			}
		}
	}

	protected Object findTreeBundleData(Object searchData) {
		final Tree tree = fBrowser.getUndisposedTree();
		if (tree == null)
			return null;

		Object data = null;
		TreeItem[] items = fBrowser.getTreeItems();
		if (items == null)
			return null;
		for (int i = 0; i < items.length; i++) {
			Object object = items[i].getData();
			data = object;
			if (object instanceof PluginObjectAdapter)
				object = ((PluginObjectAdapter) object).getObject();
			if (searchData.equals(object))
				return data;
		}
		return null;
	}

	void handleEvent(int changeType, Bundle bundle) {
		Object data = findTreeBundleData(bundle);
		switch (changeType) {
			case BundleEvent.INSTALLED :
				if (data == null)
					fBrowser.add(new PluginAdapter(bundle));
				break;
			case BundleEvent.UNINSTALLED :
				if (data != null)
					fBrowser.remove(data);
				break;
			case BundleEvent.STARTED :
			case BundleEvent.STOPPED :
			case BundleEvent.UPDATED :
			case BundleEvent.RESOLVED :
			case BundleEvent.STARTING :
			case BundleEvent.STOPPING :
			case BundleEvent.UNRESOLVED :
			case BundleEvent.LAZY_ACTIVATION :
				if (data != null)
					fBrowser.update(data);
				break;
		}
	}

	void handleExtOnlyEvent(int changeType, Bundle bundle) {
		switch (changeType) {
			case BundleEvent.INSTALLED :
			case BundleEvent.UNINSTALLED :
				// add/remove all extension points contributed by new bundle
				IExtensionPoint[] points = Platform.getExtensionRegistry().getExtensionPoints(bundle.getSymbolicName());
				for (int i = 0; i < points.length; i++) {
					Object pointData = findTreeBundleData(points[i]);
					if (pointData == null) {
						if (changeType == BundleEvent.INSTALLED)
							fBrowser.add(new ExtensionPointAdapter(points[i]));
						else
							// changeType == BundleEvent.UNINSTALLED
							fBrowser.remove(pointData);
					}
				}
				// add/remove all extensions contributed by new bundle
				IExtension[] extensions = Platform.getExtensionRegistry().getExtensions(bundle.getSymbolicName());
				for (int i = 0; i < extensions.length; i++) {
					String pointId = extensions[i].getExtensionPointUniqueIdentifier();
					if (changeType == BundleEvent.INSTALLED) {
						IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pointId);
						Object pointData = findTreeBundleData(point);
						if (pointData != null)
							fBrowser.add(pointData, new ExtensionAdapter(extensions[i]));
					} else { // changeType == BundleEvent.UNINSTALLED
						Object extensionData = findTreeBundleData(extensions[i]);
						if (extensionData != null)
							fBrowser.remove(extensionData);
					}
				}
				break;
			case BundleEvent.STARTED :
			case BundleEvent.STOPPED :
			case BundleEvent.UPDATED :
			case BundleEvent.RESOLVED :
			case BundleEvent.STARTING :
			case BundleEvent.STOPPING :
			case BundleEvent.UNRESOLVED :
			case BundleEvent.LAZY_ACTIVATION :
				// nothing needs to be done for any other cases
				break;
		}
	}

	public void serviceChanged(final ServiceEvent event) {
		final Tree tree = fBrowser.getUndisposedTree();
		if (tree == null)
			return;

		tree.getDisplay().asyncExec(new Runnable() {
			public void run() {
				handleServiceChangedEvent(event);
			}
		});

	}

	protected void handleServiceChangedEvent(ServiceEvent event) {
		ServiceReference ref = event.getServiceReference();
		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
			case ServiceEvent.UNREGISTERING :
				Bundle bundle = ref.getBundle();
				if (bundle == null)
					return;
				String name = bundle.getSymbolicName();
				if (name == null)
					return;
				TreeItem bundleItem = findBundleItem(name);
				PluginAdapter bundleAdapter = ((PluginAdapter) bundleItem.getData());
				Object[] folders = bundleAdapter.getChildren();

				for (int j = 0; j < folders.length; j++) {
					if (folders[j] instanceof IBundleFolder) {
						IBundleFolder folder = (IBundleFolder) folders[j];

						if (folder.getFolderId() == IBundleFolder.F_REGISTERED_SERVICES) {
							if ((event.getType() == ServiceEvent.REGISTERED) || (event.getType() == ServiceEvent.UNREGISTERING)) {
								((BundleFolder) folder).refresh(); // refresh model
								fBrowser.refresh(folder); // refresh view

								// refresh whole bundle in case there were folders added - they might have not existed if bundle had no service before
								fBrowser.refresh(bundleAdapter);
							}
						}
					}
				}

				break;
			case ServiceEvent.MODIFIED :
				break;
		}
	}

}
