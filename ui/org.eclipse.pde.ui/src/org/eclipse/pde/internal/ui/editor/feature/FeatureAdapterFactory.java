package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.ui.model.ifeature.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.ischema.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.IAdapterFactory;
import java.util.*;

import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.*;

public class FeatureAdapterFactory implements IAdapterFactory {
	private Image errorImage;

public FeatureAdapterFactory() {
	errorImage = PDEPluginImages.DESC_ERROR_ST_OBJ.createImage();
}
public void dispose() {
	errorImage.dispose();
}
public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adapterType.equals(IPropertySource.class)) return getProperties(adaptableObject);
	return null;
}
public java.lang.Class[] getAdapterList() {
	return new Class[] { IPropertySource.class };
}
private IPropertySource getProperties(Object object) {
	if (object instanceof IFeatureURLElement)
		return getURLProperties((IFeatureURLElement)object);
	if (object instanceof IFeaturePlugin)
		return getReferenceProperties((IFeaturePlugin)object);
	return null;
}
private IPropertySource getReferenceProperties(IFeaturePlugin ref) {
	return new ReferencePropertySource(ref, errorImage);
}
private IPropertySource getURLProperties(IFeatureURLElement element) {
	return new URLElementPropertySource(element);
}
}
