package org.eclipse.pde.internal.editor.manifest;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.properties.*;

public class ResourceAttributeDescriptor extends PropertyDescriptor {
	private boolean readOnly=false;

public ResourceAttributeDescriptor(String name, String displayName, boolean readOnly) {
	super(name, displayName);
	this.readOnly = readOnly;
}
public CellEditor createPropertyEditor(Composite parent) {
	if (readOnly) return null;
	return new ResourceAttributeCellEditor(parent);
}
public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
	if (getAlwaysIncompatible())
		return false;
	if (anotherProperty instanceof ResourceAttributeDescriptor) {
		ResourceAttributeDescriptor spd = (ResourceAttributeDescriptor) anotherProperty;

		// Compare Name
		if (!spd.getId().equals(getId()))
			return false;

		// Compare DisplayName
		if (!spd.getDisplayName().equals(getDisplayName()))
			return false;

		// Compare Category
		if (getCategory() == null) {
			if (spd.getCategory() != null)
				return false;
		} else {
			if (!getCategory().equals(spd.getCategory()))
				return false;
		}

		// Nothing was different.
		return true;
	}
	return false;
}
}
