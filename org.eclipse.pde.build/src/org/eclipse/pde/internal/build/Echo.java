package org.eclipse.pde.internal.core;

import org.apache.tools.ant.Project;

public class Echo extends org.apache.tools.ant.taskdefs.Echo {

public void setProject(Project value) {
	project = value;
}
}
