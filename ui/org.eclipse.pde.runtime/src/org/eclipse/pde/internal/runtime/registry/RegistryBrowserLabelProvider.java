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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.OverlayIcon;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

public class RegistryBrowserLabelProvider extends LabelProvider {
	
	private Image pluginImage;
	private Image activePluginImage;
	private Image unresolvedPluginImage;
	private Image libraryImage;
	private Image runtimeImage;
	private Image genericTagImage;
	private Image genericAttrImage;
	private Image extensionImage;
	private Image extensionsImage;
	private Image extensionPointImage;
	private Image extensionPointsImage;
	private Image requiresImage;
	private Image expReqPluginImage;
	private Image reqPluginImage;
	private Image locationImage;
	private TreeViewer viewer;
	
	public RegistryBrowserLabelProvider(TreeViewer viewer) {
		this.viewer = viewer;
		pluginImage = PDERuntimePluginImages.DESC_PLUGIN_OBJ.createImage();
		reqPluginImage = PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
		extensionPointImage = PDERuntimePluginImages.DESC_EXT_POINT_OBJ.createImage();
		extensionPointsImage = PDERuntimePluginImages.DESC_EXT_POINTS_OBJ.createImage();
		extensionImage = PDERuntimePluginImages.DESC_EXTENSION_OBJ.createImage();
		extensionsImage = PDERuntimePluginImages.DESC_EXTENSIONS_OBJ.createImage();
		requiresImage = PDERuntimePluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		libraryImage = PDERuntimePluginImages.DESC_JAVA_LIB_OBJ.createImage();
		genericTagImage = PDERuntimePluginImages.DESC_GENERIC_XML_OBJ.createImage();
		genericAttrImage = PDERuntimePluginImages.DESC_ATTR_XML_OBJ.createImage();
		runtimeImage = PDERuntimePluginImages.DESC_RUNTIME_OBJ.createImage();
		locationImage = PDERuntimePluginImages.DESC_LOCATION.createImage();
		
		ImageDescriptor activePluginDesc =
			new OverlayIcon(
				PDERuntimePluginImages.DESC_PLUGIN_OBJ,
				new ImageDescriptor[][] {{ PDERuntimePluginImages.DESC_RUN_CO }});
		activePluginImage = activePluginDesc.createImage();
		
		ImageDescriptor unresolvedPluginDesc =
			new OverlayIcon(
				PDERuntimePluginImages.DESC_PLUGIN_OBJ,
				new ImageDescriptor[][] {{ PDERuntimePluginImages.DESC_ERROR_CO }});
		unresolvedPluginImage = unresolvedPluginDesc.createImage();
		
		
		ImageDescriptor exportedRequiresDesc = 
			new OverlayIcon(
					PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ,
					new ImageDescriptor[][] {{ PDERuntimePluginImages.DESC_EXPORT_CO }});
		expReqPluginImage = exportedRequiresDesc.createImage();
		
	}
	public void dispose() {
		pluginImage.dispose();
		activePluginImage.dispose();
		unresolvedPluginImage.dispose();
		reqPluginImage.dispose();
		extensionPointImage.dispose();
		extensionPointsImage.dispose();
		extensionImage.dispose();
		extensionsImage.dispose();
		requiresImage.dispose();
		expReqPluginImage.dispose();
		libraryImage.dispose();
		genericTagImage.dispose();
		genericAttrImage.dispose();
		runtimeImage.dispose();
		locationImage.dispose();
	}
	public Image getImage(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		
		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;
			switch (bundle.getState()) {
			case Bundle.ACTIVE:
				return activePluginImage;
			case Bundle.RESOLVED:
				return pluginImage;
			default:
				return unresolvedPluginImage;
			}
		}
		if (element instanceof IBundleFolder) {
			int id = ((IBundleFolder) element).getFolderId();
			switch (id) {
				case IBundleFolder.F_EXTENSIONS:
					return extensionsImage;
				case IBundleFolder.F_EXTENSION_POINTS:
					return extensionPointsImage;
				case IBundleFolder.F_IMPORTS:
					return requiresImage;
				case IBundleFolder.F_LIBRARIES:
					return runtimeImage;
				case IBundleFolder.F_LOCATION:
					return locationImage;
			}
			return null;
		}
		if (element instanceof IExtension)
			return extensionImage;
		
		if (element instanceof IExtensionPoint)
			return extensionPointImage;
		
		if (element instanceof IBundlePrerequisite)
			return ((IBundlePrerequisite)element).isExported() ?
					expReqPluginImage : reqPluginImage;
		
		if (element instanceof IBundleLibrary)
			return libraryImage;
		
		if (element instanceof IConfigurationElement)
			return genericTagImage;
		
		if (element instanceof IConfigurationAttribute)
			return genericAttrImage;
		
		return null;
	}
	public String getText(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element instanceof Bundle) {
			String id = ((Bundle)element).getSymbolicName();
			String version = (String)((Bundle)element).getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
			if (version == null)
				return id;
			return id + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (element instanceof IBundleFolder) {
			switch (((IBundleFolder) element).getFolderId()) {
				case IBundleFolder.F_IMPORTS :
					return PDERuntimeMessages.RegistryView_folders_imports;
				case IBundleFolder.F_LIBRARIES :
					return PDERuntimeMessages.RegistryView_folders_libraries;
				case IBundleFolder.F_EXTENSION_POINTS :
					return PDERuntimeMessages.RegistryView_folders_extensionPoints;
				case IBundleFolder.F_EXTENSIONS :
					return PDERuntimeMessages.RegistryView_folders_extensions;
				case IBundleFolder.F_LOCATION:
					Bundle bundle = ((IBundleFolder) element).getBundle();
					URL bundleEntry = bundle.getEntry("/"); //$NON-NLS-1$
					try {
						bundleEntry = FileLocator.resolve(bundleEntry);
					} catch (IOException e) {
					}
					File file = new File(bundleEntry.getFile());
					String path = file.getAbsolutePath();
					return path.endsWith("!") ? path.substring(0, path.length() - 1) : path;
			}
		}
		if (element instanceof IExtension) {
			if (((RegistryBrowserContentProvider)viewer.getContentProvider()).isInExtensionSet) {
				String name = ((IExtension) element).getLabel();
				String id = ((IExtension) element).getExtensionPointUniqueIdentifier();
				if (name != null && name.length() > 0)
					return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
				return id;
			}

			String contributor = ((IExtension) element).getNamespaceIdentifier();
			return NLS.bind("contributed by: {0}", contributor); //$NON-NLS-1$
			
		}
		if (element instanceof IExtensionPoint) {
			String id = ((IExtensionPoint)element).getUniqueIdentifier();
			String name = ((IExtensionPoint)element).getLabel();
			if (name != null && name.length() > 0)
				return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
			return id;
		}
		if (element instanceof IBundlePrerequisite)
			return ((IBundlePrerequisite) element).getLabel();
		
		if (element instanceof IBundleLibrary)
			return ((IBundleLibrary)element).getLibrary();
		
		if (element instanceof IConfigurationElement) {
			String label = ((IConfigurationElement) element).getAttribute("label"); //$NON-NLS-1$
			if (label == null)
				label = ((IConfigurationElement) element).getName();
			
			if (label == null)
				label = ((IConfigurationElement) element).getAttribute("name"); //$NON-NLS-1$
			
			if (label == null && ((IConfigurationElement) element).getAttribute("id") != null){ //$NON-NLS-1$
				String[] labelSplit = ((IConfigurationElement) element).getAttribute("id").split("\\."); //$NON-NLS-1$ //$NON-NLS-2$
				label = labelSplit.length == 0 ? null: labelSplit[labelSplit.length-1];
			} 
			return label;
		}
		if (element instanceof IConfigurationAttribute)
			return ((IConfigurationAttribute)element).getLabel();
		
		return super.getText(element);
	}
}
