package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.net.*;
import org.eclipse.ui.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.base.model.feature.*;

public abstract class FeaturePropertySource implements IPropertySource {
	protected IFeatureObject object;

public FeaturePropertySource(IFeatureObject object) {
	this.object = object;
}
protected PropertyDescriptor createTextPropertyDescriptor(String name, String displayName) {
	if (isEditable()) return new ModifiedTextPropertyDescriptor(name, displayName);
	else return new PropertyDescriptor(name, displayName);
}
public Object getEditableValue() {
	return null;
}
public boolean isEditable() {
	return object.getModel().isEditable();
}
public boolean isPropertySet(Object property) {
	return false;
}
public void resetPropertyValue(Object property) {
}
protected IPropertyDescriptor[] toDescriptorArray(Vector result) {
	IPropertyDescriptor [] array = new IPropertyDescriptor[result.size()];
	result.copyInto(array);
	return array;
}
}
