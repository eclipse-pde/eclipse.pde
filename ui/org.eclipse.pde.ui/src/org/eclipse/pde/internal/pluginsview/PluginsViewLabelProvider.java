package org.eclipse.pde.internal.pluginsview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.PDEPluginImages;

public class PluginsViewLabelProvider extends LabelProvider {
	private PluginsView view;
	private Image pluginImage;
	private Image disabledPluginImage;
	private Image fragmentImage;
	private Image disabledFragmentImage;
	private Image configsImage;
	private Image configImage;

	public PluginsViewLabelProvider(PluginsView view) {
		this.view = view;
		initializeImages();
	}

	private void initializeImages() {
		pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		disabledPluginImage = PDEPluginImages.DESC_PLUGIN_DIS_OBJ.createImage();
		fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
		disabledFragmentImage = PDEPluginImages.DESC_FRAGMENT_DIS_OBJ.createImage();
		configsImage = PDEPluginImages.DESC_PLUGIN_CONFIGS_OBJ.createImage();
		configImage = PDEPluginImages.DESC_PLUGIN_CONFIG_OBJ.createImage();
	}

	public void dispose() {
		pluginImage.dispose();
		disabledFragmentImage.dispose();
		fragmentImage.dispose();
		disabledFragmentImage.dispose();
		configsImage.dispose();
		configImage.dispose();
		super.dispose();
	}

	public String getText(Object obj) {
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) obj;
			String name = model.getPluginBase().getId();
			if (view.getShowFullName()) {
				name = model.getPluginBase().getTranslatedName();
			}
			if (view.getShowVersion()) {
				name += " (" + model.getPluginBase().getVersion() + ")";
			}
			return name;
		}
		return super.getText(obj);
	}

	public Image getImage(Object obj) {
		if (obj instanceof ParentElement) {
			ParentElement pe = (ParentElement)obj;
			if (pe.getId()==ParentElement.PLUGIN_PROFILES)
			   return configsImage;
			return PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);
		}
		if (obj instanceof IPluginModel) {
			IPluginModel model = (IPluginModel)obj;
			return model.isEnabled()?pluginImage:disabledPluginImage;
		}
		if (obj instanceof IFragmentModel) {
			IFragmentModel model = (IFragmentModel)obj;
			return model.isEnabled()?fragmentImage:disabledFragmentImage;
		}
		return super.getImage(obj);
	}
}