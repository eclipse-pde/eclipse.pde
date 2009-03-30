/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.pde.internal.runtime.PDERuntimeMessages;

import java.util.Arrays;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.pde.internal.runtime.registry.model.*;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Constants;

public class RegistryBrowserLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

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
	private Image fPluginsImage;
	private Image fLocationImage;
	private Image fDisabledImage;
	private Image fExporterImage;
	private Image fImporterImage;
	private Image fServiceImage;
	private Image fPropertyImage;
	private Image fServicePropertyImage;
	private Image fFragmentImage;
	private Image fPackageImage;
	private RegistryBrowser fRegistryBrowser;

	public RegistryBrowserLabelProvider(RegistryBrowser browser) {
		fRegistryBrowser = browser;
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
		fPropertyImage = PDERuntimePluginImages.DESC_PROPERTY_OBJ.createImage();
		fPluginsImage = PDERuntimePluginImages.DESC_PLUGINS_OBJ.createImage();
		fFragmentImage = PDERuntimePluginImages.DESC_FRAGMENT_OBJ.createImage();
		fPackageImage = PDERuntimePluginImages.DESC_PACKAGE_OBJ.createImage();

		ImageDescriptor activePluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_RUN_CO}});
		fActivePluginImage = activePluginDesc.createImage();

		ImageDescriptor disabledPluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_ERROR_CO}});
		fDisabledImage = disabledPluginDesc.createImage();

		ImageDescriptor unresolvedPluginDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_ERROR_CO}});
		fUnresolvedPluginImage = unresolvedPluginDesc.createImage();

		ImageDescriptor exportedRequiresDesc = new OverlayIcon(PDERuntimePluginImages.DESC_REQ_PLUGIN_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_EXPORT_CO}});
		fExpReqPluginImage = exportedRequiresDesc.createImage();

		ImageDescriptor servicePropertyDesc = new OverlayIcon(PDERuntimePluginImages.DESC_PROPERTY_OBJ, new ImageDescriptor[][] {{PDERuntimePluginImages.DESC_DEFAULT_CO}});
		fServicePropertyImage = servicePropertyDesc.createImage();

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
		fPropertyImage.dispose();
		fServicePropertyImage.dispose();
		fPluginsImage.dispose();
		fFragmentImage.dispose();
		fPackageImage.dispose();
	}

	public Image getImage(Object element) {
		if (element instanceof Bundle) {
			Bundle bundle = (Bundle) element;

			if (bundle.getFragmentHost() != null)
				return fFragmentImage;

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

		if (element instanceof ServiceName) {
			return fServiceImage;
		}

		if (element instanceof ServiceRegistration) {
			return fPluginImage;
		}

		if (element instanceof Property) {
			Property property = (Property) element;
			// special handling for property objectClass
			if (property.getName().equals(Constants.OBJECTCLASS)) {
				return PDERuntimePluginImages.get(PDERuntimePluginImages.IMG_CLASS_OBJ);
			}
			// special handling for builtin service properties
			if (property.getName().startsWith("service.") || property.getName().startsWith("component.")) { //$NON-NLS-1$ //$NON-NLS-2$
				return fServicePropertyImage;
			}
			return fPropertyImage;
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
				case Folder.F_PROPERTIES :
					return fPropertyImage;
				case Folder.F_USING_BUNDLES :
					return fPluginsImage;
				case Folder.F_FRAGMENTS :
					return fPluginsImage;
				case Folder.F_EXPORTED_PACKAGES :
				case Folder.F_IMPORTED_PACKAGES :
					return fPackageImage;
			}
			return null;
		}

		if (element instanceof Extension)
			return fExtensionImage;

		if (element instanceof ExtensionPoint)
			return fExtensionPointImage;

		if (element instanceof BundlePrerequisite) {
			BundlePrerequisite prereq = (BundlePrerequisite) element;

			if (prereq.isPackage())
				return fPackageImage;

			return prereq.isExported() ? fExpReqPluginImage : fReqPluginImage;
		}

		if (element instanceof BundleLibrary)
			return fLibraryImage;

		if (element instanceof ConfigurationElement)
			return fGenericTagImage;

		if (element instanceof Attribute) {
			Attribute attr = (Attribute) element;
			if (Attribute.F_LOCATION.equals(attr.getName())) {
				return fLocationImage;
			}
			if (Attribute.F_BUNDLE.equals(attr.getName())) {
				return fPluginImage;
			}
			return fGenericAttrImage;
		}

		return null;
	}

	protected StyledString getStyledText(Object element) {

		if (element instanceof Bundle) {
			Bundle bundle = ((Bundle) element);

			StyledString sb = new StyledString(bundle.getSymbolicName());
			String version = bundle.getVersion();
			if (version != null) {
				sb.append(" (", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$ 
				sb.append(version, StyledString.DECORATIONS_STYLER);
				sb.append(")", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			}
			String host = bundle.getFragmentHost();
			if (host != null) {
				sb.append(" [", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				sb.append(host, StyledString.QUALIFIER_STYLER);
				sb.append("]", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			}
			return sb;
		}

		if (element instanceof ServiceRegistration) {
			ServiceRegistration ref = (ServiceRegistration) element;
			String identifier = " (id=" + ref.getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$

			StyledString ss = new StyledString();
			if (fRegistryBrowser.getGroupBy() == RegistryBrowser.BUNDLES) {
				String[] classes = ref.getName().getClasses();
				ss.append(Arrays.asList(classes).toString());

			} else {
				ss.append(PDERuntimeMessages.RegistryBrowserLabelProvider_RegisteredBy);
				ss.append(ref.getBundle());
			}

			ss.append(identifier, StyledString.DECORATIONS_STYLER);

			return ss;
		}

		if (element instanceof ServiceName) {
			return new StyledString(Arrays.asList(((ServiceName) element).getClasses()).toString());
		}

		if (element instanceof Folder) {
			String text = null;
			switch (((Folder) element).getId()) {
				case Folder.F_IMPORTS :
					text = PDERuntimeMessages.RegistryView_folders_imports;
					break;
				case Folder.F_IMPORTED_PACKAGES :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_ImportedPackages;
					break;
				case Folder.F_EXPORTED_PACKAGES :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_ExportedPackages;
					break;
				case Folder.F_LIBRARIES :
					text = PDERuntimeMessages.RegistryView_folders_libraries;
					break;
				case Folder.F_EXTENSION_POINTS :
					text = PDERuntimeMessages.RegistryView_folders_extensionPoints;
					break;
				case Folder.F_EXTENSIONS :
					text = PDERuntimeMessages.RegistryView_folders_extensions;
					break;
				case Folder.F_REGISTERED_SERVICES :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_registeredServices;
					break;
				case Folder.F_SERVICES_IN_USE :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_usedServices;
					break;
				case Folder.F_PROPERTIES :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_Properties;
					break;
				case Folder.F_USING_BUNDLES :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_UsingBundles;
					break;
				case Folder.F_FRAGMENTS :
					text = PDERuntimeMessages.RegistryBrowserLabelProvider_Fragments;
					break;
			}

			if (text != null) {
				return new StyledString(text);
			}
		}
		if (element instanceof Extension) {
			if (((RegistryBrowserContentProvider) fRegistryBrowser.getAdapter(IContentProvider.class)).isInExtensionSet) {
				Extension extension = ((Extension) element);

				StyledString ss = new StyledString(extension.getExtensionPointUniqueIdentifier());
				String name = extension.getLabel();
				if (name != null && name.length() > 0) {
					ss.append("[ ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					ss.append(name, StyledString.QUALIFIER_STYLER);
					ss.append(']', StyledString.QUALIFIER_STYLER);
				}

				return ss;
			}

			String contributor = ((Extension) element).getNamespaceIdentifier();
			return new StyledString(NLS.bind(PDERuntimeMessages.RegistryBrowserLabelProvider_contributedBy, contributor));

		}
		if (element instanceof ExtensionPoint) {
			ExtensionPoint extPoint = (ExtensionPoint) element;

			StyledString ss = new StyledString(extPoint.getUniqueIdentifier());
			String name = extPoint.getLabel();
			if (name != null && name.length() > 0) {
				ss.append(" [", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				ss.append(name, StyledString.QUALIFIER_STYLER);
				ss.append(']', StyledString.QUALIFIER_STYLER);
			}

			return ss;
		}
		if (element instanceof BundlePrerequisite) {
			BundlePrerequisite prereq = (BundlePrerequisite) element;

			StyledString ss = new StyledString(prereq.getName());

			String version = prereq.getVersion();
			if (version != null) {
				if (Character.isDigit(version.charAt(0)))
					version = '(' + version + ')';
				ss.append(' ').append(version, StyledString.DECORATIONS_STYLER);
			}

			return ss;
		}

		if (element instanceof BundleLibrary) {
			return new StyledString(((BundleLibrary) element).getLibrary());
		}

		if (element instanceof ConfigurationElement) {
			return new StyledString(((ConfigurationElement) element).getName());
		}
		if (element instanceof Attribute) {
			Attribute attribute = (Attribute) element;
			if (Attribute.F_BUNDLE.equals(attribute.getName())) {
				return new StyledString(attribute.getValue());
			}

			return new StyledString(attribute.getName() + " = " + attribute.getValue()); //$NON-NLS-1$
		}
		if (element instanceof Property) {
			Property property = (Property) element;
			return new StyledString(property.getName() + " = " + property.getValue()); //$NON-NLS-1$
		}

		return new StyledString(element == null ? "" : element.toString()); //$NON-NLS-1$
	}

	public void update(ViewerCell cell) {
		StyledString string = getStyledText(cell.getElement());
		cell.setText(string.getString());
		cell.setStyleRanges(string.getStyleRanges());
		cell.setImage(getImage(cell.getElement()));
		super.update(cell);
	}

	public String getText(Object element) {
		return getStyledText(element).getString();
	}
}
