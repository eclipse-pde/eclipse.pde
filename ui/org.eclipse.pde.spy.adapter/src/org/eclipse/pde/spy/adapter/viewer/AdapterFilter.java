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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.spy.adapter.model.AdapterData;
import org.eclipse.pde.spy.adapter.model.AdapterElementType;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Adapter Tree viewer filter
 * @author pascal
 *
 */
public class AdapterFilter extends ViewerFilter {

	public static final String UPDATE_CTX_FILTER ="updateCtxfilter";
	
	private String  txtSeachFilter;
	private Boolean showPackageFilter = Boolean.TRUE;
	private Boolean sourceToDestination = Boolean.TRUE;
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		
		if(element instanceof AdapterData) {
			
			((AdapterData)element).setShowPackage(showPackageFilter);
		}
		
		if (txtSeachFilter != null && !txtSeachFilter.isEmpty()) {
			doFilter((AdapterData) element);
		}
		if(txtSeachFilter != null && txtSeachFilter.isEmpty())
		{
			((AdapterData)element).setVisibilityFilter(true);
			((AdapterData)element).propagateVisibility();
		}
		return ((AdapterData)element).isVisibilityFilter();
	}
	
	@Inject
	@Optional
	public void updateTextSearchFilter(@Named(UPDATE_CTX_FILTER) FilterData filterData) {
		if( filterData == null )
		{
			return;
		}
		this.txtSeachFilter = filterData.getTxtSearchFilter();
		this.showPackageFilter= filterData.getShowPackage();
		this.sourceToDestination = filterData.getSourceToDestination();
	}

	
	private void doFilter(AdapterData adapterData)
	{
		if( Boolean.TRUE.equals(sourceToDestination) && adapterData.getAdapterElementType().equals(AdapterElementType.SOURCE_TYPE))
		{
			doVisibility(adapterData);
		}
		if( Boolean.FALSE.equals(sourceToDestination) && adapterData.getAdapterElementType().equals(AdapterElementType.DESTINATION_TYPE))
		{
			doVisibility(adapterData);
		}
	}
	
	private void doVisibility(AdapterData adapterData) {
		AtomicBoolean bfound = new AtomicBoolean(false);
		adapterData.textSearch(txtSeachFilter, bfound);
		adapterData.setVisibilityFilter(bfound.get());
		adapterData.propagateVisibility();
	}
}
