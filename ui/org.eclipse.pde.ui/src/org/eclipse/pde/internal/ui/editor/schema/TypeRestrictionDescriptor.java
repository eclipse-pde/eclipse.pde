package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.properties.*;

public class TypeRestrictionDescriptor extends PropertyDescriptor {
	private boolean readOnly=false;

public TypeRestrictionDescriptor(String name, String displayName, boolean readOnly) {
	super(name, displayName);
	this.readOnly = readOnly;
}
public CellEditor createPropertyEditor(Composite parent) {
	if (readOnly) return null;
	return new TypeRestrictionCellEditor(parent);
}
public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
	if (getAlwaysIncompatible())
		return false;
	if (anotherProperty instanceof TypeRestrictionDescriptor) {
		TypeRestrictionDescriptor spd = (TypeRestrictionDescriptor) anotherProperty;

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
