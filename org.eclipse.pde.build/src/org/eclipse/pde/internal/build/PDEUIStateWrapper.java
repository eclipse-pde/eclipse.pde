/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.service.resolver.State;

public class PDEUIStateWrapper {
	private State state;
	private HashMap classpath;
	private Map patchData;
	private long nextId;
	
	public void setState(State s) {
		state = s;
	}
	
	public void setExtraData(HashMap classpath, Map patch) {
		this.classpath = classpath;
		this.patchData = patch;
	}
	
	public State getState() {
		return state;
	}
	
	public HashMap getClasspaths() {
		return classpath;
	}

	public Map getPatchData() {
		return patchData;
	}
	
	public void setNextId(long nextId) {
		this.nextId = nextId;
	}
	
	public long getNextId() {
		return nextId;
	}
}
