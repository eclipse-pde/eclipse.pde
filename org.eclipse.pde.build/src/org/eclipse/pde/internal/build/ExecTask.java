package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.Project;

public class ExecTask extends org.apache.tools.ant.taskdefs.ExecTask {

public void setProject(Project value) {
	project = value;
}
}
