package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.model.plugin.*;

public class JavaAttributeValue extends ResourceAttributeValue {
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;

public JavaAttributeValue(IProject project, IPluginModelBase model, ISchemaAttribute attInfo, String className) {
	super(project, className);
	this.attInfo = attInfo;
	this.model = model;
}
public ISchemaAttribute getAttributeInfo() {
	return attInfo;
}
public IPluginModelBase getModel() {
	return model;
}
public String getClassName() {
	return getStringValue();
}
}
