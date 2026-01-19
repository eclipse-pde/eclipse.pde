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

import java.util.Collection;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.spy.adapter.model.AdapterData;
import org.eclipse.pde.spy.adapter.tools.AdapterHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This provider is used to display available plugins which contribute to
 * adapters.exsd extension point
 * 
 * @author pascal
 *
 */
public class AdapterContentProvider extends ColumnLabelProviderCustom implements ITreeContentProvider {

	@Inject
	private ImageRegistry imgReg;

	private int columnIndex;

	private Boolean sourceToDestination = true;

	@Override
	public Object[] getElements(Object inputElement) {

		if (inputElement instanceof Collection<?>) {
			return ((Collection<?>) inputElement).toArray();
		}
		return (Object[]) inputElement;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof AdapterData) {
			return (Object[]) ((AdapterData) parentElement).getChildren(sourceToDestination);
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof AdapterData) {
			return ((AdapterData) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {

		if (element instanceof AdapterData) {
			return ((AdapterData) element).hasChildren();
		}
		return false;
	}

	@Override
	public String getText(Object element) {

		if (element instanceof AdapterData) {
			return ((AdapterData) element).getText(columnIndex);
		}
		return "";
	}

	@Override
	public Image getImage(Object element) {
		if (columnIndex == 0) {
			if (element instanceof AdapterData) {
				String imgname = ((AdapterData) element).getImageName();
				return imgname == null ? super.getImage(element) : imgReg.get(imgname);
			}

		}
		return super.getImage(element);
	}

	@Override
	public int getToolTipStyle(Object object) {
		return SWT.SHADOW_OUT;
	}

	@Override
	public Color getToolTipBackgroundColor(Object object) {
		return AdapterHelper.getColor(Display.getDefault(),"TOOLTIP_BACKGROUND");

	}

	@Override
	protected StyleRange[] getToolTipStyleRanges(Object element) {
		if (element instanceof AdapterData) {
			return ((AdapterData) element).getToolTipStyleRanges(sourceToDestination, columnIndex);
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof AdapterData && ((AdapterData) element).getToolTipText(sourceToDestination, columnIndex) != null) {
			return ((AdapterData) element).getToolTipText(sourceToDestination, columnIndex).replaceAll("\"","");
		}
		return "";
	}

	@Inject
	@Optional
	public void updateTextSearchFilter(@Named(AdapterFilter.UPDATE_CTX_FILTER) FilterData filterData) {
		if (filterData == null) {
			return;
		}
		this.sourceToDestination = filterData.getSourceToDestination();
	}

	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;

	}
}
