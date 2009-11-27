/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import org.eclipse.jdt.internal.core.builder.StringSet;
import org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext;

/**
 * Default implementation of an {@link IBuildContext}
 * 
 * @since 1.0.1
 */
public class BuildContext implements IBuildContext {

	private static final String[] NO_TYPES = new String[0];
	
	private StringSet structchanged = null;
	private StringSet dependents = null;
	private StringSet removed = null;
	
	/**
	 * Constructor
	 */
	public BuildContext() {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#getStructurallyChangedTypes()
	 */
	public String[] getStructurallyChangedTypes() {
		if(this.structchanged == null) {
			return NO_TYPES;
		}
		return this.structchanged.values;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#getRemovedTypes()
	 */
	public String[] getRemovedTypes() {
		if(this.removed == null) {
			return NO_TYPES;
		}
		return this.removed.values;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#getDependentTypes()
	 */
	public String[] getDependentTypes() {
		if(this.dependents == null) {
			return NO_TYPES;
		}
		return this.dependents.values;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#hasChangedTypes()
	 */
	public boolean hasChangedTypes() {
		int count = (this.structchanged == null ? 0 : this.structchanged.elementSize);
		count += (this.removed == null ? 0 : this.removed.elementSize);
		return count > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#hasDependentTypes()
	 */
	public boolean hasDependentTypes() {
		return (this.dependents == null ? 0 : this.dependents.elementSize) > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#hasRemovedTypes()
	 */
	public boolean hasRemovedTypes() {
		return (this.removed == null ? 0 : this.removed.elementSize) > 0;
	}
	
	/**
	 * Adds the given type name to the collection of structurally changed types. Does nothing 
	 * if <code>null</code> is passed in as the type name.
	 * 
	 * @param typename
	 */
	public void recordStructurallyChangedType(String typename) {
		if(typename == null) {
			return;
		}
		if(this.structchanged == null) {
			this.structchanged = new StringSet(16);
		}
		this.structchanged.add(typename.replace('/', '.'));
	}

	/**
	 * Adds the given type name to the collection of removed types. Does nothing 
	 * if <code>null</code> is passed in as the type name.
	 * 
	 * @param typename
	 */
	public void recordRemovedType(String typename) {
		if(typename == null) {
			return;
		}
		if(this.removed == null) {
			this.removed = new StringSet(16);
		}
		this.removed.add(typename.replace('/', '.'));
	}
	
	/**
	 * Adds the given type name to the collection of dependent type names. Does
	 * nothing if <code>null</code> is passed in as the type name.
	 * 
	 * @param typename the type to add a dependent of
	 */
	public void recordDependentType(String typename) {
		if(typename == null) {
			return;
		}
		if(this.dependents == null) {
			this.dependents = new StringSet(32);
		}
		this.dependents.add(typename.replace('/', '.'));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#dispose()
	 */
	public void dispose() {
		if(this.structchanged != null) {
			this.structchanged.clear();
			this.structchanged = null;
		}
		if(this.dependents != null) {
			this.dependents.clear();
			this.dependents = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#hasTypes()
	 */
	public boolean hasTypes() {
		return hasChangedTypes() || hasDependentTypes() || hasRemovedTypes();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#containsChangedType(java.lang.String)
	 */
	public boolean containsChangedType(String typename) {
		if(typename == null) {
			return false;
		}
		return structchanged != null && structchanged.includes(typename.replace('/', '.'));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#containsDependentType(java.lang.String)
	 */
	public boolean containsDependentType(String typename) {
		if(typename == null) {
			return false;
		}
		return dependents != null && dependents.includes(typename.replace('/', '.'));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IBuildContext#containsRemovedType(java.lang.String)
	 */
	public boolean containsRemovedType(String typename) {
		if(typename == null) {
			return false;
		}
		return removed != null && removed.includes(typename.replace('/', '.'));
	}
}
