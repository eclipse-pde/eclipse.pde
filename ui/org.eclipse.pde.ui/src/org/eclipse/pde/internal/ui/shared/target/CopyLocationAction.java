/*******************************************************************************
 *  Copyright (c) 2023 Eclipse Contributors and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import java.nio.file.Path;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CopyLocationAction extends Action {

	private StructuredViewer fViewer;

	private String fLocation;

	public CopyLocationAction(StructuredViewer viewer) {
		fViewer = viewer;

		setText(Messages.CopyLocationAction_copyLocation);
		ISharedImages workbenchImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));

		viewer.addPostSelectionChangedListener(event -> {
			updateEnablement(event.getStructuredSelection());
		});

		updateEnablement(viewer.getStructuredSelection());
	}

	@Override
	public void run() {
		Clipboard clipboard = new Clipboard(fViewer.getControl().getDisplay());
		try {
			clipboard.setContents(new Object[] { fLocation }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	private void updateEnablement(IStructuredSelection selection) {
		fLocation = null;
		if (selection.size() == 1) {
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof TargetBundle targetBundle) {
				BundleInfo bundleInfo = targetBundle.getBundleInfo();
				if (bundleInfo != null) {
					URI location = bundleInfo.getLocation();
					if (location != null && "file".equals(location.getScheme())) { //$NON-NLS-1$
						fLocation = Path.of(location).toString();
					}
				}
			} else if (firstElement instanceof TargetFeature targetFeature) {
				fLocation = targetFeature.getLocation();
			} else if (firstElement instanceof BundleDescription bundleDescription) {
				fLocation = bundleDescription.getLocation();
			}
		}
		setEnabled(fLocation != null);
	}
}
