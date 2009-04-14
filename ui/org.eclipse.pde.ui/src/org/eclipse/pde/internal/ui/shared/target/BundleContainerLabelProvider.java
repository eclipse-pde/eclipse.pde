/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for the a tree containing bundle containers
 */
class BundleContainerLabelProvider extends BundleInfoLabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.BundleInfoLabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		try {
			if (element instanceof FeatureBundleContainer) {
				FeatureBundleContainer container = (FeatureBundleContainer) element;
				String version = container.getFeatureVersion();
				if (version != null) {
					return MessageFormat.format(Messages.BundleContainerTable_5, new String[] {container.getFeatureId(), version, container.getLocation(false), getIncludedBundlesLabel(container)});
				}
				return MessageFormat.format(Messages.BundleContainerTable_6, new String[] {container.getFeatureId(), container.getLocation(false), getIncludedBundlesLabel(container)});
			} else if (element instanceof DirectoryBundleContainer) {
				DirectoryBundleContainer container = (DirectoryBundleContainer) element;
				return MessageFormat.format(Messages.BundleContainerTable_7, new String[] {container.getLocation(false), getIncludedBundlesLabel(container)});
			} else if (element instanceof ProfileBundleContainer) {
				ProfileBundleContainer container = (ProfileBundleContainer) element;
				return MessageFormat.format(Messages.BundleContainerTable_7, new String[] {container.getLocation(false), getIncludedBundlesLabel(container)});
			}
		} catch (CoreException e) {
			return MessageFormat.format(Messages.BundleContainerTable_4, new String[] {e.getMessage()});
		}
		return super.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.BundleInfoLabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IBundleContainer) {
			int flag = 0;
			IBundleContainer container = (IBundleContainer) element;
			if (container.isResolved()) {
				IStatus status = container.getBundleStatus();
				if (status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
					flag = SharedLabelProvider.F_WARNING;
				} else if (status.getSeverity() == IStatus.ERROR) {
					flag = SharedLabelProvider.F_ERROR;
				}
			}
			if (element instanceof FeatureBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, flag);
			} else if (element instanceof DirectoryBundleContainer) {
				ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
				return PDEPlugin.getDefault().getLabelProvider().get(image, flag);
			} else if (element instanceof ProfileBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION, flag);
			}
		}
		return super.getImage(element);
	}

	/**
	 * Returns a label describing the number of bundles included (ex. 5 of 10 plug-ins)
	 * or an empty string if there is a problem determining the number of bundles
	 * @param container bundle container to check for inclusions
	 * @return string label
	 */
	private String getIncludedBundlesLabel(IBundleContainer container) {
		// TODO Provide convenience methods in IBundleContainer to access all bundles?
		if (!container.isResolved() || (!container.getBundleStatus().isOK() && !container.getBundleStatus().isMultiStatus()) || container.getBundles() == null) {
			return ""; //$NON-NLS-1$
		}

		BundleInfo[] restrictions = container.getIncludedBundles();
		int bundleCount = container.getAllBundles().length;
		String bundleCountString = Integer.toString(bundleCount);

		if (restrictions != null && restrictions.length > bundleCount) {
			// If some bundles are missing, the bundleCount is likely wrong, just do the best we can
			return ""; //$NON-NLS-1$
		}

		return MessageFormat.format(Messages.BundleContainerTable_10, new String[] {restrictions != null ? Integer.toString(restrictions.length) : bundleCountString, bundleCountString});

	}
}