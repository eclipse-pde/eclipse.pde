/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.pde.spy.event.internal.model;

public class CapturedEventFilter {
	private final static String FILTER_AS_STRING_PATTERN = Messages.CapturedEventFilter_When;

	private ItemToFilter itemToFilter;

	private Operator operator;

	private String value;

	public CapturedEventFilter(ItemToFilter itemToFilter, Operator operator, String value) {
		this.itemToFilter = itemToFilter;
		this.operator = operator;
		this.value = normalize(itemToFilter, value);
	}

	public ItemToFilter getItemToFilter() {
		return itemToFilter;
	}

	public void setItemToFilter(ItemToFilter itemToFilter) {
		this.itemToFilter = itemToFilter;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	private String normalize(ItemToFilter itemToFilter, String value) {
		if (ItemToFilter.ParameterNameAndValue.equals(itemToFilter)) {
			String[] splitted = value.split("="); //$NON-NLS-1$
			if (splitted.length != 2) {
				throw new IllegalArgumentException(Messages.CapturedEventFilter_InvalidValueFormat
						+ String.format(SpecialValue.NameAndValue.toString(), Messages.CapturedEventFilter_Name, Messages.CapturedEventFilter_Value));
			}
			return String.format(SpecialValue.NameAndValue.toString(), splitted[0].trim(), splitted[1].trim());
		}
		return value;
	}

	@Override
	public String toString() {
		return String.format(FILTER_AS_STRING_PATTERN, itemToFilter, operator, value);
	}

	@Override
	public int hashCode() {
		return (value + itemToFilter + operator).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CapturedEventFilter && obj.hashCode() == hashCode();
	}
}
