package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.core.runtime.*;
import java.util.*;

public class ExtensionPointAdapter extends ParentAdapter {


public ExtensionPointAdapter(Object object) {
	super(object);
}
protected Object [] createChildren() {
	IExtensionPoint extensionPoint = (IExtensionPoint)getObject();

	IExtension [] extensions = extensionPoint.getExtensions();
	Object [] result = new Object[extensions.length];
	for (int i=0; i<extensions.length; i++) {
		IExtension extension = extensions[i];
		result[i] = new ExtensionAdapter(extension);
	}
	return result;
}
}
