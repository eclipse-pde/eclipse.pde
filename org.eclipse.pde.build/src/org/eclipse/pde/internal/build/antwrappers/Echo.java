package org.eclipse.pde.internal.core.antwrappers;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.Project;
import org.eclipse.pde.internal.core.*;

public class Echo extends org.apache.tools.ant.taskdefs.Echo {

public void setProject(Project value) {
	project = value;
}
}
