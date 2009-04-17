/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;

public class GatheringComputer implements IPathComputer {
	private static final String PROVIDED_PATH = ":PROVIDED:"; //$NON-NLS-1$
	private final LinkedHashMap filesMap = new LinkedHashMap();

	public IPath computePath(File source) {
		String prefix = (String) filesMap.get(source);

		IPath result = null;
		if (prefix.startsWith(PROVIDED_PATH)) {
			// the desired path is provided in the map
			result = new Path(prefix.substring(10));
		} else {
			//else the map contains a prefix which must be stripped from the path
			result = new Path(source.getAbsolutePath());
			IPath rootPath = new Path(prefix);
			result = result.removeFirstSegments(rootPath.matchingFirstSegments(result));
		}
		return result.setDevice(null);
	}

	public void reset() {
		// nothing

	}

	public void addAll(GatheringComputer computer) {
		filesMap.putAll(computer.filesMap);
	}

	public void addFiles(String prefix, String[] files) {
		for (int i = 0; i < files.length; i++) {
			filesMap.put(new File(prefix, files[i]), prefix);
		}
	}

	public void addFile(String prefix, String file) {
		filesMap.put(new File(prefix, file), prefix);
	}

	public void addFile(String computedPath, File file) {
		filesMap.put(file, PROVIDED_PATH + computedPath);
	}

	public File[] getFiles() {
		Set keys = filesMap.keySet();
		return (File[]) keys.toArray(new File[keys.size()]);
	}

	public int size() {
		return filesMap.size();
	}
}