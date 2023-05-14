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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;

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
		folderImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		projectImage = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		sharedProvider.connect(this);
	}

	@Override
	public void dispose() {
		sharedProvider.disconnect(this);
		super.dispose();
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof IPluginModelBase) {
			return getText((IPluginModelBase) obj);
		}

		if (obj instanceof FileAdapter) {
			return getText((FileAdapter) obj);
		}

		if (obj instanceof IPackageFragmentRoot) {
			// use the short name
			return ((IPackageFragmentRoot) obj).getPath().lastSegment();
		}

		if (obj instanceof IJavaElement) {
			return ((IJavaElement) obj).getElementName();
		}

		if (obj instanceof IStorage) {
			return ((IStorage) obj).getName();
		}

		if (obj instanceof IDeferredWorkbenchAdapter) {
			return ((IDeferredWorkbenchAdapter) obj).getLabel(obj);
		}

		return super.getText(obj);
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof IPluginModelBase) {
			return getImage((IPluginModelBase) obj);
		}

		if (obj instanceof FileAdapter) {
			return getImage((FileAdapter) obj);
		}

		if (obj instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) obj;
			boolean hasSource = false;

			try {
				hasSource = root.getSourceAttachmentPath() != null;
			} catch (JavaModelException e) {
			}
			return JavaUI.getSharedImages().getImage(hasSource ? org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE : org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);
		}

		if (obj instanceof IPackageFragment) {
			return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PACKAGE);
		}

		if (obj instanceof ICompilationUnit) {
			return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CUNIT);
		}

		if (obj instanceof IClassFile) {
			return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CFILE);
		}

		if (obj instanceof IStorage) {
			if (obj instanceof IJarEntryResource && !((IJarEntryResource) obj).isFile())
				return folderImage;
			return getFileImage(((IStorage) obj).getName());
		}

		return null;
	}

	private String getText(IPluginModelBase model) {
		String text = sharedProvider.getText(model);
		if (!model.isEnabled())
			text = NLS.bind(PDEUIMessages.PluginsView_disabled, text);
		return text;
	}

	private String getText(FileAdapter file) {
		return file.getFile().getName();
	}

	private Image getImage(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null)
			return projectImage;

		if (model instanceof IPluginModel)
			return sharedProvider.getObjectImage((IPlugin) model.getPluginBase(), true, isInJavaSearch(model));

		return sharedProvider.getObjectImage((IFragment) model.getPluginBase(), true, isInJavaSearch(model));
	}

	private boolean isInJavaSearch(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
		return manager.isInJavaSearch(id);
	}

	private Image getImage(FileAdapter fileAdapter) {
		if (fileAdapter.isDirectory()) {
			return folderImage;
		}
		return getFileImage(fileAdapter.getFile().getName());
	}

	private Image getFileImage(String fileName) {
		ImageDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(fileName);
		return sharedProvider.get(desc);
	}
}
