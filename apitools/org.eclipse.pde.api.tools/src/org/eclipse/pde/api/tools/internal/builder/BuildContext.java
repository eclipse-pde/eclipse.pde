/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private StringSet structualChanges = null;
	private StringSet removedTypes = null;
	private StringSet descriptionChanges = null;
	private StringSet descriptionDepedents = null;

	/**
	 * Constructor
	 */
	public BuildContext() {
	}

	@Override
	public String[] getStructurallyChangedTypes() {
		if (this.structualChanges == null) {
			return NO_TYPES;
		}
		return this.structualChanges.values;
	}

	@Override
	public String[] getDescriptionChangedTypes() {
		if (this.descriptionChanges == null) {
			return NO_TYPES;
		}
		return this.descriptionChanges.values;
	}

	@Override
	public String[] getRemovedTypes() {
		if (this.removedTypes == null) {
			return NO_TYPES;
		}
		return this.removedTypes.values;
	}

	@Override
	public boolean hasStructuralChanges() {
		int count = (this.structualChanges == null ? 0 : this.structualChanges.elementSize);
		count += (this.removedTypes == null ? 0 : this.removedTypes.elementSize);
		return count > 0;
	}

	@Override
	public boolean hasDescriptionChanges() {
		return (this.descriptionChanges == null ? 0 : this.descriptionChanges.elementSize) > 0;
	}

	@Override
	public boolean hasDescriptionDependents() {
		return (this.descriptionDepedents == null ? 0 : this.descriptionDepedents.elementSize) > 0;
	}

	@Override
	public boolean hasRemovedTypes() {
		return (this.removedTypes == null ? 0 : this.removedTypes.elementSize) > 0;
	}

	/**
	 * Adds the given type name to the collection of structurally changed types.
	 * Does nothing if <code>null</code> is passed in as the type name.
	 *
	 * @param typename
	 */
	public void recordStructuralChange(String typename) {
		if (typename == null) {
			return;
		}
		if (this.structualChanges == null) {
			this.structualChanges = new StringSet(16);
		}
		this.structualChanges.add(typename.replace('/', '.'));
	}

	/**
	 * Adds the given type name to the collection of removed types. Does nothing
	 * if <code>null</code> is passed in as the type name.
	 *
	 * @param typename
	 */
	public void recordRemovedType(String typename) {
		if (typename == null) {
			return;
		}
		if (this.removedTypes == null) {
			this.removedTypes = new StringSet(16);
		}
		this.removedTypes.add(typename.replace('/', '.'));
	}

	/**
	 * Adds the given type name to the collection of types that have had an API
	 * description change. Does nothing if <code>null</code> is passed in.
	 *
	 * @param typename the type that has an API description change or
	 *            <code>null</code>
	 */
	public void recordDescriptionChanged(String typename) {
		if (typename != null) {
			if (this.descriptionChanges == null) {
				this.descriptionChanges = new StringSet(16);
			}
			this.descriptionChanges.add(typename.replace('/', '.'));
		}
	}

	/**
	 * Adds the given type name to the collection of dependent type names. Does
	 * nothing if <code>null</code> is passed in as the type name.
	 *
	 * @param typename the type to add a dependent of
	 */
	public void recordDescriptionDependent(String typename) {
		if (typename == null) {
			return;
		}
		if (this.descriptionDepedents == null) {
			this.descriptionDepedents = new StringSet(16);
		}
		this.descriptionDepedents.add(typename.replace('/', '.'));
	}

	@Override
	public void dispose() {
		if (this.structualChanges != null) {
			this.structualChanges.clear();
			this.structualChanges = null;
		}
		if (this.removedTypes != null) {
			this.removedTypes.clear();
			this.removedTypes = null;
		}
		if (this.descriptionChanges != null) {
			this.descriptionChanges.clear();
			this.descriptionChanges = null;
		}
		if (this.descriptionDepedents != null) {
			this.descriptionDepedents.clear();
			this.descriptionDepedents = null;
		}
	}

	@Override
	public boolean hasTypes() {
		return hasStructuralChanges() || hasRemovedTypes() || hasDescriptionChanges() || hasDescriptionDependents();
	}

	@Override
	public boolean containsStructuralChange(String typename) {
		if (typename == null) {
			return false;
		}
		return structualChanges != null && structualChanges.includes(typename.replace('/', '.'));
	}

	@Override
	public boolean containsDescriptionChange(String typename) {
		if (typename == null) {
			return false;
		}
		return descriptionChanges != null && descriptionChanges.includes(typename.replace('/', '.'));
	}

	@Override
	public boolean containsDescriptionDependent(String typename) {
		if (typename == null) {
			return false;
		}
		return descriptionDepedents != null && descriptionDepedents.includes(typename.replace('/', '.'));
	}

	@Override
	public boolean containsRemovedType(String typename) {
		if (typename == null) {
			return false;
		}
		return removedTypes != null && removedTypes.includes(typename.replace('/', '.'));
	}

	@Override
	public String[] getDescriptionDependentTypes() {
		if (this.descriptionDepedents == null) {
			return NO_TYPES;
		}
		return this.descriptionDepedents.values;
	}
}
