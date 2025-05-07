/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
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
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.tasks;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.pde.bnd.ui.Central;
import org.eclipse.pde.bnd.ui.Workspaces;
import org.eclipse.pde.bnd.ui.internal.FileUtils;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;

public class BndFileCapReqLoader extends BndBuilderCapReqLoader {

	private Builder builder;

	public BndFileCapReqLoader(File bndFile) {
		super(bndFile);
	}

	@Override
	protected synchronized Builder getBuilder() throws Exception {
		if (builder == null) {
			Builder b;

			IFile[] wsfiles = FileUtils.getWorkspaceFiles(file);
			if (wsfiles == null || wsfiles.length == 0)
				throw new Exception("Unable to determine project owner for Bnd file: " + file.getAbsolutePath());

			IProject project = wsfiles[0].getProject();

			// Calculate the manifest
			Workspace ws = Workspaces.getWorkspace(project).orElse(null);
			Project bndProject = Central.getProject(ws, project);
			if (bndProject == null)
				return null;
			if (file.getName()
				.equals(Project.BNDFILE)) {
				ProjectBuilder pb = bndProject.getBuilder(null);
				boolean close = true;
				try {
					b = pb.getSubBuilders()
						.get(0);
					if (b == pb) {
						close = false;
					} else {
						pb.removeClose(b);
					}
				} finally {
					if (close) {
						pb.close();
					}
				}
			} else {
				b = bndProject.getSubBuilder(file);
			}

			if (b == null) {
				b = new Builder();
				b.setProperties(file);
			}
			b.build();

			builder = b;
		}
		return builder;
	}

	@Override
	public synchronized void close() throws IOException {
		if (builder != null)
			builder.close();
		builder = null;
	}

}
