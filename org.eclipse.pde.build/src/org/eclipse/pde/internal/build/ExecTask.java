package org.eclipse.pde.internal.core;

import org.apache.tools.ant.Project;

public class ExecTask extends org.apache.tools.ant.taskdefs.ExecTask {

public void setProject(Project value) {
	project = value;
}
}
