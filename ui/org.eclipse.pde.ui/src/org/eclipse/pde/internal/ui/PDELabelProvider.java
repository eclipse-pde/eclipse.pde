/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.builders.CompilerFlags;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class PDELabelProvider extends SharedLabelProvider {
	private static final String KEY_OUT_OF_SYNC =
		"PluginModelManager.outOfSync"; //$NON-NLS-1$

	public PDELabelProvider() {

	}
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
			return getObjectText((IProductPlugin)obj);
		}
		if (obj instanceof IPluginImport) {
			return getObjectText((IPluginImport)obj);
		}
		if (obj instanceof IPluginLibrary) {
			return getObjectText((IPluginLibrary) obj);
		}
		if (obj instanceof IPluginExtension) {
			return getObjectText((IPluginExtension)obj);
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
			return getObjectText((IProductFeature)obj);
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
			return getObjectText((IBuildEntry)obj);
		}
		return super.getText(obj);
	}

	public String getObjectText(IPluginBase pluginBase) {
		String name =
			isFullNameModeEnabled()
				? pluginBase.getTranslatedName()
				: pluginBase.getId();
		name = preventNull(name);
		String version = pluginBase.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + " (" + pluginBase.getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		else
			text = name;
		if (pluginBase.getModel() != null && !pluginBase.getModel().isInSync())
			text += " " + PDEPlugin.getResourceString(KEY_OUT_OF_SYNC); //$NON-NLS-1$
		return text;
	}
	
	private String preventNull(String text) {
		return text!=null?text:""; //$NON-NLS-1$
	}

	public String getObjectText(IPluginExtension extension) {
		return preventNull(isFullNameModeEnabled()
			? extension.getTranslatedName()
			: extension.getPoint());
	}

	public String getObjectText(IPluginExtensionPoint point) {
		return preventNull(isFullNameModeEnabled()
			? point.getTranslatedName()
			: point.getId());
	}

	public String getObjectText(ImportObject obj) {
		if (isFullNameModeEnabled())
			return obj.toString();
		return preventNull(obj.getId());
	}
	
	public String getObjectText(IProductPlugin obj) {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(obj.getId());	
		String version = model == null ? null : model.getPluginBase().getVersion();
		String text = null;
		if (isFullNameModeEnabled()) {
			if (model != null) {
				text = model.getPluginBase().getTranslatedName();
			}
		} else {
			text = preventNull(obj.getId());
		}
		return version == null ? text : text + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public String getObjectText(IPluginImport obj) {
		if (isFullNameModeEnabled()) {
			String id = obj.getId();
			IPlugin plugin = PDECore.getDefault().findPlugin(obj.getId());
			if (plugin != null) {
				return plugin.getTranslatedName();
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
		String text = obj.getName();
		if (obj instanceof ISchemaRepeatable) {
			ISchemaRepeatable rso = (ISchemaRepeatable) obj;
			boolean unbounded = rso.getMaxOccurs() == Integer.MAX_VALUE;
			int maxOccurs = rso.getMaxOccurs();
			int minOccurs = rso.getMinOccurs();
			if (maxOccurs != 1 || minOccurs != 1) {
				text += " (" + minOccurs + " - "; //$NON-NLS-1$ //$NON-NLS-2$
				if (unbounded)
					text += "*)"; //$NON-NLS-1$
				else
					text += maxOccurs + ")"; //$NON-NLS-1$
			}
		}
		return text;
	}

	public String getObjectText(FeaturePlugin obj) {
		String name =
			isFullNameModeEnabled() ? obj.getTranslatableLabel() : obj.getId();
		String version = obj.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + " (" + version + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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
				return preventNull(feature.getLabel());
			}
		}
		return preventNull(obj.getId());
	}

	public String getObjectText(IFeatureModel obj) {
		IFeature feature = obj.getFeature();
		return preventNull(feature.getId()) + " (" + preventNull(feature.getVersion()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$

	}

	public String getObjectText(FeatureChild obj) {
		return preventNull(obj.getId()) + " (" + preventNull(obj.getVersion()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public String getObjectText(IProductFeature obj) {
		return preventNull(obj.getId()) + " (" + preventNull(obj.getVersion()) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getObjectText(ISiteFeature obj) {
		return preventNull(obj.getURL());
	}

	public String getObjectText(ISiteArchive obj) {
		return preventNull(obj.getPath());
	}
	public String getObjectText(ISiteCategoryDefinition obj) {
		return preventNull(obj.getLabel());
	}
	public String getObjectText(ISiteCategory obj) {
		ISiteCategoryDefinition def = obj.getDefinition();
		if (def != null)
			return preventNull(def.getLabel());
		return preventNull(obj.getName());
	}

	public Image getImage(Object obj) {
		if (obj instanceof IBundlePluginBase) {
			return getObjectImage((IBundlePluginBase) obj);
		}
		if (obj instanceof IBundlePluginModelBase) {
			return getObjectImage((IBundlePluginBase)((IBundlePluginModelBase) obj).getPluginBase());
		}
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
		if (obj instanceof IPluginImport) {
			return getObjectImage((IPluginImport) obj);
		}
		if (obj instanceof IProductPlugin) {
			return getObjectImage((IProductPlugin)obj);
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
		if (obj instanceof IDocumentSection || obj instanceof ISchema) {
			int flags = getSchemaObjectFlags((ISchemaObject) obj);
			return get(PDEPluginImages.DESC_DOC_SECTION_OBJ, flags);
		}
		if (obj instanceof ISchemaCompositor) {
			return getObjectImage((ISchemaCompositor) obj);
		}
		if (obj instanceof IFeatureURLElement) {
			return getObjectImage((IFeatureURLElement) obj);
		}
		if (obj instanceof IFeatureModel) {
			int flags = 0;
			if (((IFeatureModel)obj).getUnderlyingResource() == null) 
				flags |= F_EXTERNAL;
			return get(PDEPluginImages.DESC_FEATURE_OBJ, flags);
		}
		if (obj instanceof IFeatureChild) {
			return getObjectImage((IFeatureChild) obj);
		}
		if (obj instanceof IProductFeature) {
			return getObjectImage((IProductFeature)obj);
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
		return super.getImage(obj);
	}

	public Image getObjectImage(IBundlePluginBase bundlePluginBase) {
		return getObjectImage(bundlePluginBase, false, false);
	}
	
	public Image getObjectImage(IBundlePluginBase bundlePluginBase,
								boolean checkEnabled,
								boolean javaSearch) {
		IBundlePluginModelBase model = (IBundlePluginModelBase)bundlePluginBase.getModel();

		int flags = getModelFlags(model);

		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = PDEPluginImages.DESC_BUNDLE_OBJ;
		if (model.isFragmentModel())
			desc = PDEPluginImages.DESC_BUNDLE_FRAGMENT_OBJ;
		if (checkEnabled && model.isEnabled() == false)
			{}//desc = PDEPluginImages.DESC_EXT_PLUGIN_OBJ;
		return get(desc, flags);
	}
	
	private int getModelFlags(IBundlePluginModelBase model) {
		int flags = 0;
		if (!(model.isLoaded() && model.isInSync()))
			flags = F_ERROR;
		IResource resource = model.getUnderlyingResource();
		if (resource == null) {
			flags |= F_EXTERNAL;
		} else {
			IProject project = resource.getProject();
			try {
				if (WorkspaceModelManager.isBinaryPluginProject(project)) {
					String property =
						project.getPersistentProperty(
							PDECore.EXTERNAL_PROJECT_PROPERTY);
					if (property != null) {
						flags |= F_BINARY;
					}
				}
			} catch (CoreException e) {
			}
		}
		return flags;
	}
	private Image getObjectImage(IPlugin plugin) {
		return getObjectImage(plugin, false, false);
	}

	public Image getObjectImage(
		IPlugin plugin,
		boolean checkEnabled,
		boolean javaSearch) {
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
				if (WorkspaceModelManager.isBinaryPluginProject(project)) {
					String property =
						project.getPersistentProperty(
							PDECore.EXTERNAL_PROJECT_PROPERTY);
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

	public Image getObjectImage(
		IFragment fragment,
		boolean checkEnabled,
		boolean javaSearch) {
		IPluginModelBase model = fragment.getPluginModel();
		int flags = getModelFlags(model);
		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = PDEPluginImages.DESC_FRAGMENT_OBJ;
		if (checkEnabled && model.isEnabled() == false)
			desc = PDEPluginImages.DESC_EXT_FRAGMENT_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(ImportObject iobj) {
		int flags = 0;
		if (iobj.isResolved() == false)
			flags = F_ERROR;
		else if (iobj.getImport().isReexported())
			flags = F_EXPORT;
		IPlugin plugin = iobj.getPlugin();
		if (plugin != null) {
			IPluginModelBase model = plugin.getPluginModel();
			flags |= getModelFlags(model);
		}
		return get(getRequiredPluginImageDescriptor(iobj.getImport()), flags);
	}
	
	protected ImageDescriptor getRequiredPluginImageDescriptor(IPluginImport iobj) {
		return PDEPluginImages.DESC_REQ_PLUGIN_OBJ;
	}

	private Image getObjectImage(IPluginImport obj) {
		int flags = 0;
		if (obj.isReexported())
			flags = F_EXPORT;
		return get(getRequiredPluginImageDescriptor(obj), flags);
	}
	
	private Image getObjectImage(IProductPlugin obj) {
		String id = obj.getId();
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(id);
		if (model != null)
			return getImage(model);
	
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
		int flags = getSchemaObjectFlags(element);
		return get(PDEPluginImages.DESC_GEL_SC_OBJ, flags);
	}
	private Image getObjectImage(ISchemaAttribute att) {
		int flags = getSchemaObjectFlags(att);
		if (att.getKind() == IMetaAttribute.JAVA)
			return get(PDEPluginImages.DESC_ATT_CLASS_OBJ, flags);
		if (att.getKind() == IMetaAttribute.RESOURCE)
			return get(PDEPluginImages.DESC_ATT_FILE_OBJ, flags);
		if (att.getUse() == ISchemaAttribute.REQUIRED)
			return get(PDEPluginImages.DESC_ATT_REQ_OBJ, flags);
		return get(PDEPluginImages.DESC_ATT_IMPL_OBJ, flags);
	}

	private Image getObjectImage(ISchemaCompositor compositor) {
		switch (compositor.getKind()) {
			case ISchemaCompositor.ALL :
				return get(PDEPluginImages.DESC_ALL_SC_OBJ);
			case ISchemaCompositor.CHOICE :
				return get(PDEPluginImages.DESC_CHOICE_SC_OBJ);
			case ISchemaCompositor.SEQUENCE :
				return get(PDEPluginImages.DESC_SEQ_SC_OBJ);
			case ISchemaCompositor.GROUP :
				return get(PDEPluginImages.DESC_GROUP_SC_OBJ);
		}
		return null;
	}

	private int getSchemaObjectFlags(ISchemaObject sobj) {
		int flags = 0;
		String text = sobj.getDescription();
		if (text != null)
			text = text.trim();
		if (text != null && text.length() > 0 && text.charAt(0)!='[') {
			// complete
			flags = F_EDIT;
		}
		return flags;
	}

	private Image getObjectImage(IFeatureURLElement url) {
		return get(PDEPluginImages.DESC_LINK_OBJ);
	}

	private Image getObjectImage(IFeaturePlugin plugin) {
		int flags = 0;
		if (((FeaturePlugin) plugin).getPluginBase() == null) {
			int cflag = CompilerFlags.getFlag(null, CompilerFlags.F_UNRESOLVED_PLUGINS);
			if (cflag==CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag==CompilerFlags.WARNING)
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
			if (cflag==CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag==CompilerFlags.WARNING)
				flags = F_WARNING;
		}
		return get(PDEPluginImages.DESC_FEATURE_OBJ, flags);
	}
	
	private Image getObjectImage(IProductFeature feature) {
		return get(PDEPluginImages.DESC_FEATURE_OBJ, 0);
	}

	private Image getObjectImage(IFeatureData data) {
		int flags = 0;
		if (!data.exists())
			flags = F_ERROR;
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				data.getId());
		return get(desc, flags);
	}

	private Image getObjectImage(IFeatureImport obj) {
		FeatureImport iimport = (FeatureImport) obj;
		int type = iimport.getType();
		ImageDescriptor base;
		int flags = 0;
		
		if (type==IFeatureImport.FEATURE) {
			base = PDEPluginImages.DESC_FEATURE_OBJ;
			IFeature feature = iimport.getFeature();
			if (feature == null)
				flags = F_ERROR;
		}
		else {
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

	public boolean isFullNameModeEnabled() {
		return PDEPlugin.isFullNameModeEnabled();
	}
}
