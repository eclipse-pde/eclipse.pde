/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal;

import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.util.OverlayIcon;
import org.eclipse.pde.internal.util.SharedLabelProvider;

/**
 * @version 	1.0
 * @author
 */
public class PDELabelProvider extends SharedLabelProvider {

	public PDELabelProvider() {

	}
	public String getText(Object obj) {
		if (obj instanceof IPluginModelBase) {
			return getObjectText(((IPluginModelBase)obj).getPluginBase());
		}
		if (obj instanceof IPluginBase) {
			return getText((IPluginBase)obj);
		}
		return super.getText(obj);
	}
	
	public String getObjectText(IPluginBase pluginBase) {
		String name = isFullNameModeEnabled()?pluginBase.getTranslatedName() : pluginBase.getId();
		String version = pluginBase.getVersion();
		if (version!=null && version.length()>0)
			return name + " ("+pluginBase.getVersion()+")";
		else
			return name;
	}
	
	public String getObjectText(IPluginExtension extension) {
		return isFullNameModeEnabled() ? extension.getTranslatedName() : extension.getId();
	}
	
	public String getObjectText(IPluginExtensionPoint point) {
		return isFullNameModeEnabled() ? point.getTranslatedName() : point.getFullId();
	}
	
	public String getObjectText(IPluginImport iimport) {
		if (!isFullNameModeEnabled()) {
			return iimport.getId();
		}
		return "";
	}

	public Image getImage(Object obj) {
		if (obj instanceof IPluginModel) {
			return getObjectImage(((IPluginModel)obj).getPlugin());
		}
		return super.getImage(obj);
	}

	private Image getObjectImage(IPlugin plugin) {
		return PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
	}
	
	private Image getObjectImage(IFragment fragment) {
		return PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
	}
	
	private boolean isFullNameModeEnabled() {
		return MainPreferencePage.isFullNameModeEnabled();
	}
}