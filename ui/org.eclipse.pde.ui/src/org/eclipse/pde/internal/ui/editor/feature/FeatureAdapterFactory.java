/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySource;

public class FeatureAdapterFactory implements IAdapterFactory {
	private Image errorImage;

	public FeatureAdapterFactory() {
		errorImage = PDEPluginImages.DESC_ERROR_ST_OBJ.createImage();
	}
	public void dispose() {
		errorImage.dispose();
	}
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.equals(IPropertySource.class))
			return getProperties(adaptableObject);
		return null;
	}
	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class };
	}
	private IPropertySource getProperties(Object object) {
		if (object instanceof IFeatureURLElement)
			return getURLProperties((IFeatureURLElement) object);
		if (object instanceof IFeaturePlugin)
			return getReferenceProperties((IFeaturePlugin) object);
		if (object instanceof IFeatureData)
			return getDataProperties((IFeatureData) object);
		if (object instanceof IFeatureChild)
			return getChildProperties((IFeatureChild) object);
		return null;
	}
	private IPropertySource getReferenceProperties(IFeaturePlugin ref) {
		return new ReferencePropertySource(ref, errorImage);
	}
	private IPropertySource getURLProperties(IFeatureURLElement element) {
		return new URLElementPropertySource(element);
	}

	private IPropertySource getDataProperties(IFeatureData data) {
		return new FeatureEntryPropertySource(data);
	}

	private IPropertySource getChildProperties(IFeatureChild child) {
		return new FeatureChildPropertySource(child);
	}
}
