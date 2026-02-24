/*******************************************************************************
 * Copyright (c)  Lacherp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lacherp - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.adapter.viewer;

/**
 * This class is used to store data filter in the context
 * @author pascal
 *
 */
public class FilterData {

	String txtSearchFilter;
	boolean showPackage;
	boolean sourceToDestination;
	/**
	 * Ctor
	 */
	public FilterData() {
		showPackage = Boolean.TRUE;
		sourceToDestination = Boolean.TRUE;
	}

	/**
	 * Copy ctor
	 * @param fdata
	 */
	public FilterData(FilterData fdata)
	{
		this.txtSearchFilter = fdata.txtSearchFilter;
		this.showPackage = fdata.showPackage;
		this.sourceToDestination = fdata.sourceToDestination;
	}
	/**
	 * @return the txtSearchFilter
	 */
	public String getTxtSearchFilter() {
		return txtSearchFilter;
	}

	/**
	 * @param txtSearchFilter the txtSearchFilter to set
	 */
	public void setTxtSearchFilter(String txtSearchFilter) {
		this.txtSearchFilter = txtSearchFilter;

	}

	/**
	 * @return the showPackage
	 */
	public Boolean getShowPackage() {
		return showPackage;
	}

	/**
	 * @param showPackage the showPackage to set
	 */
	public void setShowPackage(Boolean showPackage) {
		this.showPackage = showPackage;
	}

	/**
	 * @return the sourceToDestination
	 */
	public Boolean getSourceToDestination() {
		return sourceToDestination;
	}

	/**
	 * @param sourceToDestination the sourceToDestination to set
	 */
	public void setSourceToDestination(Boolean sourceToDestination) {
		this.sourceToDestination = sourceToDestination;
	}

	
}
