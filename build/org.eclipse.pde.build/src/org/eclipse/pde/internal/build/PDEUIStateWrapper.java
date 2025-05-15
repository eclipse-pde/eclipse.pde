/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.State;

public class PDEUIStateWrapper {
	private State state;
	private HashMap<Long, String[]> classpath;
	// Map of source entries to their output folders
	private Map<String, Map<String, Set<IPath>>> outputFolders;
	private Map<Long, String> patchData;
	private long nextId;

	public void setState(State s) {
		state = s;
	}

	public void setExtraData(HashMap<Long, String[]> classpath, Map<Long, String> patch, Map<String, Map<String, Set<IPath>>> outputFolders) {
		this.classpath = classpath;
		this.patchData = patch;
		this.outputFolders = outputFolders;
	}

	public State getState() {
		return state;
	}

	public HashMap<Long, String[]> getClasspaths() {
		return classpath;
	}

	public Map<String, Map<String, Set<IPath>>> getOutputFolders() {
		return outputFolders;
	}

	public Map<Long, String> getPatchData() {
		return patchData;
	}

	public void setNextId(long nextId) {
		this.nextId = nextId;
	}

	public long getNextId() {
		return nextId;
	}
}
