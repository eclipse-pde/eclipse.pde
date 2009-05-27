/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
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
		private Image fImage;

		/**
		 * Constructor for ImagImageDescriptor.
		 */
		public ImageImageDescriptor(Image image) {
			super();
			fImage = image;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see Object#equals(Object)
		 */
		public boolean equals(Object obj) {
			return (obj != null) && getClass().equals(obj.getClass()) && fImage.equals(((ImageImageDescriptor) obj).fImage);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ImageDescriptor#getImageData()
		 */
		public ImageData getImageData() {
			return fImage.getImageData();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see Object#hashCode()
		 */
		public int hashCode() {
			return fImage.hashCode();
		}
	}

	private String fElement;

	private DependenciesView fView;

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

	/*
	 * @see Action#run()
	 */
	public void run() {
		fView.gotoHistoryEntry(fElement);
	}

}
