/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.util.*;
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
