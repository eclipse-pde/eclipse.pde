package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.views.properties.*;

public class PortabilityChoiceDescriptor extends PropertyDescriptor {
	private boolean readOnly=false;
	private Choice [] choices;

public PortabilityChoiceDescriptor(String name, String displayName, Choice[] choices, boolean readOnly) {
	super(name, displayName);
	this.readOnly = readOnly;
	this.choices = choices;
}
public CellEditor createPropertyEditor(Composite parent) {
	if (readOnly) return null;
	return new PortabilityChoiceCellEditor(parent, choices);
}
public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
	if (getAlwaysIncompatible())
		return false;
	if (anotherProperty instanceof PortabilityChoiceDescriptor) {
		PortabilityChoiceDescriptor spd = (PortabilityChoiceDescriptor) anotherProperty;

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
