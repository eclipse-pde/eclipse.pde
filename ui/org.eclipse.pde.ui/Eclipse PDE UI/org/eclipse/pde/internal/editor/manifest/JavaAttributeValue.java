package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.resources.*;

public class JavaAttributeValue extends ResourceAttributeValue {
	private ISchemaAttribute attInfo;

public JavaAttributeValue(IProject project, ISchemaAttribute attInfo, String className) {
	super(project, className);
	this.attInfo = attInfo;
}
public ISchemaAttribute getAttributeInfo() {
	return attInfo;
}
public String getClassName() {
	return getStringValue();
}
}
