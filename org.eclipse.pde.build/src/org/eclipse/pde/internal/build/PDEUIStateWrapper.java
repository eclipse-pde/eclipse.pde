/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.State;

public class PDEUIStateWrapper {
	private State state;
	private HashMap extraData;
	
	public void setState(State s) {
		state = s;
	}
	
	public void setExtraData(HashMap data) {
		extraData = data;
	}
	
	public State getState() {
		return state;
	}
	
	public HashMap getExtraData() {
		return extraData;
	}
}
