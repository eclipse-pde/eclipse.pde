package org.eclipse.pde.internal.core.ant;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.*;
/**
 * Represents an Ant condition.
 */
public class Condition {

	protected String type;
	protected List singleConditions;
	protected List nestedConditions;

	/**
	 * Types of conditions.
	 */
	public static final String TYPE_AND = "and";


public Condition() {
	singleConditions = new ArrayList(5);
	nestedConditions = new ArrayList(5);
}

public Condition(String type) {
	this();
	this.type = type;
}

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

public void addEquals(String arg1, String arg2) {
	StringBuffer condition = new StringBuffer();
	condition.append("<equals ");
	condition.append("arg1=\"");
	condition.append(arg1);
	condition.append("\" ");
	condition.append("arg2=\"");
	condition.append(arg2);
	condition.append("\"/>");
	singleConditions.add(condition.toString());
}

public void add(Condition condition) {
	nestedConditions.add(condition);
}

}