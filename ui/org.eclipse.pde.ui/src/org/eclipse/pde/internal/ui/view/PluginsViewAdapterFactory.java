package org.eclipse.pde.internal.ui.view;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.pde.internal.core.*;

public class PluginsViewAdapterFactory implements IAdapterFactory {
	private FileAdapterPropertySource adapterPropertySource;
	private ModelEntryPropertySource entryPropertySource;

public Object getAdapter(Object adaptableObject, Class adapterType) {
	if (adapterType.equals(IPropertySource.class)) return getProperties(adaptableObject);
	return null;
}

public java.lang.Class[] getAdapterList() {
	return new Class[] { IPropertySource.class };
}

private IPropertySource getProperties(Object object) {
	if (object instanceof FileAdapter) {
		if (adapterPropertySource==null)
			adapterPropertySource = new FileAdapterPropertySource();
		adapterPropertySource.setAdapter((FileAdapter)object);
		return adapterPropertySource;
	}
	if (object instanceof ModelEntry) {
		if (entryPropertySource==null)
			entryPropertySource = new ModelEntryPropertySource();
		entryPropertySource.setEntry((ModelEntry)object);
		return entryPropertySource;
	}
	return null;
}
}
