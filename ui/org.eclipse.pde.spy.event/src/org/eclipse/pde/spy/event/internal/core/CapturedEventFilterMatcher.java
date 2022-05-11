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
package org.eclipse.pde.spy.event.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.spy.event.internal.model.CapturedEvent;
import org.eclipse.pde.spy.event.internal.model.CapturedEventFilter;
import org.eclipse.pde.spy.event.internal.model.ItemToFilter;
import org.eclipse.pde.spy.event.internal.model.Operator;
import org.eclipse.pde.spy.event.internal.model.Parameter;
import org.eclipse.pde.spy.event.internal.model.SpecialValue;

public class CapturedEventFilterMatcher {
	public boolean matches(CapturedEvent event, CapturedEventFilter filter) {
		Object value = getItemToFilterValue(event, filter.getItemToFilter());

		if (value instanceof List) {
			List<?> list = (List<?>) value;
			for (Object item : list) {
				if (matches(filter.getValue(), item, filter.getOperator())) {
					return true;
				}
			}
			return false;
		}
		return matches(filter.getValue(), value, filter.getOperator());
	}

	private Object getItemToFilterValue(CapturedEvent event, ItemToFilter itemToFilter) {
		if (itemToFilter.equals(ItemToFilter.Topic)) {
			return event.getTopic();
		}
		if (itemToFilter.equals(ItemToFilter.Publisher)) {
			return event.getPublisherClassName();
		}
		if (itemToFilter.equals(ItemToFilter.ChangedElement)) {
			return event.getChangedElementClassName();
		}
		if (itemToFilter.equals(ItemToFilter.ParameterName)) {
			List<String> names = new ArrayList<String>();
			for (Parameter parameter : event.getParameters()) {
				names.add(parameter.getName());
			}
			return names;
		}
		if (itemToFilter.equals(ItemToFilter.ParameterValue)) {
			List<Object> values = new ArrayList<Object>();
			for (Parameter parameter : event.getParameters()) {
				values.add(parameter.getValue());
			}
			return values;
		}
		if (itemToFilter.equals(ItemToFilter.ParameterNameAndValue)) {
			List<String> nameAndValues = new ArrayList<String>();
			for (Parameter parameter : event.getParameters()) {
				String nameAndValue = String.format(SpecialValue.NameAndValue.toString(), parameter.getName().trim(),
						(parameter.getValue() == null ? SpecialValue.Null : parameter.getValue().toString().trim()));
				nameAndValues.add(nameAndValue);
			}
			return nameAndValues;
		}

		throw new IllegalArgumentException(Messages.CapturedEventFilterMatcher_NoSupportedItem + itemToFilter.toString());
	}

	private boolean matches(String expected, Object current, Operator operator) {
		if (SpecialValue.Null.toString().equalsIgnoreCase(expected)) {
			return operator.isPositive() ? matchesToNull(current) : !matchesToNull(current);
		}
		if (SpecialValue.EmptyString.toString().equalsIgnoreCase(expected)) {
			return operator.isPositive() ? matchesToEmptyString(current) : !matchesToEmptyString(current);
		}
		if (Operator.Equals.equals(operator)) {
			return equalsTo(expected, current);
		}
		if (Operator.NotEquals.equals(operator)) {
			return !equalsTo(expected, current);
		}
		if (Operator.Contains.equals(operator)) {
			return contains(expected, current);
		}
		if (Operator.NotContains.equals(operator)) {
			return !contains(expected, current);
		}
		if (Operator.StartsWith.equals(operator)) {
			return startsWith(expected, current);
		}
		if (Operator.NotStartsWith.equals(operator)) {
			return !startsWith(expected, current);
		}

		throw new IllegalArgumentException(Messages.CapturedEventFilterMatcher_NoSupportedOperator + operator);
	}

	private boolean matchesToEmptyString(Object current) {
		return current != null && current instanceof String && current.toString().length() == 0;
	}

	private boolean matchesToNull(Object current) {
		return current == null || current.toString().equalsIgnoreCase(SpecialValue.Null.toString());
	}

	private boolean equalsTo(String expected, Object current) {
		return current != null && expected.equalsIgnoreCase(current.toString());
	}

	private boolean contains(String expected, Object current) {
		return current != null && current.toString().toLowerCase().contains(expected.toLowerCase());
	}

	private boolean startsWith(String expected, Object current) {
		return current != null && current.toString().toLowerCase().startsWith(expected.toLowerCase());
	}
}
