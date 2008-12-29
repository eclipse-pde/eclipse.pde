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

import java.util.Arrays;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.registry.model.*;
import org.eclipse.swt.graphics.Image;

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
		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;

			if (!bundle.isEnabled())
				return fDisabledImage;

			switch (bundle.getState()) {
				case Bundle.ACTIVE :
					return fActivePluginImage;
				case Bundle.UNINSTALLED :
					return fUnresolvedPluginImage;
				case Bundle.INSTALLED :
					if (!bundle.isEnabled())
						return fUnresolvedPluginImage;
				default :
					return fPluginImage;
			}
		}

		if (element instanceof ServiceRegistration) {
			return fServiceImage;
		}

		if (element instanceof Folder) {
			int id = ((Folder) element).getId();
			switch (id) {
				case Folder.F_EXTENSIONS :
					return fExtensionsImage;
				case Folder.F_EXTENSION_POINTS :
					return fExtensionPointsImage;
				case Folder.F_IMPORTS :
					return fRequiresImage;
				case Folder.F_LIBRARIES :
					return fRuntimeImage;
				case Folder.F_REGISTERED_SERVICES :
					return fExporterImage;
				case Folder.F_SERVICES_IN_USE :
					return fImporterImage;
			}
			return null;
		}
		if (element instanceof Extension)
			return fExtensionImage;

		if (element instanceof ExtensionPoint)
			return fExtensionPointImage;

		if (element instanceof BundlePrerequisite)
			return ((BundlePrerequisite) element).isExported() ? fExpReqPluginImage : fReqPluginImage;

		if (element instanceof BundleLibrary)
			return fLibraryImage;

		if (element instanceof ConfigurationElement)
			return fGenericTagImage;

		if (element instanceof Attribute) {
			Attribute attr = (Attribute) element;
			if (Attribute.F_LOCATION.equals(attr.getName())) {
				return fLocationImage;
			}
			return fGenericAttrImage;
		}

		return null;
	}

	public String getText(Object element) {
		if (element instanceof Bundle) {
			String id = ((Bundle) element).getSymbolicName();
			String version = ((Bundle) element).getVersion();
			if (version == null)
				return id;
			return id + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (element instanceof ServiceRegistration) {
			ServiceRegistration ref = (ServiceRegistration) element;
			String[] classes = ref.getClasses();
			String identifier = " (id=" + ref.getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return Arrays.asList(classes).toString().concat(identifier);
		}
		if (element instanceof Folder) {
			switch (((Folder) element).getId()) {
				case Folder.F_IMPORTS :
					return PDERuntimeMessages.RegistryView_folders_imports;
				case Folder.F_LIBRARIES :
					return PDERuntimeMessages.RegistryView_folders_libraries;
				case Folder.F_EXTENSION_POINTS :
					return PDERuntimeMessages.RegistryView_folders_extensionPoints;
				case Folder.F_EXTENSIONS :
					return PDERuntimeMessages.RegistryView_folders_extensions;
				case Folder.F_REGISTERED_SERVICES :
					return PDERuntimeMessages.RegistryBrowserLabelProvider_registeredServices;
				case Folder.F_SERVICES_IN_USE :
					return PDERuntimeMessages.RegistryBrowserLabelProvider_usedServices;
			}
		}
		if (element instanceof Extension) {
			if (((RegistryBrowserContentProvider) fViewer.getContentProvider()).isInExtensionSet) {
				String name = ((Extension) element).getLabel();
				String id = ((Extension) element).getExtensionPointUniqueIdentifier();
				if (name != null && name.length() > 0)
					return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
				return id;
			}

			String contributor = ((Extension) element).getNamespaceIdentifier();
			return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_contributedBy, contributor);

		}
		if (element instanceof ExtensionPoint) {
			String id = ((ExtensionPoint) element).getUniqueIdentifier();
			String name = ((ExtensionPoint) element).getLabel();
			if (name != null && name.length() > 0)
				return NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_nameIdBind, id, name);
			return id;
		}
		if (element instanceof BundlePrerequisite) {
			BundlePrerequisite prereq = (BundlePrerequisite) element;
			String version = prereq.getVersion();
			if (version != null) {
				if (Character.isDigit(version.charAt(0)))
					version = '(' + version + ')';
				return prereq.getName() + ' ' + version;
			}

			return prereq.getName();
		}

		if (element instanceof BundleLibrary)
			return ((BundleLibrary) element).getLibrary();

		if (element instanceof ConfigurationElement) {
			return ((ConfigurationElement) element).getName();
		}
		if (element instanceof Attribute) {
			Attribute attribute = (Attribute) element;
			return attribute.getName() + " = " + attribute.getValue(); //$NON-NLS-1$
		}

		return super.getText(element);
	}
}
