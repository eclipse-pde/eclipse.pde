package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.*;
import org.eclipse.ui.views.properties.*;

public abstract class RegistryPropertySource implements IPropertySource {

public Object getEditableValue() {
	return null;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	return null;
}
public boolean isPropertySet(Object id) {
	return false;
}
public void resetPropertyValue(Object id) {}
public void setPropertyValue(Object id, Object value) {}
protected IPropertyDescriptor[] toDescriptorArray(Vector result) {
	IPropertyDescriptor [] array = new IPropertyDescriptor[result.size()];
	result.copyInto(array);
	return array;
}
}
