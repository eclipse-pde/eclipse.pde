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
package org.eclipse.pde.internal.ui.view;

import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.ui.views.properties.*;
import java.util.*;
import java.text.DateFormat;

public class FileAdapterPropertySource implements IPropertySource {
	private IPropertyDescriptor [] descriptors;
	private FileAdapter adapter;

	/**
	 * Constructor for FileAdapterPropertySource.
	 */
	public FileAdapterPropertySource() {
		super();
	}
	
	public void setAdapter(FileAdapter adapter) {
		this.adapter = adapter;
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
			descriptors = new IPropertyDescriptor[5];
			descriptors[0] = new PropertyDescriptor("editable", "editable"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[1] = new PropertyDescriptor("last", "last modified"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[2] = new PropertyDescriptor("name", "name"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[3] = new PropertyDescriptor("path", "path"); //$NON-NLS-1$ //$NON-NLS-2$
			descriptors[4] = new PropertyDescriptor("size", "size"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return descriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		String key = id.toString();
		if (key.equals("editable")) //$NON-NLS-1$
			return "false"; //$NON-NLS-1$
		if (key.equals("last")) { //$NON-NLS-1$
			Date date = new Date(adapter.getFile().lastModified());
			return DateFormat.getInstance().format(date);
		}
		if (key.equals("name")) //$NON-NLS-1$
			return adapter.getFile().getName();
		if (key.equals("path")) //$NON-NLS-1$
			return adapter.getFile().getAbsolutePath();
		if (key.equals("size")) //$NON-NLS-1$
			return ""+adapter.getFile().length(); //$NON-NLS-1$
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
