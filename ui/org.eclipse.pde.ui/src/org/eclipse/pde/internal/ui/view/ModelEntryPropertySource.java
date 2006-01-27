/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class ModelEntryPropertySource implements IPropertySource {
	private IPropertyDescriptor [] descriptors;
	private ModelEntry entry;

	/**
	 * Constructor for FileAdapterPropertySource.
	 */
	public ModelEntryPropertySource() {
		super();
	}
	
	public void setEntry(ModelEntry entry) {
		this.entry = entry;
	}

	/**
	 * @see IPropertySource#getEditableValue()
	 */
	public Object getEditableValue() {
		return null;
	}

	/**
	 * @see IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors==null) {
			descriptors = new IPropertyDescriptor[8];
			descriptors[0] = new PropertyDescriptor("kind", "kind"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[1] = new PropertyDescriptor("name", "name"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[2] = new PropertyDescriptor("fragment", "fragment"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[3] = new PropertyDescriptor("path", "path"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[4] = new PropertyDescriptor("id", "id"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[5] = new PropertyDescriptor("version", "version"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[6] = new PropertyDescriptor("provider", "provider"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[7] = new PropertyDescriptor("enabled", "enabled"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return descriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		String key = id.toString();
		IPluginModelBase model = entry.getActiveModel();
		IResource resource = model.getUnderlyingResource();
		if (key.equals("enabled")) //$NON-NLS-1$
			return model.isEnabled()?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
		if (key.equals("kind")) //$NON-NLS-1$
			return resource!=null?"workspace":"external"; //$NON-NLS-1$ //$NON-NLS-2$
		if (key.equals("fragment")) //$NON-NLS-1$
			return model.isFragmentModel()?"yes":"no"; //$NON-NLS-1$ //$NON-NLS-2$
		if (key.equals("name")) //$NON-NLS-1$
			return model.getPluginBase().getTranslatedName();
		if (key.equals("path")) { //$NON-NLS-1$
			if (resource!=null)
				return resource.getLocation().toOSString();
			return model.getInstallLocation();
		}
		if (key.equals("id")) //$NON-NLS-1$
			return model.getPluginBase().getId();
		if (key.equals("version")) //$NON-NLS-1$
			return model.getPluginBase().getVersion();
		if (key.equals("provider")) //$NON-NLS-1$
			return model.getPluginBase().getProviderName();
		return null;
	}

	/**
	 * @see IPropertySource#isPropertySet(Object)
	 */
	public boolean isPropertySet(Object id) {
		return false;
	}

	/**
	 * @see IPropertySource#resetPropertyValue(Object)
	 */
	public void resetPropertyValue(Object id) {
	}

	/**
	 * @see IPropertySource#setPropertyValue(Object, Object)
	 */
	public void setPropertyValue(Object id, Object value) {
	}

}
