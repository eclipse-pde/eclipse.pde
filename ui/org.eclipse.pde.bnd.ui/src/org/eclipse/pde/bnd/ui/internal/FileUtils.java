/*******************************************************************************
 * Copyright (c) 2013, 2021 bndtools project and others.
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
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - ongoing enhancements
 *     Jürgen Albert <j.albert@data-in-motion.biz> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.internal;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

public class FileUtils {

	
	public static IFile[] getWorkspaceFiles(File javaFile) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();
		IFile[] candidates = root.findFilesForLocationURI(javaFile.toURI());
		Arrays.sort(candidates, (a, b) -> Integer.compare(a.getFullPath()
			.segmentCount(),
			b.getFullPath()
				.segmentCount()));
		return candidates;
	}
}
