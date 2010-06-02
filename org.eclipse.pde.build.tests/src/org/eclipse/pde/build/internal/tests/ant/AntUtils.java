/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.ant;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Parallel;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.Type;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.pde.build.tests.Activator;

public class AntUtils {

	/* This is somewhat destructive to the target, as resolving UnknownTasks
	 * results in Types being filtered from the getTasks list and no longer
	 * being accessible.
	 */
	static public Object getFirstChildByName(Target target, String name) {
		Task[] tasks = target.getTasks();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i].getTaskName().equals(name)) {
				if (tasks[i] instanceof UnknownElement) {
					UnknownElement task = (UnknownElement) tasks[i];
					task.maybeConfigure();
					return task.getRealThing();
				} else {
					return tasks[i];
				}
			}
		}
		return null;
	}

	static public Object[] getChildrenByName(Target target, String name) {
		List list = new ArrayList();
		Task[] tasks = target.getTasks();
		for (int i = 0; i < tasks.length; i++) {
			if (tasks[i].getTaskName().equals(name)) {
				if (tasks[i] instanceof UnknownElement) {
					UnknownElement task = (UnknownElement) tasks[i];
					task.maybeConfigure();
					list.add(task.getRealThing());
				} else {
					list.add(tasks[i]);
				}
			}
		}
		return list.toArray();
	}

	static public Task[] getParallelTasks(Parallel parallel) throws Exception {
		Field nestedField = parallel.getClass().getDeclaredField("nestedTasks");
		nestedField.setAccessible(true);

		Vector nested = (Vector) nestedField.get(parallel);
		return (Task[]) nested.toArray(new Task[nested.size()]);
	}

	static public void setupProject(Project project, Map alternateTasks) {
		setupClasspath();

		List tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			org.eclipse.ant.core.Task coreTask = (org.eclipse.ant.core.Task) iterator.next();

			AntTypeDefinition def = new AntTypeDefinition();
			String name = ProjectHelper.genComponentName(coreTask.getURI(), coreTask.getTaskName());
			def.setName(name);
			if (alternateTasks != null && alternateTasks.containsKey(name))
				def.setClassName((String) alternateTasks.get(name));
			else
				def.setClassName(coreTask.getClassName());
			def.setClassLoader(Activator.getDefault().getClass().getClassLoader());
			def.setAdaptToClass(Task.class);
			def.setAdapterClass(TaskAdapter.class);
			ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
		}

		List types = AntCorePlugin.getPlugin().getPreferences().getTypes();
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			Type type = (Type) iterator.next();
			AntTypeDefinition def = new AntTypeDefinition();
			String name = ProjectHelper.genComponentName(type.getURI(), type.getTypeName());
			def.setName(name);
			def.setClassName(type.getClassName());
			def.setClassLoader(Activator.getDefault().getClass().getClassLoader());
			ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
		}
	}

	static private void setupClasspath() {
		URL[] antClasspath = AntCorePlugin.getPlugin().getPreferences().getURLs();
		StringBuffer buff = new StringBuffer();
		File file = null;
		for (int i = 0; i < antClasspath.length; i++) {
			try {
				file = new File(FileLocator.toFileURL(antClasspath[i]).getPath());
			} catch (IOException e) {
				continue;
			}
			buff.append(file.getAbsolutePath());
			buff.append("; "); //$NON-NLS-1$
		}

		org.apache.tools.ant.types.Path systemClasspath = new org.apache.tools.ant.types.Path(null, buff.substring(0, buff.length() - 2));
		org.apache.tools.ant.types.Path.systemClasspath = systemClasspath;
	}
}
