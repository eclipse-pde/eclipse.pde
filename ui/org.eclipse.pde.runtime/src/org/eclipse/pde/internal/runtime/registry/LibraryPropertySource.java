package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.*;

public class LibraryPropertySource extends RegistryPropertySource {
	private ILibrary library;
	public static final String P_PATH = "path";
	public static final String KEY_PATH = "RegistryView.libraryPR.path";
	public static final String KEY_EXPORTED = "RegistryView.libraryPR.exported";
	public static final String KEY_FULLY_EXPORTED = "RegistryView.libraryPR.fullyExported";
	public static final String P_EXPORTED = "exported";
	public static final String P_FULLY_EXPORTED = "fully_exported";

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
		return library.isExported()?"true":"false";
	if (name.equals(P_FULLY_EXPORTED))
		return library.isFullyExported()?"true":"false";
	return null;
}
}
