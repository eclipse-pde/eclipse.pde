package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.ui.editor.ModifiedTextPropertyDescriptor;
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