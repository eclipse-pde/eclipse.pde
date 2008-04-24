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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class RegistryBrowserLabelProvider extends LabelProvider {

	private Image fPluginImage;
	private Image fActivePluginImage;
	private Image fUnresolvedPluginImage;
	private Image fLibraryImage;
	private Image fRuntimeImage;
	private Image fGenericTagImage;
	private Image fGenericAttrImage;
	private Image fExtensionImage;
	private Image fExtensionsImage;
	private Image fExtensionPointImage;
	private Image fExtensionPointsImage;
	private Image fRequiresImage;
	private Image fExpReqPluginImage;
	private Image fReqPluginImage;
	private Image fLocationImage;
	private Image fDisabledImage;
	private Image fExporterImage;
	private Image fImporterImage;
	private Image fServiceImage;
	private TreeViewer fViewer;

	public RegistryBrowserLabelProvider(TreeViewer viewer) {
		fViewer = viewer;
		fPluginImage = PDERuntimePluginImages.DESC_PLUGIN_OBJ.createImage();
		fReqPluginImage = PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
		fExtensionPointImage = PDERuntimePluginImages.DESC_EXT_POINT_OBJ.createImage();
		fExtensionPointsImage = PDERuntimePluginImages.DESC_EXT_POINTS_OBJ.createImage();
		fExtensionImage = PDERuntimePluginImages.DESC_EXTENSION_OBJ.createImage();
		fExtensionsImage = PDERuntimePluginImages.DESC_EXTENSIONS_OBJ.createImage();
		fRequiresImage = PDERuntimePluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fLibraryImage = PDERuntimePluginImages.DESC_JAVA_LIB_OBJ.createImage();
		fGenericTagImage = PDERuntimePluginImages.DESC_GENERIC_XML_OBJ.createImage();
		fGenericAttrImage = PDERuntimePluginImages.DESC_ATTR_XML_OBJ.createImage();
		fRuntimeImage = PDERuntimePluginImages.DESC_RUNTIME_OBJ.createImage();
		fLocationImage = PDERuntimePluginImages.DESC_LOCATION.createImage();
		fExporterImage = PDERuntimePluginImages.DESC_EXP_OBJ.createImage();
		fImporterImage = PDERuntimePluginImages.DESC_IMP_OBJ.createImage();
		fServiceImage = PDERuntimePluginImages.DESC_SERVICE_OBJ.createImage();

		ImageDescriptor activePluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_RUN_CO}});
		fActivePluginImage = activePluginDesc.createImage();

		ImageDescriptor disabledPluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_ERROR_CO}});
		fDisabledImage = disabledPluginDesc.createImage();

		ImageDescriptor unresolvedPluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_ERROR_CO}});
		fUnresolvedPluginImage = unresolvedPluginDesc.createImage();

		ImageDescriptor exportedRequiresDesc = new OverlayIcon(PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_EXPORT_CO}});
		fExpReqPluginImage = exportedRequiresDesc.createImage();

	}

	public void dispose() {
		fPluginImage.dispose();
		fActivePluginImage.dispose();
		fUnresolvedPluginImage.dispose();
		fReqPluginImage.dispose();
		fExtensionPointImage.dispose();
		fExtensionPointsImage.dispose();
		fExtensionImage.dispose();
		fExtensionsImage.dispose();
		fRequiresImage.dispose();
		fExpReqPluginImage.dispose();
		fLibraryImage.dispose();
		fGenericTagImage.dispose();
		fGenericAttrImage.dispose();
		fRuntimeImage.dispose();
		fLocationImage.dispose();
		fDisabledImage.dispose();
		fImporterImage.dispose();
		fExporterImage.dispose();
		fServiceImage.dispose();
	}

	public Image getImage(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();

		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;

			// check if bundle is disabled
			PlatformAdmin plaformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
			State state = plaformAdmin.getState(false);

			BundleDescription description = state.getBundle(bundle.getBundleId());
			if ((state.getDisabledInfos(description)).length > 0)
				return fDisabledImage;

			switch (bundle.getState()) {
				case Bundle.ACTIVE :
					return fActivePluginImage;
				case Bundle.UNINSTALLED :
					return fUnresolvedPluginImage;
				case Bundle.INSTALLED :
					if ((state.getDisabledInfos(description)).length > 0)
						return fUnresolvedPluginImage;
				default :
					return fPluginImage;
			}
		}

		if (element instanceof ServiceReference) {
			return fServiceImage;
		}

		if (element instanceof IBundleFolder) {
			int id = ((IBundleFolder) element).getFolderId();
			switch (id) {
				case IBundleFolder.F_EXTENSIONS :
					return fExtensionsImage;
				case IBundleFolder.F_EXTENSION_POINTS :
					return fExtensionPointsImage;
				case IBundleFolder.F_IMPORTS :
					return fRequiresImage;
				case IBundleFolder.F_LIBRARIES :
					return fRuntimeImage;
				case IBundleFolder.F_LOCATION :
					return fLocationImage;
				case IBundleFolder.F_REGISTERED_SERVICES :
					return fExporterImage;
				case IBundleFolder.F_SERVICES_IN_USE :
					return fImporterImage;
			}
			return null;
		}
		if (element instanceof IExtension)
			return fExtensionImage;

		if (element instanceof IExtensionPoint)
			return fExtensionPointImage;

		if (element instanceof IBundlePrerequisite)
			return ((IBundlePrerequisite) element).isExported() ? fExpReqPluginImage : fReqPluginImage;

		if (element instanceof IBundleLibrary)
			return fLibraryImage;

		if (element instanceof IConfigurationElement)
			return fGenericTagImage;

		if (element instanceof IConfigurationAttribute)
			return fGenericAttrImage;

		return null;
	}

	public String getText(Object element) {
		if (element instanceof PluginObjectAdapter)
			element = ((PluginObjectAdapter) element).getObject();
		if (element instanceof Bundle) {
			String id = ((Bundle) element).getSymbolicName();
			String version = (String) ((Bundle) element).getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
			if (version == null)
				return id;
			return id + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (element instanceof ServiceReference) {
			ServiceReference ref = (ServiceReference) element;
			String[] classes = (String[]) ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
			Long id = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
			String identifier = " (id=" + id.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return Arrays.asList(classes).toString().concat(identifier);
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
				case IBundleFolder.F_REGISTERED_SERVICES :
					return PDERuntimeMessages.RegistryBrowserLabelProvider_registeredServices;
				case IBundleFolder.F_SERVICES_IN_USE :
					return PDERuntimeMessages.RegistryBrowserLabelProvider_usedServices;
				case IBundleFolder.F_LOCATION :
					Bundle bundle = ((IBundleFolder) element).getBundle();
					URL bundleEntry = bundle.getEntry("/"); //$NON-NLS-1$
					try {
						bundleEntry = FileLocator.resolve(bundleEntry);
					} catch (IOException e) { // do nothing
					}
					IPath path = new Path(bundleEntry.getFile());
					String pathString = path.removeTrailingSeparator().toOSString();
					if (pathString.startsWith("file:")) //$NON-NLS-1$
						pathString = pathString.substring(5);
					if (pathString.endsWith("!")) //$NON-NLS-1$
						pathString = pathString.substring(0, pathString.length() - 1);
					return pathString;
			}
		}
		if (element instanceof IExtension) {
			if (((RegistryBrowserContentProvider) fViewer.getContentProvider()).isInExtensionSet) {
				String name = ((IExtension) element).getLabel();
				String id = ((IExtension) element).getExtensionPointUniqueIdentifier();
				if (name != null && name.length() > 0)
					return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
				return id;
			}

			String contributor = ((IExtension) element).getNamespaceIdentifier();
			return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_contributedBy, contributor);

		}
		if (element instanceof IExtensionPoint) {
			String id = ((IExtensionPoint) element).getUniqueIdentifier();
			String name = ((IExtensionPoint) element).getLabel();
			if (name != null && name.length() > 0)
				return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
			return id;
		}
		if (element instanceof IBundlePrerequisite)
			return ((IBundlePrerequisite) element).getLabel();

		if (element instanceof IBundleLibrary)
			return ((IBundleLibrary) element).getLibrary();

		if (element instanceof IConfigurationElement) {
			String label = ((IConfigurationElement) element).getAttribute("label"); //$NON-NLS-1$
			if (label == null)
				label = ((IConfigurationElement) element).getName();

			if (label == null)
				label = ((IConfigurationElement) element).getAttribute("name"); //$NON-NLS-1$

			if (label == null && ((IConfigurationElement) element).getAttribute("id") != null) { //$NON-NLS-1$
				String[] labelSplit = ((IConfigurationElement) element).getAttribute("id").split("\\."); //$NON-NLS-1$ //$NON-NLS-2$
				label = labelSplit.length == 0 ? null : labelSplit[labelSplit.length - 1];
			}
			return label;
		}
		if (element instanceof IConfigurationAttribute)
			return ((IConfigurationAttribute) element).getLabel();

		return super.getText(element);
	}
}
