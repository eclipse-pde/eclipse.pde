/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

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
