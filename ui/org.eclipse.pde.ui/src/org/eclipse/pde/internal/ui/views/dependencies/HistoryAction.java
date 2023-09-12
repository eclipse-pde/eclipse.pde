/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.PlatformUI;

/**
 * Action used for the open historical input again in DependenciesView
 */
public class HistoryAction extends Action {
	/**
	 * Image descriptor for an image.
	 */
	public class ImageImageDescriptor extends ImageDescriptor {
		private final Image fImage;

		/**
		 * Constructor for ImagImageDescriptor.
		 */
		public ImageImageDescriptor(Image image) {
			super();
			fImage = image;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && getClass().equals(obj.getClass()) && fImage.equals(((ImageImageDescriptor) obj).fImage);
		}

		@Override
		public ImageData getImageData() {
			return fImage.getImageData();
		}

		@Override
		public int hashCode() {
			return fImage.hashCode();
		}
	}

	private final String fElement;

	private final DependenciesView fView;

	public HistoryAction(DependenciesView view, String element) {
		super();
		fView = view;
		fElement = element;

		String elementName = element.toString();
		setText(elementName);
		setImageDescriptor(getImageDescriptor(elementName));
		setDisabledImageDescriptor(PDEPluginImages.DESC_PLUGIN_OBJ);

		setDescription(NLS.bind(PDEUIMessages.HistoryAction_description, elementName));
		setToolTipText(NLS.bind(PDEUIMessages.HistoryAction_tooltip, elementName));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IHelpContextIds.HISTORY_ACTION);
	}

	private ImageDescriptor getImageDescriptor(String element) {
		DependenciesLabelProvider imageProvider = new DependenciesLabelProvider(false);
		ImageDescriptor desc = new ImageImageDescriptor(imageProvider.getImage(element));
		imageProvider.dispose();

		return desc;
	}

	@Override
	public void run() {
		fView.gotoHistoryEntry(fElement);
	}

}
