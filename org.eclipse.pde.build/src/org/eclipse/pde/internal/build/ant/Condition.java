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

import java.util.*;

/**
 * Represents an Ant condition.
 */
public class Condition {

	/**
	 * Types of conditions.
	 */
	protected String type;	
	public static final String TYPE_AND = "and";	//$NON-NLS-1$
	protected List singleConditions;
	protected List nestedConditions;

/**
 * Default constructor for the class. */
public Condition() {
	this.singleConditions = new ArrayList(5);
	this.nestedConditions = new ArrayList(5);
}

public Condition(String type) {
	this();
	this.type = type;
}/**
 * Add this Ant condition to the given Ant script.
 *  * @param script the script to add the condition to * @param tab the number of tabs to use when indenting */
protected void print(AntScript script, int tab) {
	if (type != null)
		script.printStartTag(tab++, type);
	for (Iterator iterator = singleConditions.iterator(); iterator.hasNext();)
		script.printString(tab, (String) iterator.next());
	for (Iterator iterator = nestedConditions.iterator(); iterator.hasNext();) {
		Condition condition = (Condition) iterator.next();
		condition.print(script, tab);
	}
	if (type != null)
		script.printEndTag(--tab, type);
}

/**
 * Add an "equals" condition to this Ant condition.
 *  * @param arg1 the left-hand side of the equals * @param arg2 the right-hand side of the equals */
public void addEquals(String arg1, String arg2) {
	StringBuffer condition = new StringBuffer();
	condition.append("<equals "); //$NON-NLS-1$
	condition.append("arg1=\""); //$NON-NLS-1$
	condition.append(arg1);
	condition.append("\" "); //$NON-NLS-1$
	condition.append("arg2=\""); //$NON-NLS-1$
	condition.append(arg2);
	condition.append("\"/>"); //$NON-NLS-1$
	singleConditions.add(condition.toString());
}

/**
 * Add the given condition to this Ant condition.
 *  * @param condition the condition to add */
public void add(Condition condition) {
	nestedConditions.add(condition);
}

}