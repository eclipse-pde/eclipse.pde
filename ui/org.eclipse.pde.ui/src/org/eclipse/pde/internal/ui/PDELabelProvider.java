/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 218618
 *     Brian de Alwis (MTI) - bug 429420
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.util.Locale;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.ischema.ISchemaRepeatable;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

public class PDELabelProvider extends SharedLabelProvider {
	private static final String SYSTEM_BUNDLE = "system.bundle"; //$NON-NLS-1$

	public PDELabelProvider() {
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof IPluginModelBase) {
			return getObjectText(((IPluginModelBase) obj).getPluginBase());
		}
		if (obj instanceof IPluginBase) {
			return getObjectText((IPluginBase) obj);
		}
		if (obj instanceof ImportObject) {
			return getObjectText((ImportObject) obj);
		}
		if (obj instanceof IProductPlugin) {
			return getObjectText((IProductPlugin) obj);
		}
		if (obj instanceof BundleDescription) {
			return getObjectText((BundleDescription) obj);
		}
		if (obj instanceof IPluginImport) {
			return getObjectText((IPluginImport) obj);
		}
		if (obj instanceof IPluginLibrary) {
			return getObjectText((IPluginLibrary) obj);
		}
		if (obj instanceof IPluginExtension) {
			return getObjectText((IPluginExtension) obj);
		}
		if (obj instanceof IPluginExtensionPoint) {
			return getObjectText((IPluginExtensionPoint) obj);
		}
		if (obj instanceof NamedElement) {
			return ((NamedElement) obj).getLabel();
		}
		if (obj instanceof ISchemaObject) {
			return getObjectText((ISchemaObject) obj);
		}
		if (obj instanceof FeaturePlugin) {
			return getObjectText((FeaturePlugin) obj);
		}
		if (obj instanceof FeatureImport) {
			return getObjectText((FeatureImport) obj);
		}
		if (obj instanceof IFeatureModel) {
			return getObjectText((IFeatureModel) obj);
		}
		if (obj instanceof FeatureChild) {
			return getObjectText((FeatureChild) obj);
		}
		if (obj instanceof IProductFeature) {
			return getObjectText((IProductFeature) obj);
		}
		if (obj instanceof IProductModel) {
			return getObjectText((IProductModel) obj);
		}
		if (obj instanceof ISiteFeature) {
			return getObjectText((ISiteFeature) obj);
		}
		if (obj instanceof ISiteArchive) {
			return getObjectText((ISiteArchive) obj);
		}
		if (obj instanceof ISiteCategoryDefinition) {
			return getObjectText((ISiteCategoryDefinition) obj);
		}
		if (obj instanceof ISiteCategory) {
			return getObjectText((ISiteCategory) obj);
		}
		if (obj instanceof IBuildEntry) {
			return getObjectText((IBuildEntry) obj);
		}
		if (obj instanceof PackageObject) {
			return getObjectText((PackageObject) obj);
		}
		if (obj instanceof ExecutionEnvironment) {
			return getObjectText((ExecutionEnvironment) obj);
		}
		if (obj instanceof Locale) {
			return getObjectText((Locale) obj);
		}
		if (obj instanceof IStatus) {
			return getObjectText((IStatus) obj);
		}
		return super.getText(obj);
	}

	private String getObjectText(ExecutionEnvironment environment) {
		return preventNull(environment.getName());
	}

	public String getObjectText(IPluginBase pluginBase) {
		String name = isFullNameModeEnabled() ? pluginBase.getTranslatedName() : pluginBase.getId();
		name = preventNull(name);
		String version = pluginBase.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(pluginBase.getVersion());
		else
			text = name;
		if (SYSTEM_BUNDLE.equals(pluginBase.getId())) {
			text += getSystemBundleInfo();
		}
		if (pluginBase.getModel() != null && !pluginBase.getModel().isInSync())
			text += " " + PDEUIMessages.PluginModelManager_outOfSync; //$NON-NLS-1$
		return text;
	}

	private String getSystemBundleInfo() {
		IPluginBase systemBundle = PluginRegistry.findModel(SYSTEM_BUNDLE).getPluginBase();
		return NLS.bind(" [{0}]", systemBundle.getId()); //$NON-NLS-1$
	}

	private String preventNull(String text) {
		return text != null ? text : ""; //$NON-NLS-1$
	}

	public String getObjectText(IPluginExtension extension) {
		return preventNull(isFullNameModeEnabled() ? extension.getTranslatedName() : extension.getPoint());
	}

	public String getObjectText(IPluginExtensionPoint point) {
		return preventNull(isFullNameModeEnabled() ? point.getTranslatedName() : point.getId());
	}

	public String getObjectText(ImportObject obj) {
		String version = obj.getImport().getVersion();
		if (version != null && version.length() > 0)
			version = formatVersion(version);

		String text = isFullNameModeEnabled() ? obj.toString() : preventNull(obj.getId());
		if (SYSTEM_BUNDLE.equals(obj.getId()))
			return text + getSystemBundleInfo();
		return version == null || version.length() == 0 ? text : text + " " + version; //$NON-NLS-1$
	}

	public String getObjectText(IProductPlugin obj) {
		// TODO, should we just get the model and call the proper method?
		String name = obj.getId();
		String version = obj.getVersion();
		String text;
		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(obj.getVersion());
		else
			text = name;
		return preventNull(text);
	}

	public String getObjectText(BundleDescription bundle) {
		String id = bundle.getSymbolicName();
		if (isFullNameModeEnabled()) {
			IPluginModelBase model = PluginRegistry.findModel((Resource) bundle);
			if (model != null) {
				return model.getPluginBase().getTranslatedName();
			}
			return id != null ? id : "?"; //$NON-NLS-1$
		}
		return preventNull(id);
	}

	public String getObjectText(IPluginImport obj) {
		if (isFullNameModeEnabled()) {
			String id = obj.getId();
			IPluginModelBase model = PluginRegistry.findModel(obj.getId());
			if (model != null) {
				return model.getPluginBase().getTranslatedName();
			}
			return id != null ? id : "?"; //$NON-NLS-1$
		}
		return preventNull(obj.getId());
	}

	public String getObjectText(IBuildEntry obj) {
		return obj.getName();
	}

	public String getObjectText(IPluginLibrary obj) {
		return preventNull(obj.getName());
	}

	public String getObjectText(ISchemaObject obj) {
		StringBuilder text = new StringBuilder(obj.getName());
		if (obj instanceof ISchemaRepeatable rso) {
			boolean unbounded = rso.getMaxOccurs() == Integer.MAX_VALUE;
			int maxOccurs = rso.getMaxOccurs();
			int minOccurs = rso.getMinOccurs();
			if (maxOccurs != 1 || minOccurs != 1) {
				if (isRTL() && SWTUtil.isBidi())
					text.append('\u200f');
				text.append(" ("); //$NON-NLS-1$
				text.append(minOccurs);
				text.append(" - "); //$NON-NLS-1$
				if (unbounded)
					text.append('*');
				else
					text.append(maxOccurs);
				text.append(')');
			}
		}

		return text.toString();
	}

	private String getObjectText(Locale obj) {
		String country = " (" + obj.getDisplayCountry() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		return obj.getDisplayLanguage() + (" ()".equals(country) ? "" : country); //$NON-NLS-1$//$NON-NLS-2$
	}

	public String getObjectText(FeaturePlugin obj) {
		String name = isFullNameModeEnabled() ? obj.getLabel() : obj.getId();
		String version = obj.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(version);
		else
			text = name;
		return preventNull(text);
	}

	public String getObjectText(FeatureImport obj) {
		int type = obj.getType();
		if (type == IFeatureImport.PLUGIN) {
			IPlugin plugin = obj.getPlugin();
			if (plugin != null && isFullNameModeEnabled()) {
				return preventNull(plugin.getTranslatedName());
			}
		} else if (type == IFeatureImport.FEATURE) {
			IFeature feature = obj.getFeature();
			if (feature != null && isFullNameModeEnabled()) {
				return preventNull(feature.getTranslatableLabel());
			}
		}
		return preventNull(obj.getId());
	}

	public String getObjectText(IFeatureModel obj, boolean showVersion) {
		IFeature feature = obj.getFeature();
		String name = (isFullNameModeEnabled()) ? feature.getTranslatableLabel() : feature.getId();
		if (!showVersion)
			return preventNull(name);
		return preventNull(name) + ' ' + formatVersion(feature.getVersion());

	}

	public String getObjectText(IFeatureModel obj) {
		return getObjectText(obj, true);
	}

	public String getObjectText(FeatureChild obj) {
		return preventNull(obj.getId()) + ' ' + formatVersion(obj.getVersion());
	}

	public String getObjectText(IProductFeature obj) {
		String name = preventNull(obj.getId());
		if (obj.isRootInstallMode()) {
			name += " (root)"; //$NON-NLS-1$
		}
		if (VersionUtil.isEmptyVersion(obj.getVersion()))
			return name;
		return name + ' ' + formatVersion(obj.getVersion());
	}

	private String getObjectText(IProductModel obj) {
		IProduct product = obj.getProduct();
		String name = preventNull(product.getId());
		if (name.isEmpty()) {
			name = preventNull(product.getName());
		}
		if (VersionUtil.isEmptyVersion(product.getVersion())) {
			return name;
		}
		return name + ' ' + formatVersion(product.getVersion());
	}

	public String getObjectText(ISiteFeature obj) {
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModel(obj.getId(), obj.getVersion() != null ? obj.getVersion() : ICoreConstants.DEFAULT_VERSION);
		if (model != null)
			return getObjectText(model);
		String url = obj.getURL();
		if (url != null)
			return url;
		return preventNull(obj.getId());
	}

	public String getObjectText(ISiteBundle obj) {
		IPluginModelBase modelBase = PluginRegistry.findModel(obj.getId(), obj.getVersion(), IMatchRules.COMPATIBLE, null);
		if (modelBase != null)
			return getObjectText(modelBase.getPluginBase());
		return preventNull(obj.getId());
	}

	public String getObjectText(ISiteArchive obj) {
		return preventNull(obj.getPath());
	}

	public String getObjectText(ISiteCategoryDefinition obj) {
		return preventNull(obj.getLabel());
	}

	public String getObjectText(PackageObject obj) {
		StringBuilder buffer = new StringBuilder(obj.getName());
		String version = obj.getVersion();
		if (version != null && !version.equals(Version.emptyVersion.toString())) {
			// Format version range for ImportPackageObject.  ExportPackageObject is handled correctly in this function
			version = formatVersion(version);
			buffer.append(' ').append(version);
		}
		return buffer.toString();
	}

	public String getObjectText(ISiteCategory obj) {
		ISiteCategoryDefinition def = obj.getDefinition();
		if (def != null)
			return preventNull(def.getLabel());
		return preventNull(obj.getName());
	}

	private String getObjectText(IStatus status) {
		return status.getMessage();
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof IPlugin) {
			return getObjectImage((IPlugin) obj);
		}
		if (obj instanceof IFragment) {
			return getObjectImage((IFragment) obj);
		}
		if (obj instanceof IPluginModel) {
			return getObjectImage(((IPluginModel) obj).getPlugin());
		}
		if (obj instanceof IFragmentModel) {
			return getObjectImage(((IFragmentModel) obj).getFragment());
		}
		if (obj instanceof ImportObject) {
			return getObjectImage((ImportObject) obj);
		}
		if (obj instanceof IPluginElement) {
			return getObjectImage((IPluginElement) obj);
		}
		if (obj instanceof IPluginImport) {
			return getObjectImage((IPluginImport) obj);
		}
		if (obj instanceof IProductPlugin) {
			return getObjectImage((IProductPlugin) obj);
		}
		if (obj instanceof BundleDescription) {
			return getObjectImage((BundleDescription) obj);
		}
		if (obj instanceof IPluginLibrary) {
			return getObjectImage((IPluginLibrary) obj);
		}
		if (obj instanceof IPluginExtension) {
			return getObjectImage((IPluginExtension) obj);
		}
		if (obj instanceof IPluginExtensionPoint) {
			return getObjectImage((IPluginExtensionPoint) obj);
		}
		if (obj instanceof NamedElement) {
			return ((NamedElement) obj).getImage();
		}
		if (obj instanceof ISchemaElement) {
			return getObjectImage((ISchemaElement) obj);
		}
		if (obj instanceof ISchemaAttribute) {
			return getObjectImage((ISchemaAttribute) obj);
		}
		if (obj instanceof ISchemaInclude) {
			ISchema schema = ((ISchemaInclude) obj).getIncludedSchema();
			return get(PDEPluginImages.DESC_PAGE_OBJ, schema == null || !schema.isValid() ? F_ERROR : 0);
		}
		if (obj instanceof IDocumentSection || obj instanceof ISchema) {
			return get(PDEPluginImages.DESC_DOC_SECTION_OBJ);
		}
		if (obj instanceof ISchemaCompositor) {
			return getObjectImage((ISchemaCompositor) obj);
		}
		if (obj instanceof IFeatureURLElement) {
			return getObjectImage((IFeatureURLElement) obj);
		}
		if (obj instanceof IFeatureModel) {
			int flags = 0;
			if (((IFeatureModel) obj).getUnderlyingResource() == null)
				flags |= F_EXTERNAL;
			return get(PDEPluginImages.DESC_FEATURE_OBJ, flags);
		}
		if (obj instanceof IFeatureChild) {
			return getObjectImage((IFeatureChild) obj);
		}
		if (obj instanceof IProductFeature) {
			return getObjectImage((IProductFeature) obj);
		}
		if (obj instanceof IFeaturePlugin) {
			return getObjectImage((IFeaturePlugin) obj);
		}
		if (obj instanceof IFeatureData) {
			return getObjectImage((IFeatureData) obj);
		}
		if (obj instanceof IFeatureImport) {
			return getObjectImage((IFeatureImport) obj);
		}
		if (obj instanceof IFeatureInfo) {
			return getObjectImage((IFeatureInfo) obj);
		}
		if (obj instanceof IProductModel) {
			return getObjectImage((IProductModel) obj);
		}
		if (obj instanceof IBuildEntry) {
			return get(PDEPluginImages.DESC_BUILD_VAR_OBJ);
		}
		if (obj instanceof ISiteFeature) {
			return getObjectImage((ISiteFeature) obj);
		}
		if (obj instanceof ISiteArchive) {
			return getObjectImage((ISiteArchive) obj);
		}
		if (obj instanceof ISiteCategoryDefinition) {
			return getObjectImage((ISiteCategoryDefinition) obj);
		}
		if (obj instanceof ISiteCategory) {
			return getObjectImage((ISiteCategory) obj);
		}
		if (obj instanceof ExportPackageObject) {
			return getObjectImage((ExportPackageObject) obj);
		}
		if (obj instanceof PackageObject) {
			return getObjectImage((PackageObject) obj);
		}
		if (obj instanceof ExecutionEnvironment) {
			return getObjectImage((ExecutionEnvironment) obj);
		}
		if (obj instanceof ResolverError) {
			return getObjectImage((ResolverError) obj);
		}
		if (obj instanceof Locale) {
			return get(PDEPluginImages.DESC_DISCOVERY);
		}
		if (obj instanceof IStatus) {
			return getObjectImage((IStatus) obj);
		}
		return super.getImage(obj);
	}

	private Image getObjectImage(ResolverError obj) {
		int type = obj.getType();
		return switch (type) {
			case ResolverError.MISSING_IMPORT_PACKAGE, ResolverError.EXPORT_PACKAGE_PERMISSION, ResolverError.IMPORT_PACKAGE_PERMISSION, ResolverError.IMPORT_PACKAGE_USES_CONFLICT -> JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			case ResolverError.MISSING_EXECUTION_ENVIRONMENT -> get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
			case ResolverError.FRAGMENT_BUNDLE_PERMISSION, ResolverError.FRAGMENT_CONFLICT -> get(PDEPluginImages.DESC_FRAGMENT_OBJ);
			default -> get(PDEPluginImages.DESC_PLUGIN_OBJ);
		};
	}

	private Image getObjectImage(ExecutionEnvironment environment) {
		return get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
	}

	private Image getObjectImage(IPlugin plugin) {
		return getObjectImage(plugin, false, false);
	}

	private Image getObjectImage(BundleDescription bundle) {
		return bundle.getHost() == null ? get(PDEPluginImages.DESC_PLUGIN_OBJ) : get(PDEPluginImages.DESC_FRAGMENT_OBJ);
	}

	public Image getObjectImage(IPlugin plugin, boolean checkEnabled, boolean javaSearch) {
		IPluginModelBase model = plugin.getPluginModel();
		int flags = getModelFlags(model);

		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = PDEPluginImages.DESC_PLUGIN_OBJ;
		if (checkEnabled && model.isEnabled() == false)
			desc = PDEPluginImages.DESC_EXT_PLUGIN_OBJ;
		return get(desc, flags);
	}

	private int getModelFlags(IPluginModelBase model) {
		int flags = 0;
		if (!(model.isLoaded() && model.isInSync()))
			flags = F_ERROR;
		IResource resource = model.getUnderlyingResource();
		if (resource == null) {
			flags |= F_EXTERNAL;
		} else {
			IProject project = resource.getProject();
			try {
				if (WorkspaceModelManager.isBinaryProject(project)) {
					String property = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
					if (property != null) {
						/*
						if (property.equals(PDECore.EXTERNAL_PROJECT_VALUE))
							flags |= F_EXTERNAL;
						else if (property.equals(PDECore.BINARY_PROJECT_VALUE))
						*/
						flags |= F_BINARY;
					}
				}
			} catch (CoreException e) {
			}
		}
		return flags;
	}

	private Image getObjectImage(IFragment fragment) {
		return getObjectImage(fragment, false, false);
	}

	public Image getObjectImage(IFragment fragment, boolean checkEnabled, boolean javaSearch) {
		IPluginModelBase model = fragment.getPluginModel();
		int flags = getModelFlags(model);
		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = PDEPluginImages.DESC_FRAGMENT_OBJ;
		if (checkEnabled && !model.isEnabled())
			desc = PDEPluginImages.DESC_EXT_FRAGMENT_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(ImportObject iobj) {
		int flags = 0;
		IPluginImport iimport = iobj.getImport();
		if (!iobj.isResolved())
			flags = iimport.isOptional() ? F_WARNING : F_ERROR;
		else if (iimport.isReexported())
			flags = F_EXPORT;
		if (iimport.isOptional())
			flags |= F_OPTIONAL;
		IPlugin plugin = iobj.getPlugin();
		if (plugin != null) {
			IPluginModelBase model = plugin.getPluginModel();
			flags |= getModelFlags(model);
		}
		return get(getRequiredPluginImageDescriptor(iimport), flags);
	}

	protected ImageDescriptor getRequiredPluginImageDescriptor(IPluginElement iobj) {
		return PDEPluginImages.DESC_GENERIC_XML_OBJ;
	}

	private Image getObjectImage(IPluginElement obj) {
		int flags = 0;
		IPluginObject parent = obj.getParent();
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			String point = ((IPluginExtension) parent).getPoint();
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			ISchema schema = registry.getSchema(point);
			if (schema != null) {
				ISchemaElement schemaElement = schema.findElement(obj.getName());
				// Only label an element if either the element is deprecated or it actually specifies a deprecated attribute;
				// We don't want to mislabel non-deprecated elements with no deprecated attributes.
				if (schemaElement != null && (schemaElement.isDeprecated() || hasDeprecatedAttributes(obj, schemaElement)))
					flags |= F_WARNING;
			}
		}
		return get(getRequiredPluginImageDescriptor(obj), flags);
	}

	/** Return true if the {@code obj} has an attribute that is deprecated */
	private boolean hasDeprecatedAttributes(IPluginElement obj, ISchemaElement schemaElement) {
		for (ISchemaAttribute schAtt : schemaElement.getAttributes()) {
			if (schAtt.isDeprecated()) {
				IPluginAttribute pAtt = obj.getAttribute(schAtt.getName());
				if (pAtt != null) {
					return true;
				}
			}
		}
		return false;
	}

	protected ImageDescriptor getRequiredPluginImageDescriptor(IPluginImport iobj) {
		return PDEPluginImages.DESC_REQ_PLUGIN_OBJ;
	}

	private Image getObjectImage(IPluginImport obj) {
		int flags = 0;
		if (obj.isReexported())
			flags |= F_EXPORT;
		return get(getRequiredPluginImageDescriptor(obj), flags);
	}

	private Image getObjectImage(IProductPlugin obj) {
		Version version = (obj.getVersion() != null && obj.getVersion().length() > 0 && !obj.getVersion().equals(ICoreConstants.DEFAULT_VERSION)) ? Version.parseVersion(obj.getVersion()) : null;
		BundleDescription desc = TargetPlatformHelper.getState().getBundle(obj.getId(), version);
		if (desc != null) {
			return desc.getHost() == null ? get(PDEPluginImages.DESC_PLUGIN_OBJ) : get(PDEPluginImages.DESC_FRAGMENT_OBJ);
		}
		return get(PDEPluginImages.DESC_PLUGIN_OBJ, F_ERROR);
	}

	private Image getObjectImage(IPluginLibrary library) {
		return get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
	}

	private Image getObjectImage(IPluginExtension point) {
		return get(PDEPluginImages.DESC_EXTENSION_OBJ);
	}

	private Image getObjectImage(IPluginExtensionPoint point) {
		return get(PDEPluginImages.DESC_EXT_POINT_OBJ);
	}

	private Image getObjectImage(ISchemaElement element) {
		int flags = 0;
		if (element instanceof ISchemaObjectReference && ((ISchemaObjectReference) element).getReferencedObject() == null)
			flags |= F_ERROR;
		ImageDescriptor desc = element instanceof ISchemaObjectReference ? PDEPluginImages.DESC_XML_ELEMENT_REF_OBJ : PDEPluginImages.DESC_GEL_SC_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(ISchemaAttribute att) {
		int kind = att.getKind();
		String type = att.getType().getName();
		int use = att.getUse();
		int flags = 0;
		if (use == ISchemaAttribute.OPTIONAL)
			flags = 0; //|= F_OPTIONAL;
		if (kind == IMetaAttribute.JAVA)
			return get(PDEPluginImages.DESC_ATT_CLASS_OBJ, flags);
		if (kind == IMetaAttribute.RESOURCE)
			return get(PDEPluginImages.DESC_ATT_FILE_OBJ, flags);
		if (kind == IMetaAttribute.IDENTIFIER)
			return get(PDEPluginImages.DESC_ATT_ID_OBJ, flags);
		if (type.equals(ISchemaAttribute.TYPES[ISchemaAttribute.BOOL_IND]))
			return get(PDEPluginImages.DESC_ATT_BOOLEAN_OBJ, flags);

		return get(PDEPluginImages.DESC_ATT_STRING_OBJ);
	}

	private Image getObjectImage(ISchemaCompositor compositor) {
		return switch (compositor.getKind()) {
			case ISchemaCompositor.ALL -> get(PDEPluginImages.DESC_ALL_SC_OBJ);
			case ISchemaCompositor.CHOICE -> get(PDEPluginImages.DESC_CHOICE_SC_OBJ);
			case ISchemaCompositor.SEQUENCE -> get(PDEPluginImages.DESC_SEQ_SC_OBJ);
			case ISchemaCompositor.GROUP -> get(PDEPluginImages.DESC_GROUP_SC_OBJ);
			default -> null;
		};
	}

	private Image getObjectImage(IFeatureURLElement url) {
		return get(PDEPluginImages.DESC_LINK_OBJ);
	}

	private Image getObjectImage(IFeaturePlugin plugin) {
		int flags = 0;
		if (((FeaturePlugin) plugin).getPluginBase() == null) {
			int cflag = CompilerFlags.getFlag(null, CompilerFlags.F_UNRESOLVED_PLUGINS);
			if (cflag == CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag == CompilerFlags.WARNING)
				flags = F_WARNING;
		}
		if (plugin.isFragment())
			return get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
		return get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
	}

	private Image getObjectImage(IFeatureChild feature) {
		int flags = 0;
		if (((FeatureChild) feature).getReferencedFeature() == null) {
			int cflag = CompilerFlags.getFlag(null, CompilerFlags.F_UNRESOLVED_FEATURES);
			if (cflag == CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag == CompilerFlags.WARNING)
				flags = F_WARNING;
		}
		return get(PDEPluginImages.DESC_FEATURE_OBJ, flags);
	}

	private Image getObjectImage(IProductFeature feature) {
		int flags = 0;
		String version = feature.getVersion().length() > 0 ? feature.getVersion() : ICoreConstants.DEFAULT_VERSION;
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModel(feature.getId(), version);
		if (model == null)
			flags = F_ERROR;
		return get(PDEPluginImages.DESC_FEATURE_OBJ, flags);
	}

	private Image getObjectImage(IFeatureData data) {
		int flags = 0;
		if (!data.exists())
			flags = F_ERROR;
		ImageDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(data.getId());
		return get(desc, flags);
	}

	private Image getObjectImage(IFeatureImport obj) {
		FeatureImport iimport = (FeatureImport) obj;
		int type = iimport.getType();
		ImageDescriptor base;
		int flags = 0;

		if (type == IFeatureImport.FEATURE) {
			base = PDEPluginImages.DESC_FEATURE_OBJ;
			IFeature feature = iimport.getFeature();
			if (feature == null)
				flags = F_ERROR;
		} else {
			base = PDEPluginImages.DESC_REQ_PLUGIN_OBJ;
			IPlugin plugin = iimport.getPlugin();
			if (plugin == null)
				flags = F_ERROR;
		}

		return get(base, flags);
	}

	private Image getObjectImage(IFeatureInfo info) {
		int flags = 0;
		String text = info.getDescription();
		if (text != null)
			text = text.trim();
		if (text != null && text.length() > 0) {
			// complete
			flags = F_EDIT;
		}
		return get(PDEPluginImages.DESC_DOC_SECTION_OBJ, flags);
	}

	private Image getObjectImage(IProductModel productModel) {
		return get(PDEPluginImages.DESC_PRODUCT_DEFINITION);
	}

	public Image getObjectImage(ISiteFeature obj) {
		int flags = 0;
		if (obj.getArchiveFile() != null) {
			flags = F_BINARY;
		}
		return get(PDEPluginImages.DESC_JAVA_LIB_OBJ, flags);
	}

	public Image getObjectImage(ISiteArchive obj) {
		return get(PDEPluginImages.DESC_JAVA_LIB_OBJ, 0);
	}

	public Image getObjectImage(ISiteCategoryDefinition obj) {
		return get(PDEPluginImages.DESC_CATEGORY_OBJ);
	}

	public Image getObjectImage(ISiteCategory obj) {
		int flags = obj.getDefinition() == null ? F_ERROR : 0;
		return get(PDEPluginImages.DESC_CATEGORY_OBJ, flags);
	}

	public Image getObjectImage(ExportPackageObject obj) {
		int flags = 0;
		if (obj.isInternal()) {
			flags = F_INTERNAL;
			// if internal with at least one friend
			if (obj.getFriends().length > 0)
				flags = F_FRIEND;
		}
		ImageDescriptor desc = JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_PACKAGE);
		return get(desc, flags);
	}

	public Image getObjectImage(PackageObject obj) {
		int flags = 0;
		if (obj instanceof ImportPackageObject importPackageObject) {
			if (importPackageObject.isOptional()) {
				flags |= F_OPTIONAL;
			}
			if (!importPackageObject.isResolved()) {
				flags |= importPackageObject.isOptional() ? F_WARNING : F_ERROR;
			}
		}
		ImageDescriptor desc = JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_PACKAGE);
		return get(desc, flags);
	}

	private Image getObjectImage(IStatus status) {
		int sev = status.getSeverity();
		return switch (sev) {
			case IStatus.ERROR -> PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_ERROR_TSK);
			case IStatus.WARNING -> PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_WARN_TSK);
			default -> PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_INFO_TSK);
		};
	}

	public boolean isFullNameModeEnabled() {
		return PDEPlugin.isFullNameModeEnabled();
	}

	/*
	 * BIDI support (bug 183417)
	 * Any time we display a bracketed version, we should preface it with /u200f (zero width Arabic character).
	 * Then inside the bracket, we should include /u200e (zero width Latin character).  Since the leading separator
	 * will be resolved based on its surrounding text, when it is surrounded by a Arabic character and a Latin character
	 * the bracket will take the proper shape based on the underlying embedded direction.  The Latin character must
	 * come after the bracket since versions are represented with Latin numbers.  This ensure proper number format.
	 */

	/*
	 * returns true if instance has either Arabic of Hebrew locale (text displayed RTL)
	 */
	private static boolean isRTL() {
		Locale locale = Locale.getDefault();
		String localeString = locale.toString();
		return (localeString.startsWith("ar") || //$NON-NLS-1$
		localeString.startsWith("he")); //$NON-NLS-1$
	}

	/*
	 * Returns a String containing the unicode to properly display the version ranging when running bidi.
	 */
	public static String formatVersion(String versionRange) {
		boolean isBasicVersion = versionRange == null || versionRange.length() == 0 || Character.isDigit(versionRange.charAt(0));
		if (isBasicVersion) {
			if (SWTUtil.isBidi())
				// The versionRange is a single version.  Since parenthesis is neutral, it direction is determined by leading and following character.
				// Since leading character is Arabic and following character is Latin, the parenthesis will take default (proper) direction.
				// Must have the following character be the Latin character to ensure version is formatted as Latin (LTR)
				return "\u200f(\u200e" + versionRange + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return "(" + versionRange + ')'; //$NON-NLS-1$
		} else if (isRTL() && SWTUtil.isBidi()) {
			// when running RTL and formatting a versionRange, we need to break up the String to make sure it is properly formatted.
			// A version should always be formatted LTR (start with \u202d, ends with \u202c) since it is composed of Latin characters.
			// With specifying this format, if the qualifier has a Latin character, it will not be formatted correctly.
			int index = versionRange.indexOf(',');
			if (index > 0) {
				// begin with zero length Arabic character so version appears on left (correct) side of id.
				// Then add RTL strong encoding so parentheses and comma have RTL formatting.
				StringBuilder buffer = new StringBuilder("\u200f\u202e"); //$NON-NLS-1$
				// begin with leading separator (either parenthesis or bracket)
				buffer.append(versionRange.charAt(0));
				// start LTR encoding for min version
				buffer.append('\u202d');
				// min version
				buffer.append(versionRange.substring(1, index));
				// end LTR encoding, add ',' (which will be RTL due to first RTL strong encoding), and start LTR encoding for max version
				// We require a space between the two numbers otherwise it is considered 1 number in Arabic (comma is digit grouping system).
				buffer.append("\u202c, \u202d"); //$NON-NLS-1$
				// max version
				buffer.append(versionRange.substring(index + 1, versionRange.length() - 1));
				// end LTR encoding
				buffer.append('\u202c');
				// add trailing separator
				buffer.append(versionRange.charAt(versionRange.length() - 1));
				return buffer.toString();
			}
			//			} else {
			//			    since the version is LTR and we are running LTR, do nothing.  This is the case for version ranges when run in LTR
		}
		return versionRange;
	}

}
