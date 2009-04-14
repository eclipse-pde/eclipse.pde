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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Provides text and image labels for BundleInfo and IResolveBundle objects.
 */
public class BundleInfoLabelProvider extends LabelProvider {

	boolean fShowVersion = true;

	public BundleInfoLabelProvider() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public BundleInfoLabelProvider(boolean showVersion) {
		fShowVersion = showVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof BundleInfo) {
			StringBuffer buf = new StringBuffer();
			buf.append(((BundleInfo) element).getSymbolicName());
			if (fShowVersion) {
				String version = ((BundleInfo) element).getVersion();
				if (version != null) {
					buf.append(' ');
					buf.append('(');
					buf.append(version);
					buf.append(')');
				}
			}
			return buf.toString();
		} else if (element instanceof IResolvedBundle) {
			IResolvedBundle bundle = ((IResolvedBundle) element);
			if (!bundle.getStatus().isOK()) {
				// TODO Better error message for missing bundles
				return bundle.getStatus().getMessage();
			}
			return getText(bundle.getBundleInfo());
		} else if (element instanceof IStatus) {
			return ((IStatus) element).getMessage();
		} else if (element instanceof IPath) {
			return ((IPath) element).removeFirstSegments(1).toString();
		}
		return super.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IResolvedBundle) {
			IResolvedBundle bundle = (IResolvedBundle) element;
			int flag = 0;
			if (bundle.getStatus().getSeverity() == IStatus.WARNING || bundle.getStatus().getSeverity() == IStatus.INFO) {
				flag = SharedLabelProvider.F_WARNING;
			} else if (bundle.getStatus().getSeverity() == IStatus.ERROR) {
				flag = SharedLabelProvider.F_ERROR;
			}

			if (bundle.isFragment()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ, flag);
			} else if (bundle.isSourceBundle()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ, flag);
			} else {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ, flag);
			}
		} else if (element instanceof BundleInfo) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
		} else if (element instanceof IStatus) {
			int severity = ((IStatus) element).getSeverity();
			if (severity == IStatus.WARNING || severity == IStatus.INFO) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			} else if (severity == IStatus.ERROR) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
		} else if (element instanceof IPath) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		return super.getImage(element);
	}

}
