package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.resource.*;
import java.net.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.ischema.*;

public abstract class AbstractSchemaDescriptor implements ISchemaDescriptor {
	private Schema schema;

public AbstractSchemaDescriptor() {
	super();
}
public static ImageDescriptor createAbsoluteImageDescriptor(String imageName) {
	URL url = null;
	try {
		url = new URL(imageName);
	} catch (MalformedURLException e) {
	}
	if (url != null)
		return ImageDescriptor.createFromURL(url);
	else
		return null;
}
protected Schema createSchema() {
	URL url = getSchemaURL();
	if (url==null) return null;
	return new Schema(this, url);
}
public void dispose() {
	if (schema != null && schema.isDisposed() == false) {
		schema.dispose();
		schema = null;
	}
}
public ISchema getSchema() {
	if (schema==null) {
		loadSchema();
	}
	return schema;
}
private void loadSchema() {
	schema = createSchema();
	if (schema != null)
		schema.load();
}
public void reload() {
	if (schema!=null) {
		schema.reload();
	}
}

public abstract boolean isEnabled();
}
