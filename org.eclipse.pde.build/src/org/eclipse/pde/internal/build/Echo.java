package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.Project;

public class Echo extends org.apache.tools.ant.taskdefs.Echo {

public void setProject(Project value) {
	project = value;
}
}
