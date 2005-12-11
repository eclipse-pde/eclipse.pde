/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ibundle.IBundle;

public class RequiredExecutionEnvironmentHeader extends CompositeManifestHeader {
    
    private static final long serialVersionUID = 1L;
    
    public RequiredExecutionEnvironmentHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}
    
    protected PDEManifestElement createElement(ManifestElement element) {
    	return new ExecutionEnvironment(this, element.getValue());
    }
    
    public boolean hasExecutionEnvironment(IExecutionEnvironment env) {
    	return hasElement(env.getId());
    }
    
    public void addExecutionEnvironment(IExecutionEnvironment env) {
    	addManifestElement(new ExecutionEnvironment(this, env.getId()));  	
    }
    
    public ExecutionEnvironment removeExecutionEnvironment(ExecutionEnvironment env) {
    	return (ExecutionEnvironment)removeManifestElement(env);
    }
     
    public ExecutionEnvironment[] getEnvironments() {
    	PDEManifestElement[] elements = getElements();
    	ExecutionEnvironment[] result = new ExecutionEnvironment[elements.length];
    	System.arraycopy(elements, 0, result, 0, elements.length);
        return result;
   }
    
}
