/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.core.runtime.ILibrary;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.views.properties.*;

public class LibraryPropertySource extends RegistryPropertySource {
	private ILibrary library;
	public static final String P_PATH = "path"; //$NON-NLS-1$
	public static final String KEY_PATH = "RegistryView.libraryPR.path"; //$NON-NLS-1$
	public static final String KEY_EXPORTED = "RegistryView.libraryPR.exported"; //$NON-NLS-1$
	public static final String KEY_FULLY_EXPORTED = "RegistryView.libraryPR.fullyExported"; //$NON-NLS-1$
	public static final String P_EXPORTED = "exported"; //$NON-NLS-1$
	public static final String P_FULLY_EXPORTED = "fully_exported"; //$NON-NLS-1$

public LibraryPropertySource(ILibrary library) {
	this.library = library;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_PATH, PDERuntimePlugin.getResourceString(KEY_PATH)));
	result.addElement(new PropertyDescriptor(P_EXPORTED, PDERuntimePlugin.getResourceString(KEY_EXPORTED)));
	result.addElement(new PropertyDescriptor(P_FULLY_EXPORTED, PDERuntimePlugin.getResourceString(KEY_FULLY_EXPORTED)));
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_PATH))
		return library.getPath().toString();
	if (name.equals(P_EXPORTED))
		return library.isExported()?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
	if (name.equals(P_FULLY_EXPORTED))
		return library.isFullyExported()?"true":"false"; //$NON-NLS-1$ //$NON-NLS-2$
	return null;
}
}
