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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;

public class NewElementAction extends Action {
	public static final String UNKNOWN_ELEMENT_TAG = PDEPlugin.getResourceString("NewElementAction.generic"); //$NON-NLS-1$
	private ISchemaElement elementInfo;
	private Hashtable counters;
	private IPluginParent parent;
	private IProject project;

public NewElementAction(ISchemaElement elementInfo, IPluginParent parent) {
	this.counters= PDEPlugin.getDefault().getDefaultNameCounters();
	this.elementInfo = elementInfo;
	//this.project = project;
	this.parent = parent;
	setText(getElementName());
	setImageDescriptor(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	IResource resource = parent.getModel().getUnderlyingResource();
	if (resource!=null) project = resource.getProject();
	setEnabled(parent.getModel().isEditable());
}
public String createDefaultClassName(ISchemaAttribute attInfo, int counter) {
	String tag = attInfo.getParent().getName();
	String expectedType = attInfo.getBasedOn();
	String className = ""; //$NON-NLS-1$
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
		if (className.length() > 2 && className.charAt(0) == 'I' && Character.isUpperCase(className.charAt(1)))
			className = className.substring(1);
	}
    String packageName = createDefaultPackageName(project.getName(), className);
	className += counter;
    return packageName + "." + className; //$NON-NLS-1$
}
public String createDefaultName(ISchemaAttribute attInfo, int counter) {
	if (attInfo.getType().getName().equals("boolean")) //$NON-NLS-1$
		return "true"; //$NON-NLS-1$
	
	String tag = attInfo.getParent().getName();
	return project.getName() + "." + tag + counter; //$NON-NLS-1$
}
private String getCounterKey(ISchemaElement elementInfo) {
	return elementInfo.getSchema().getQualifiedPointId()+"."+elementInfo.getName(); //$NON-NLS-1$
}
private String getElementName() {
	return elementInfo!=null?elementInfo.getName() : UNKNOWN_ELEMENT_TAG;
}
private void initializeAttribute(
	IPluginElement element,
	ISchemaAttribute attInfo,
	int counter)
	throws CoreException {
	String value = null;
	if (attInfo.getKind() == ISchemaAttribute.JAVA)
		value = createDefaultClassName(attInfo, counter);
	else if (attInfo.getUse() == ISchemaAttribute.DEFAULT && attInfo.getValue() != null)
		value = attInfo.getValue().toString();
	else if (attInfo.getType().getRestriction()!= null)
		value = attInfo.getType().getRestriction().getChildren()[0].toString();
	else
		value = createDefaultName(attInfo, counter);

	element.setAttribute(attInfo.getName(), value);
}
private void initializeAttributes(IPluginElement element) throws CoreException {
	ISchemaElement elementInfo = (ISchemaElement)((IPluginElement)element).getElementInfo();
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
		if (attInfo.getUse()==ISchemaAttribute.REQUIRED || attInfo.getUse() == ISchemaAttribute.DEFAULT)
			initializeAttribute(element, attInfo, counter.intValue());
	}
}
public void run() {
	IPluginElement newElement =
		parent.getModel().getFactory().createElement(parent);
	try {
		newElement.setName(getElementName());
		initializeAttributes(newElement);
		parent.add(newElement);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}

public String createDefaultPackageName(String id, String className) {
    StringBuffer buffer = new StringBuffer();
    IStatus status;
    for (int i = 0; i < id.length(); i++) {
        char ch = id.charAt(i);
        if (buffer.length() == 0) {
            if (Character.isJavaIdentifierStart(ch))
                buffer.append(Character.toLowerCase(ch));
        } else {
            if (Character.isJavaIdentifierPart(ch))
                buffer.append(ch);
            else if (ch == '.'){
                status = JavaConventions.validatePackageName(buffer.toString());
                if (status.getSeverity() == IStatus.ERROR)
                    buffer.append(className.toLowerCase());
                buffer.append(ch);
            }
        }
    }

    status = JavaConventions.validatePackageName(buffer.toString());
    if (status.getSeverity() == IStatus.ERROR)
        buffer.append(className.toLowerCase());
    
    return buffer.toString();
}
}
