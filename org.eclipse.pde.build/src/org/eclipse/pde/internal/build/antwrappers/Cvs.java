package org.eclipse.pde.internal.core.antwrappers;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.Project;
import org.eclipse.pde.internal.core.*;

public class Cvs extends org.apache.tools.ant.taskdefs.Cvs {
/**
 * need to be able to set the project explicitly so we can construct
 * this task from code rather than via xml parsing.
 */
public void setProject(Project value) {
	project = value;
}
}