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
package org.eclipse.pde.api.tools.internal.search;


public class SkippedComponent {
	/**
	 * if the skipped component has no .api_description
	 */
	private boolean noapidescription;
	/**
	 * if the skipped component was skipped because it was found in an exclude list
	 */
	private boolean inexcludelist;
	/**
	 * the id of of the skipped component
	 */
	private String componentid;

	/**
	 * Constructor
	 * @param noapidescription
	 * @param inexcludelist
	 * @param componentid
	 */
	public SkippedComponent(String componentid, boolean noapidescription, boolean inexcludelist) {
		this.noapidescription = noapidescription;
		this.inexcludelist = inexcludelist;
		this.componentid = componentid;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof SkippedComponent) {
			return this.componentid.equals(((SkippedComponent)obj).componentid);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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
	 * @return true if the the skipped component has no .api_description file
	 */
	public boolean hasNoApiDescription() {
		return this.noapidescription;
	}
	
	/**
	 * @return true if the component was skipped because it appeared in an exclude list
	 */
	public boolean wasExcluded() {
		return this.inexcludelist;
	}
}