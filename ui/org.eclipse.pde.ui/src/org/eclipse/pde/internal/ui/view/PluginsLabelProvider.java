package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.*;

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
		if (obj instanceof IPackageFragmentRoot) {
			// use the short name
			IPath path = ((IPackageFragmentRoot)obj).getPath();
			return path.lastSegment();
		}
		if (obj instanceof IJavaElement) {
			return ((IJavaElement) obj).getElementName();
		}
		if (obj instanceof IStorage) {
			return ((IStorage)obj).getName();
		}
		return super.getText(obj);
	}

	public Image getImage(Object obj) {
		if (obj instanceof ModelEntry) {
			return getImage((ModelEntry) obj);
		}
		if (obj instanceof FileAdapter) {
			return getImage((FileAdapter) obj);
		}
		if (obj instanceof IPackageFragmentRoot) {
			return JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
		}
		if (obj instanceof IPackageFragment) {
			return JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE);
		}
		if (obj instanceof ICompilationUnit) {
			return JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CUNIT);
		}
		if (obj instanceof IClassFile) {
			return JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CFILE);
		}
		if (obj instanceof IStorage) {
			String name = ((IStorage)obj).getName();
			return getFileImage(name);
		}
		return null;
	}

	private String getText(ModelEntry entry) {
		IPluginModelBase model = entry.getActiveModel();
		String text = sharedProvider.getText(model);
		if (model.isEnabled() == false)
			text += " - disabled";
		return text;
	}

	private String getText(FileAdapter file) {
		return file.getFile().getName();
	}

	private Image getImage(ModelEntry entry) {
		IPluginModelBase model = entry.getActiveModel();
		if (model.getUnderlyingResource() != null)
			return projectImage;
		if (model instanceof IPluginModel)
			return sharedProvider.getObjectImage(
				(IPlugin) model.getPluginBase(),
				true,
				entry.isInJavaSearch());
		else
			return sharedProvider.getObjectImage(
				(IFragment) model.getPluginBase(),
				true,
				entry.isInJavaSearch());
	}

	private Image getImage(FileAdapter fileAdapter) {
		if (fileAdapter.isDirectory()) {
			return folderImage;
		}
		return getFileImage(fileAdapter.getFile().getName());
	}
	
	private Image getFileImage(String fileName) {
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				fileName);
		return sharedProvider.get(desc);
	}
}