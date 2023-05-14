/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.tasks;

import java.io.File;
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

	@Override
	public void runScript(File script, String target, Map<String, String> properties) {
		Ant task = new Ant();
		task.setLocation(parentTask.getLocation());
		task.setProject(parentTask.getProject());
		task.init();

		for (Entry<String, String> entry : properties.entrySet()) {
			Property antProperty = task.createProperty();
			antProperty.setName(entry.getKey());
			antProperty.setValue(entry.getValue());
		}

		task.setTarget(target);
		task.setInheritAll(false);
		task.setInheritRefs(false);
		task.setDir(script.getParentFile());
		task.setAntfile(script.getName());
		task.execute();
	}
}
