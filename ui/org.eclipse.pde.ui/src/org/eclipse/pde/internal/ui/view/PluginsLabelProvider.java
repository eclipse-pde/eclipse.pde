package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.ImageDescriptor;

public class PluginsLabelProvider extends LabelProvider {
	private PDELabelProvider sharedProvider;
	private Image projectImage;
	private Image folderImage;

	/**
	 * Constructor for PluginsLabelProvider.
	 */
	public PluginsLabelProvider() {
		super();
		sharedProvider = PDEPlugin.getDefault().getLabelProvider();
		folderImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_FOLDER);
			projectImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_OBJ_PROJECT);
		sharedProvider.connect(this);
	}

	public void dispose() {
		sharedProvider.disconnect(this);
		super.dispose();
	}

	public String getText(Object obj) {
		if (obj instanceof ModelEntry) {
			return getText((ModelEntry) obj);
		}
		if (obj instanceof FileAdapter) {
			return getText((FileAdapter) obj);
		}
		return super.getText(obj);
	}

	public Image getImage(Object obj) {
		if (obj instanceof ModelEntry) {
			return getImage((ModelEntry) obj);
		}
		if (obj instanceof FileAdapter) {
			return getImage((FileAdapter)obj);
		}
		return null;
	}

	private String getText(ModelEntry entry) {
		IPluginModelBase model = entry.getActiveModel();
		String text = sharedProvider.getText(model);
		if (model.isEnabled()==false)
			text += " - disabled";
		return text;
	}

	private String getText(FileAdapter file) {
		return file.getFile().getName();
	}

	private Image getImage(ModelEntry entry) {
		IPluginModelBase model = entry.getActiveModel();
		if (model.getUnderlyingResource()!=null)
			return projectImage;
		if (model instanceof IPluginModel)
			return sharedProvider.getObjectImage((IPlugin)model.getPluginBase(), true, entry.isInJavaSearch());
		else
			return sharedProvider.getObjectImage((IFragment)model.getPluginBase(), true, entry.isInJavaSearch());
	}

	private Image getImage(FileAdapter fileAdapter) {
		if (fileAdapter.hasChildren()) {
			return folderImage;
		}
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				fileAdapter.getFile().getName());
		return sharedProvider.get(desc);
	}
}