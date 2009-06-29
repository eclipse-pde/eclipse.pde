/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.eclipse.pde.internal.build.ant.IScriptRunner;

public class AntScriptRunner implements IScriptRunner {
	private final Task parentTask;

	public AntScriptRunner(Task parent) {
		parentTask = parent;
	}

	public void runScript(File script, String target, Map properties) {
		Ant task = new Ant();
		task.setLocation(parentTask.getLocation());
		task.setProject(parentTask.getProject());
		task.init();

		for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Entry) iterator.next();
			Property antProperty = task.createProperty();
			antProperty.setName((String) entry.getKey());
			antProperty.setValue((String) entry.getValue());
		}

		task.setTarget(target);
		task.setInheritAll(false);
		task.setInheritRefs(false);
		task.setDir(script.getParentFile());
		task.setAntfile(script.getName());
		task.execute();
	}
}
