/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.search;

import java.util.TreeSet;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;

public class SkippedComponent implements IApiElement {
	/**
	 * the id of of the skipped component
	 */
	private String componentid;
	/**
	 * The version of the component
	 */
	private String version;
	/**
	 * the set of resolution errors barring the component from being scanned
	 */
	private ResolverError[] errors = null;

	/**
	 * Constructor
	 *
	 * @param componentid
	 * @param version
	 * @param errors the {@link ResolverError}s, if any, that prevented this
	 *            component from being scanned
	 */
	public SkippedComponent(String componentid, String version, ResolverError[] errors) {
		this.componentid = componentid;
		this.version = version;
		this.errors = errors;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SkippedComponent) {
			return this.componentid.equals(((SkippedComponent) obj).componentid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.componentid.hashCode();
	}

	/**
	 * @return the component id of the skipped component
	 */
	public String getComponentId() {
		return this.componentid;
	}

	/**
	 * @return true if the component was skipped because it appeared in an
	 *         exclude list
	 */
	public boolean wasExcluded() {
		return this.errors == null;
	}

	/**
	 * @return true if the the component had resolution errors
	 */
	public boolean hasResolutionErrors() {
		return this.errors != null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getAncestor(int)
	 */
	@Override
	public IApiElement getAncestor(int ancestorType) {
		return null;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getApiComponent()
	 */
	@Override
	public IApiComponent getApiComponent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getName()
	 */
	@Override
	public String getName() {
		return this.componentid;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getParent()
	 */
	@Override
	public IApiElement getParent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getType()
	 */
	@Override
	public int getType() {
		return IApiElement.COMPONENT;
	}

	/**
	 * @return the errors
	 */
	public ResolverError[] getErrors() {
		return this.errors;
	}

	/**
	 * Resolves the root errors for the given set of errors
	 *
	 * @param rerrors
	 * @param collector
	 * @return the resolved leaf set of problem messages
	 */
	private String[] resolveRootErrors(ResolverError[] rerrors) {
		TreeSet<String> collector = new TreeSet<>((o1, o2) -> (o1).compareTo(o2));
		ResolverError error = null;
		VersionConstraint[] constraints = null;
		BundleDescription[] bundle = new BundleDescription[1];
		for (ResolverError rerror : rerrors) {
			error = rerror;
			if (error.getType() != ResolverError.MISSING_REQUIRE_BUNDLE) {
				collector.add(error.toString());
			}
			bundle[0] = error.getBundle();
			constraints = bundle[0].getContainingState().getStateHelper().getUnsatisfiedLeaves(bundle);
			if (constraints.length == 0) {
				collector.add(error.toString());
			}
			for (VersionConstraint constraint : constraints) {
				collector.add(constraint.toString());
			}
		}
		return collector.toArray(new String[collector.size()]);
	}

	/**
	 * @return the formatted details of why the component was skipped
	 */
	public String getErrorDetails() {
		if (this.errors != null) {
			StringBuilder buffer = new StringBuilder();
			String[] problems = resolveRootErrors(this.errors);
			for (String problem : problems) {
				buffer.append(problem).append("<br/>"); //$NON-NLS-1$
			}
			return buffer.toString();
		}
		return SearchMessages.SkippedComponent_component_was_excluded;
	}
}