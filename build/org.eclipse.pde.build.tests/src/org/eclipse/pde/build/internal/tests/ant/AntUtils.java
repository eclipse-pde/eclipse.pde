/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.ant;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskAdapter;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Parallel;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.Type;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.pde.build.tests.Activator;

public class AntUtils {

	/*
	 * This is somewhat destructive to the target, as resolving UnknownTasks results
	 * in Types being filtered from the getTasks list and no longer being
	 * accessible.
	 */
	static public Object getFirstChildByName(Target target, String name) {
		Task[] tasks = target.getTasks();
		for (Task task2 : tasks) {
			if (task2.getTaskName().equals(name)) {
				if (task2 instanceof UnknownElement task) {
					task.maybeConfigure();
					return task.getRealThing();
				}
				return task2;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static public Object[] getChildrenByName(Target target, String name) {
		@SuppressWarnings("rawtypes")
		List list = new ArrayList();
		Task[] tasks = target.getTasks();
		for (Task task2 : tasks) {
			if (task2.getTaskName().equals(name)) {
				if (task2 instanceof UnknownElement task) {
					task.maybeConfigure();
					list.add(task.getRealThing());
				} else {
					list.add(task2);
				}
			}
		}
		return list.toArray();
	}

	static public Task[] getParallelTasks(Parallel parallel) throws Exception {
		Field nestedField = parallel.getClass().getDeclaredField("nestedTasks");
		nestedField.setAccessible(true);

		@SuppressWarnings("unchecked")
		Vector<Task> nested = (Vector<Task>) nestedField.get(parallel);
		return nested.toArray(new Task[nested.size()]);
	}

	static public void setupProject(Project project, Map<String, String> alternateTasks) {
		setupClasspath();

		List<org.eclipse.ant.core.Task> tasks = AntCorePlugin.getPlugin().getPreferences().getTasks();
		for (org.eclipse.ant.core.Task coreTask : tasks) {
			AntTypeDefinition def = new AntTypeDefinition();
			String name = ProjectHelper.genComponentName(coreTask.getURI(), coreTask.getTaskName());
			def.setName(name);
			if (alternateTasks != null && alternateTasks.containsKey(name)) {
				def.setClassName(alternateTasks.get(name));
			} else {
				def.setClassName(coreTask.getClassName());
			}
			def.setClassLoader(Activator.getDefault().getClass().getClassLoader());
			def.setAdaptToClass(Task.class);
			def.setAdapterClass(TaskAdapter.class);
			ComponentHelper.getComponentHelper(project).addDataTypeDefinition(def);
		}

		List<Type> types = AntCorePlugin.getPlugin().getPreferences().getTypes();
		for (Type type : types) {
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
		for (URL element : antClasspath) {
			try {
				file = new File(FileLocator.toFileURL(element).getPath());
			} catch (IOException e) {
				continue;
			}
			buff.append(file.getAbsolutePath());
			buff.append("; "); //$NON-NLS-1$
		}

		org.apache.tools.ant.types.Path systemClasspath = new org.apache.tools.ant.types.Path(null,
				buff.substring(0, buff.length() - 2));
		org.apache.tools.ant.types.Path.systemClasspath = systemClasspath;
	}
}
