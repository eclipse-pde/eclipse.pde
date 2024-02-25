/*******************************************************************************
 * Copyright (c) 2010, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Neil Bartlett <njbartlett@gmail.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.repo;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Path;

import aQute.bnd.build.Project;

public class ProjectBundle implements IAdaptable {

	private final Project			project;
	private final String			bsn;

	ProjectBundle(Project project, String bsn) {
		this.project = project;
		this.bsn = bsn;
	}

	public Project getProject() {
		return project;
	}

	public String getBsn() {
		return bsn;
	}

	@Override
	public String toString() {
		return "ProjectBundle [project=" + project + ", bsn=" + bsn + "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		T result = null;

		if (IFile.class.equals(adapter) || IResource.class.equals(adapter)) {
			try {
				File targetDir = project.getTarget();
				File bundleFile = new File(targetDir, bsn + ".jar");
				if (bundleFile.isFile()) {
					Path path = new Path(bundleFile.getAbsolutePath());
					result = (T) ResourcesPlugin.getWorkspace()
						.getRoot()
						.getFileForLocation(path);
				}
			} catch (Exception e) {
				ILog.get().error(
						MessageFormat.format("Error retrieving bundle {0} from project {1}.", bsn, project.getName()),
						e);
			}
		}
		return result;
	}

	public boolean isSub() {
		return !project.getName()
			.equals(bsn);
	}
}
