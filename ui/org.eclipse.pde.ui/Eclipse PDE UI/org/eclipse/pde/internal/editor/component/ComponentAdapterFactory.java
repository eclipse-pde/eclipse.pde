package org.eclipse.pde.internal.editor.component;

import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.core.runtime.IAdapterFactory;
import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.graphics.*;

public class ComponentAdapterFactory implements IAdapterFactory {
	private Image errorImage;

public ComponentAdapterFactory() {
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
	if (object instanceof IComponentURLElement)
		return getURLProperties((IComponentURLElement)object);
	if (object instanceof IComponentPlugin || object instanceof IComponentFragment)
		return getReferenceProperties((IComponentReference)object);
	return null;
}
private IPropertySource getReferenceProperties(IComponentReference ref) {
	return new ReferencePropertySource(ref, errorImage);
}
private IPropertySource getURLProperties(IComponentURLElement element) {
	return new URLElementPropertySource(element);
}
}
