package org.eclipse.pde.internal.core;

import org.apache.tools.ant.Project;

public class Mkdir extends org.apache.tools.ant.taskdefs.Mkdir {

public void execute() {
	super.execute();
}
public void setProject(Project value) {
	project = value;
}
}
