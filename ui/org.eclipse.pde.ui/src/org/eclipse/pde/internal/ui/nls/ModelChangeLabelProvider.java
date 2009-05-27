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
package org.eclipse.pde.internal.ui.nls;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;

public class ModelChangeLabelProvider extends PDELabelProvider {

	private Image manifestImage;
	private Image xmlImage;

	public ModelChangeLabelProvider() {
		xmlImage = PDEPluginImages.DESC_PLUGIN_MF_OBJ.createImage();
		manifestImage = PDEPluginImages.DESC_PAGE_OBJ.createImage();
	}

	public String getText(Object obj) {
		if (obj instanceof ModelChange)
			return getObjectText(((ModelChange) obj).getParentModel().getPluginBase());
		if (obj instanceof ModelChangeFile)
			return getObjectText((ModelChangeFile) obj);
		return super.getText(obj);
	}

	private String getObjectText(ModelChangeFile pair) {
		StringBuffer text = new StringBuffer(pair.getFile().getName());
		int count = pair.getNumChanges();
		text.append(" ["); //$NON-NLS-1$
		text.append(count);
		if (count == 1)
			text.append(PDEUIMessages.ModelChangeLabelProvider_instance);
		else
			text.append(PDEUIMessages.ModelChangeLabelProvider_instances);
		text.append("]"); //$NON-NLS-1$

		return text.toString();
	}

	public Image getImage(Object obj) {
		if (obj instanceof ModelChange) {
			IPluginModelBase model = ((ModelChange) obj).getParentModel();
			if (model instanceof IPluginModel)
				return getObjectImage(((IPluginModel) model).getPlugin(), false, false);
			if (model instanceof IFragmentModel)
				return getObjectImage(((IFragmentModel) model).getFragment(), false, false);
		}
		if (obj instanceof ModelChangeFile)
			return getObjectImage((ModelChangeFile) obj);
		return super.getImage(obj);
	}

	private Image getObjectImage(ModelChangeFile file) {
		String type = file.getFile().getFileExtension();
		if ("xml".equalsIgnoreCase(type)) //$NON-NLS-1$
			return xmlImage;
		if ("MF".equalsIgnoreCase(type)) //$NON-NLS-1$
			return manifestImage;
		return null;
	}

	public void dispose() {
		if (manifestImage != null)
			manifestImage.dispose();
		if (xmlImage != null)
			xmlImage.dispose();
		super.dispose();
	}
}
