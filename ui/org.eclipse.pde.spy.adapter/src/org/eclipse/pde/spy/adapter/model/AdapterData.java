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
package org.eclipse.pde.spy.adapter.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.pde.spy.adapter.Messages;
import org.eclipse.pde.spy.adapter.tools.AdapterHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
 * Adapter data model Object This class is used to store ConfigarationElement
 * elements which use an adapter
 * 
 * @author pascal
 *
 */
public class AdapterData implements Comparable<AdapterData> {

	String sourceType;
	String destinationType;
	String adapterClassName;
	boolean isInterface = false;
	boolean checkInterfaceClass = false;
	AdapterData parent;
	List<AdapterData> children = new ArrayList<>();
	AdapterElementType elemType;
	boolean visibilityFilter = true;
	Boolean showPackage;

	int selectedColumn;

	/**
	 * @return the selectedColumn
	 */
	public int getSelectedColumn() {
		return selectedColumn;
	}

	/**
	 * @param selectedColumn the selectedColumn to set
	 */
	public void setSelectedColumn(int selectedColumn) {
		this.selectedColumn = selectedColumn;
	}

	/**
	 * Ctor
	 * 
	 * @param elemType
	 */
	public AdapterData(AdapterElementType elemType) {
		this.elemType = elemType;
		showPackage = Boolean.TRUE;
	}

	public AdapterData(AdapterData adapterData) {
		this.elemType = adapterData.getAdapterElementType();
		this.showPackage = Boolean.TRUE;
		this.destinationType = adapterData.getDestinationType();
		this.adapterClassName = adapterData.getAdapterClassName();
		this.sourceType = adapterData.getSourceType();
	}

	/**
	 * propagate visibility to children
	 */
	public void propagateVisibility() {
		children.forEach(d -> {
			d.setVisibilityFilter(isVisibilityFilter());
			d.propagateVisibility();
		});
	}

	public void textSearch(String txtSearch, AtomicBoolean bfound) {

		if (bfound.get()) {
			return;
		}
		String txt = this.toString();
		bfound.set(txt.contains(txtSearch));
		// check in adapter class
		children.forEach(d -> {
			d.textSearch(txtSearch, bfound);
		});

	}

	/**
	 * @return the sourceType
	 */
	public String getSourceType() {
		return checkNull(sourceType);
	}

	/**
	 * @param sourceType the sourceType to set
	 */
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * @return the destinationType
	 */
	public String getDestinationType() {
		return checkNull(destinationType);
	}

	/**
	 * @param destinationType the destinationType to set
	 */
	public void setDestinationType(String destinationType) {
		this.destinationType = destinationType;
	}

	/**
	 * @return the adapterClassName
	 */
	public String getAdapterClassName() {
		return checkNull(adapterClassName);
	}

	/**
	 * @param adapterClassName the adapterClassName to set
	 */
	public void setAdapterClassName(String adapterClassName) {
		this.adapterClassName = adapterClassName;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	public List<AdapterData> getChildrenList() {
		return this.children;
	}

	public Object getChildren(boolean sourceToDestination) {
		if (!children.isEmpty()) {
			Collections.sort(children);
			if( sourceToDestination) {
				Map<String, List<AdapterData>> childs = children.stream().collect(Collectors.groupingBy(AdapterData::getDestinationType));
				children.clear();
				childs.values().forEach(ls-> children.add(ls.get(0)));
				return  children.toArray();
			}else {
				Map<String, List<AdapterData>> childs = children.stream().collect(Collectors.groupingBy(AdapterData::getSourceType));
				children.clear();
				childs.values().forEach(ls-> children.add(ls.get(0)));
				return  children.toArray();
			}
		}
		return new AdapterData[0];
	}

	public Object getParent() {
		return this.parent;
	}

	public AdapterData getAdapterDataParent() {
		return (AdapterData) this.parent;
	}

	public void setParent(AdapterData parent) {
		this.parent = parent;
	}

	public AdapterElementType getAdapterElementType() {
		return this.elemType;
	}

	/**
	 * @return the isInterface
	 */
	public boolean isInterface() {
		return isInterface;
	}

	/**
	 * @param isInterface the isInterface to set
	 */
	public void setInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	/**
	 * @return the showPackage
	 */
	public boolean isShowPackage() {
		return showPackage;
	}

	/**
	 * @param showPackage the showPackage to set
	 */
	public void setShowPackage(boolean showPackage) {
		this.showPackage = showPackage;
	}

	public String getText(int columnIndex) {
		if (columnIndex == 0) {
			return elemType.equals(AdapterElementType.SOURCE_TYPE) ? displayPackage(getSourceType())
					: displayPackage(getDestinationType());
		}
		if (columnIndex == 1 && getParent() != null) {

			return elemType.equals(AdapterElementType.DESTINATION_TYPE) ? displayPackage(getAdapterClassName())
					: displayPackage(((AdapterData) getParent()).getAdapterClassName());
		}
		return "";
	}

	public String getImageName() {
		String className = elemType.equals(AdapterElementType.SOURCE_TYPE) ? getSourceType():getDestinationType();
		if (elemType.equals(AdapterElementType.DESTINATION_TYPE)) {
			return AdapterHelper.DESTINATION_TYPE_IMG_KEY;
		}
		if(!checkInterfaceClass) {
			Bundle bundle = AdapterHelper.getBundleForClassName(className);
			if (bundle != null)
				setInterface(AdapterHelper.isInterfaceTypeClass(bundle, className));
			else {
				// may be it's an interface
				if (subStringPackage(className).startsWith("I")) {
					return AdapterHelper.INTERFACE_IMG_KEY;
				}
				return AdapterHelper.SOURCE_TYPE_IMG_KEY;
			}
			checkInterfaceClass = true;
		}
		if (elemType.equals(AdapterElementType.SOURCE_TYPE)) {
			if( isInterface()) {
				return AdapterHelper.INTERFACE_IMG_KEY;
			}
			return AdapterHelper.SOURCE_TYPE_IMG_KEY;
		}
		
		return null;
		
	}

	/**
	 * @return the visibilityFilter
	 */
	public boolean isVisibilityFilter() {
		return visibilityFilter;
	}

	/**
	 * @param visibilityFilter the visibilityFilter to set
	 */
	public void setVisibilityFilter(boolean visibilityFilter) {
		this.visibilityFilter = visibilityFilter;
	}

	public Stream<AdapterData> convertSourceToType() {
		final ArrayList<AdapterData> result = new ArrayList<>();
		this.getChildrenList().forEach(child -> {
			AdapterData newAdapterData = new AdapterData(child);
			AdapterData soon = new AdapterData(this);
			soon.setParent(child);
			newAdapterData.getChildrenList().add(soon);

			result.add(newAdapterData);
		});
		return result.stream();
	}

	@Override
	public String toString() {
		return getSourceType() + "@" + getDestinationType() + getAdapterClassName();
	}


	@Override
	public int compareTo(AdapterData o) {
		return this.getText(0).compareTo(o.getText(0));
	}

	public String getToolTipText(boolean sourceToDestination, int columnIndex) {

		if (columnIndex == 1) {
			return getAdapterClassName().isEmpty() ? "" : getAdapterFactorySourceTooltip();
		}
		// column 0
		if (sourceToDestination && getParent() == null) {
			return getRootSourceTypeTooltip();
		}
		if (!sourceToDestination && getParent() == null) {
			return getRootDesinationTypeTooltip();
		}
		if (sourceToDestination && getParent() != null) {
			return getChildSourceTypeToolTip();
		}
		return getChildDestinationTypeToolTip();

	}
	
	public StyleRange[] getToolTipStyleRanges(Boolean sourceToDestination, int columnIndex) {
		if (columnIndex == 1) {
			return getAdapterClassName().isEmpty() ? null : getAdapterFactorySourceTooltipStyleRanges();
		}
		// column 0
		if (sourceToDestination && getParent() == null) {
			return getRootSourceTypeTooltipStyleRanges();
		}
		if (!sourceToDestination && getParent() == null) {
			return getRootDesinationTypeTooltipStyleRanges();
		}
		if (sourceToDestination && getParent() != null) {
			return getChildSourceTypeToolTipStyleRanges();
		}
		return getChildDestinationTypeToolTipStyleRanges();
		
	}

	private String getRootSourceTypeTooltip() {
		return NLS.bind(Messages.rootSourceTypeTooltip, subStringPackage(getSourceType()));
	}

	private String getRootDesinationTypeTooltip() {
		return NLS.bind(Messages.rootDestinationTypeToolTip, subStringPackage(getDestinationType()));
	}

	private String getAdapterFactorySourceTooltip() {
		List<String> bindings = Arrays.asList(subStringPackage(getAdapterClassName()),
				subStringPackage(((AdapterData) getParent()).getSourceType()), concatChildren(true));
		return NLS.bind(Messages.adapterFactory, bindings.toArray());
	}

	private String getChildDestinationTypeToolTip() {
		List<String> bindings = Arrays.asList(subStringPackage(getSourceType()),
				subStringPackage(((AdapterData) getParent()).getDestinationType()),
				subStringPackage(((AdapterData) getParent()).getAdapterClassName()));
		return NLS.bind(Messages.childDestinationTypeToolTip, bindings.toArray());
	}

	private String getChildSourceTypeToolTip() {
		List<String> bindings = Arrays.asList(subStringPackage(((AdapterData) getParent()).getSourceType()),
				subStringPackage(getDestinationType()), subStringPackage(getAdapterClassName()));
		return NLS.bind(Messages.childSourceTypeToolTip, bindings.toArray());
	}
	
	private StyleRange[] getAdapterFactorySourceTooltipStyleRanges() {
		int length0 = subStringPackage(getAdapterClassName()).length();
		int length1 =subStringPackage(((AdapterData) getParent()).getSourceType()).length();
		int length2 = 	concatChildren(true).length();	
	
		StyleRange [] styleRanges = new  StyleRange[4];
		styleRanges[0] = getBoldStyle(0, length0);
		styleRanges[1] = getStandard(length0+1, 30);
		styleRanges[2] = getBoldStyle(length0+31, length1);
		styleRanges[3] = getBoldStyle(length0+35+length1, length2);
	
		return styleRanges;
	}

	private StyleRange[] getRootSourceTypeTooltipStyleRanges() {
		StyleRange [] styleRanges = new  StyleRange[2];
		styleRanges[0] = getStandard(0, 43);
		styleRanges[1] = getBoldStyle(44, subStringPackage(getSourceType()).length());
		return styleRanges;
	}

	private StyleRange[] getRootDesinationTypeTooltipStyleRanges() {
		StyleRange [] styleRanges = new  StyleRange[2];
		styleRanges[0] = getStandard(0, 57);
		styleRanges[1] = getBoldStyle(58, subStringPackage(getDestinationType()).length());
		return styleRanges;
	}

	private StyleRange[] getChildSourceTypeToolTipStyleRanges() {
		
		int length0 = subStringPackage(((AdapterData) getParent()).getSourceType()).length();
		int length1 = subStringPackage(getDestinationType()).length();
		int length2 = subStringPackage(getAdapterClassName()).length();
		StyleRange [] styleRanges = new  StyleRange[4];
		styleRanges[0] = getBoldStyle(0, length0);
		styleRanges[1] = getStandard(length0+1, 18);
		styleRanges[2] = getBoldStyle(length0+19, length1);
		styleRanges[3] = getBoldStyle(length0+28+length1, length2);
		return styleRanges;
	}

	private StyleRange[] getChildDestinationTypeToolTipStyleRanges() {
		
		int length0 = subStringPackage(getSourceType()).length();
		int length1 = subStringPackage(((AdapterData) getParent()).getDestinationType()).length();
		int length2 = subStringPackage(((AdapterData) getParent()).getAdapterClassName()).length();
		StyleRange [] styleRanges = new  StyleRange[4];
		styleRanges[0] = getBoldStyle(0, length0);
		styleRanges[1] = getStandard(length0+1, 20);
		styleRanges[2] = getBoldStyle(length0+22, length1);
		styleRanges[3] = getBoldStyle(length0+28+length1, length2);
		return styleRanges;
	}

	private StyleRange getStandard(int start,int length) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = length;
		styleRange.fontStyle = SWT.NORMAL;
		styleRange.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		return styleRange;
	}
	
	private StyleRange getBoldStyle(int start,int length) {
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = length;
		styleRange.fontStyle = SWT.BOLD;
		styleRange.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		return styleRange;
	}
	
	private String checkNull(String val) {
		return (val == null) ? "" : val;
	}

	private String displayPackage(String value) {
		if (Boolean.TRUE.equals(showPackage)) {
			return value;
		}
		return subStringPackage(value);
	}
	
	

	private String subStringPackage(String value) {
		return value.substring(value.lastIndexOf(".") + 1, value.length());
	}

	private String concatChildren(boolean sourceToDestination) {
		if (sourceToDestination)
			return ((AdapterData) getParent()).children.stream().map((a) -> a.subStringPackage(a.getDestinationType()))
					.collect(Collectors.joining(", "));
		else
			return "";
	}

}
