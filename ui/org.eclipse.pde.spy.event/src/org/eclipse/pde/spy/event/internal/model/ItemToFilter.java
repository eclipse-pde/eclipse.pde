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

public enum ItemToFilter {
	NotSelected(Messages.ItemToFilter_Title), Topic(Messages.ItemToFilter_Topic), ParameterName(Messages.ItemToFilter_Name), ParameterNameAndValue(
			Messages.ItemToFilter_NameAndValue), ParameterValue(Messages.ItemToFilter_SomeParameterValue), Publisher(
					Messages.ItemToFilter_EventPublisher), ChangedElement(Messages.ItemToFilter_ChangedElement);

	private final String text;

	private ItemToFilter(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public static ItemToFilter toItem(String text) {
		for (ItemToFilter item : values()) {
			if (item.text.equals(text)) {
				return item;
			}
		}
		throw new IllegalArgumentException(
				String.format(Messages.ItemToFilter_NotFound, ItemToFilter.class.getSimpleName(), text));
	}
}