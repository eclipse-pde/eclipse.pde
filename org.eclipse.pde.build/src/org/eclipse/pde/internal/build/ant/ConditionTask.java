/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.ant;

/**
 * Represents an Ant condition.
 */
public class ConditionTask implements ITask {

	protected String property;
	protected String value;
	protected Condition condition;

/**
 * Constructor for the condition.
 *  * @param property * @param value * @param condition */
public ConditionTask(String property, String value, Condition condition) {
	this.property = property;
	this.value = value;
	this.condition = condition;
}

/**
 * @see ITask#print(AntScript, int) */
public void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<condition"); //$NON-NLS-1$
	script.printAttribute("property", property, true); //$NON-NLS-1$
	script.printAttribute("value", value, false); //$NON-NLS-1$
	script.println(">"); //$NON-NLS-1$
	condition.print(script, ++tab);
	script.printTab(--tab);
	script.println("</condition>"); //$NON-NLS-1$
}
}
