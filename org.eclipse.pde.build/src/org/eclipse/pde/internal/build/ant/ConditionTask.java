package org.eclipse.pde.internal.build.ant;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
/**
 * Represents an Ant condition.
 */
public class ConditionTask implements ITask {

	protected String property;
	protected String value;
	protected Condition condition;

public ConditionTask(String property, String value, Condition condition) {
	this.property = property;
	this.value = value;
	this.condition = condition;
}

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
