/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal;

import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.util.OverlayIcon;
import org.eclipse.pde.internal.util.SharedLabelProvider;
import org.eclipse.pde.internal.model.plugin.ImportObject;
import org.eclipse.pde.internal.elements.NamedElement;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.pde.internal.editor.IPDEEditorPage;
import org.eclipse.pde.model.build.IBuildEntry;
import org.eclipse.pde.internal.model.feature.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * @version 	1.0
 * @author
 */
public class PDELabelProvider extends SharedLabelProvider {
	private static final String KEY_OUT_OF_SYNC = "WorkspaceModelManager.outOfSync";

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
		if (obj instanceof IPluginLibrary) {
			return getObjectText((IPluginLibrary) obj);
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
		return super.getText(obj);
	}

	public String getObjectText(IPluginBase pluginBase) {
		String name =
			isFullNameModeEnabled() ? pluginBase.getTranslatedName() : pluginBase.getId();
		String version = pluginBase.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + " (" + pluginBase.getVersion() + ")";
		else
			text = name;
		if (!pluginBase.getModel().isInSync())
			text += " " + PDEPlugin.getResourceString(KEY_OUT_OF_SYNC);
		return text;
	}

	public String getObjectText(IPluginExtension extension) {
		return isFullNameModeEnabled()
			? extension.getTranslatedName()
			: extension.getId();
	}

	public String getObjectText(IPluginExtensionPoint point) {
		return isFullNameModeEnabled() ? point.getTranslatedName() : point.getId();
	}

	public String getObjectText(ImportObject obj) {
		if (isFullNameModeEnabled())
			return obj.toString();
		return obj.getId();
	}

	public String getObjectText(IPluginLibrary obj) {
		return obj.getName();
	}

	public String getObjectText(ISchemaObject obj) {
		String text = obj.getName();
		if (obj instanceof ISchemaRepeatable) {
			ISchemaRepeatable rso = (ISchemaRepeatable) obj;
			boolean unbounded = rso.getMaxOccurs() == Integer.MAX_VALUE;
			int maxOccurs = rso.getMaxOccurs();
			int minOccurs = rso.getMinOccurs();
			if (maxOccurs != 1 || minOccurs != 1) {
				text += " (" + minOccurs + " - ";
				if (unbounded)
					text += "*)";
				else
					text += maxOccurs + ")";
			}
		}
		return text;
	}

	public String getObjectText(FeaturePlugin obj) {
		FeaturePlugin fref = (FeaturePlugin) obj;
		IPluginBase pluginBase = fref.getPluginBase();
		if (pluginBase != null) {
			return getObjectText(pluginBase);
		} else
			return obj.toString();
	}

	public String getObjectText(FeatureImport obj) {
		IPlugin plugin = obj.getPlugin();
		if (plugin != null && isFullNameModeEnabled()) {
			return plugin.getTranslatedName();
		}
		return obj.getId();
	}

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
		if (obj instanceof IPluginImport) {
			return getObjectImage((IPluginImport) obj);
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
		if (obj instanceof IFeaturePlugin) {
			return getObjectImage((IFeaturePlugin) obj);
		}
		if (obj instanceof IFeatureImport) {
			return getObjectImage((IFeatureImport) obj);
		}
		if (obj instanceof IFeatureInfo) {
			return getObjectImage((IFeatureInfo) obj);
		}
		if (obj instanceof IPDEEditorPage) {
			return get(PDEPluginImages.DESC_PAGE_OBJ);
		}
		if (obj instanceof IBuildEntry) {
			return get(PDEPluginImages.DESC_BUILD_VAR_OBJ);
		}
		return super.getImage(obj);
	}

	private Image getObjectImage(IPlugin plugin) {
		IPluginModelBase model = plugin.getModel();
		int flags = getModelFlags(model);
		return get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
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
				String property =
					project.getPersistentProperty(PDEPlugin.EXTERNAL_PROJECT_PROPERTY);
				if (property != null) {
					if (property.equals(PDEPlugin.EXTERNAL_PROJECT_VALUE))
						flags |= F_EXTERNAL;
					else if (property.equals(PDEPlugin.BINARY_PROJECT_VALUE))
						flags |= F_BINARY;
				}
			} catch (CoreException e) {
			}
		}
		return flags;
	}

	private Image getObjectImage(IFragment fragment) {
		IPluginModelBase model = fragment.getModel();
		int flags = getModelFlags(model);
		return get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
	}

	private Image getObjectImage(ImportObject iobj) {
		int flags = 0;
		if (iobj.isResolved() == false)
			flags = F_ERROR;
		else if (iobj.getImport().isReexported())
			flags = F_EXPORT;
		return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, flags);
	}

	private Image getObjectImage(IPluginImport obj) {
		int flags = 0;
		if (obj.isReexported())
			flags = F_EXPORT;
		return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, flags);
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
		if (att.getKind() == ISchemaAttribute.JAVA)
			return get(PDEPluginImages.DESC_ATT_CLASS_OBJ, flags);
		if (att.getKind() == ISchemaAttribute.RESOURCE)
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
		if (text != null && text.length() > 0) {
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
		if (((FeaturePlugin) plugin).getPluginBase() == null)
			flags = F_ERROR;
		if (plugin.isFragment())
			return get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
		else
			return get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
	}

	private Image getObjectImage(IFeatureImport obj) {
		FeatureImport iimport = (FeatureImport) obj;
		IPlugin plugin = iimport.getPlugin();
		int flags = 0;
		if (plugin == null)
			flags = F_ERROR;
		return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, flags);
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

	public boolean isFullNameModeEnabled() {
		return MainPreferencePage.isFullNameModeEnabled();
	}
}