/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.osgi;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.osgi.OSGiWorkspaceModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class OSGiLabelProvider extends PDELabelProvider {
	public Image getImage(Object obj) {
		if (obj instanceof IBundlePluginBase) {
			return getObjectImage((IBundlePluginBase) obj);
		}
		if (obj instanceof IBundlePluginModelBase) {
			return getObjectImage((IBundlePluginBase)((IBundlePluginModelBase) obj).getPluginBase());
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
				if (OSGiWorkspaceModelManager.isBinaryBundleProject(project)) {
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
}
