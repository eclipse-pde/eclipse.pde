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

import java.util.*;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.views.properties.*;

public abstract class FeaturePropertySource implements IPropertySource {
	protected IFeatureObject object;

	public FeaturePropertySource(IFeatureObject object) {
		this.object = object;
	}
	protected PropertyDescriptor createTextPropertyDescriptor(
		String name,
		String displayName) {
		if (isEditable())
			return new ModifiedTextPropertyDescriptor(name, displayName);
		else
			return new PropertyDescriptor(name, displayName);
	}

	protected PropertyDescriptor createChoicePropertyDescriptor(
		String name,
		String displayName,
		final String[] choices) {
		if (isEditable()) {
			PropertyDescriptor desc = new ComboBoxPropertyDescriptor(name, displayName, choices);
			desc.setLabelProvider(new LabelProvider() {
				public String getText(Object obj) {
					Integer index = (Integer)obj;
					return choices[index.intValue()];
				}
			});
			return desc;
		} else
			return new PropertyDescriptor(name, displayName);
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
		IPropertyDescriptor[] array = new IPropertyDescriptor[result.size()];
		result.copyInto(array);
		return array;
	}
}
