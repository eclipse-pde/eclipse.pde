package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;
import java.util.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.action.*;

public class NewElementAction extends Action {
	public static final String UNKNOWN_ELEMENT_TAG = "Generic";
	private ISchemaElement elementInfo;
	private Hashtable counters;
	private IPluginParent parent;
	private IProject project;

public NewElementAction(ISchemaElement elementInfo, IPluginParent parent) {
	this.counters= PDEPlugin.getDefault().getDefaultNameCounters();
	this.elementInfo = elementInfo;
	this.project = project;
	this.parent = parent;
	setText(getElementName());
	setImageDescriptor(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	IResource resource = parent.getModel().getUnderlyingResource();
	if (resource!=null) project = resource.getProject();
}
public String createDefaultClassName(ISchemaAttribute attInfo, int counter) {
	String tag = attInfo.getParent().getName();
	String projectName = project.getName();
	String packageName = projectName;
	String expectedType = attInfo.getBasedOn();
	String className = "";
	if (expectedType == null) {
		StringBuffer buf = new StringBuffer(tag);
		buf.setCharAt(0, Character.toUpperCase(tag.charAt(0)));
		className = buf.toString();
	} else {
		//package will be the same as the plugin ID
		// class name will be generated based on the required interface
		className = expectedType;
		int dotLoc = className.lastIndexOf('.');
		if (dotLoc != -1)
			className = className.substring(dotLoc + 1);
		if (className.charAt(0) == 'I')
			className = className.substring(1);
	}
	className += counter;
	return packageName + "." + className;
}
public String createDefaultName(ISchemaAttribute attInfo, int counter) {
	String tag = attInfo.getParent().getName();
	return project.getName() + "." + tag + counter;
}
private String getCounterKey(ISchemaElement elementInfo) {
	return elementInfo.getSchema().getPointId()+"."+elementInfo.getName();
}
private String getElementName() {
	return elementInfo!=null?elementInfo.getName() : UNKNOWN_ELEMENT_TAG;
}
private void initializeRequiredAttribute(
	IPluginElement element,
	ISchemaAttribute attInfo,
	int counter)
	throws CoreException {
	String value = null;
	if (attInfo.getKind() == ISchemaAttribute.JAVA)
		value = createDefaultClassName(attInfo, counter);
	else
		value = createDefaultName(attInfo, counter);

	element.setAttribute(attInfo.getName(), value);
}
private void initializeRequiredAttributes(IPluginElement element) throws CoreException {
	ISchemaElement elementInfo = element.getElementInfo();
	if (elementInfo==null) return;
	String counterKey = getCounterKey(elementInfo);
	Integer counter = (Integer)counters.get(counterKey);
	if (counter==null) {
		counter = new Integer(1);
	}
	else counter = new Integer(counter.intValue() + 1);
	counters.put(counterKey, counter);
	ISchemaAttribute [] attributes = elementInfo.getAttributes();
	for (int i=0; i<attributes.length; i++) {
		ISchemaAttribute attInfo = attributes[i];
		if (attInfo.getUse()!=ISchemaAttribute.REQUIRED) continue;
		initializeRequiredAttribute(element, attInfo, counter.intValue());
	}
}
public void run() {
	IPluginElement newElement =
		parent.getModel().getFactory().createElement(parent);
	try {
		newElement.setName(getElementName());
		initializeRequiredAttributes(newElement);
		parent.add(newElement);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
}
