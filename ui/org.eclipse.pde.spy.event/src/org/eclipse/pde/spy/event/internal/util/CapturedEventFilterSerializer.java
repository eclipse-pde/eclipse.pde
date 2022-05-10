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
package org.eclipse.pde.spy.event.internal.util;

import org.eclipse.pde.spy.event.internal.model.CapturedEventFilter;
import org.eclipse.pde.spy.event.internal.model.ItemToFilter;
import org.eclipse.pde.spy.event.internal.model.Operator;

public class CapturedEventFilterSerializer {
	private final static String FILTER_PARAM_SEPARATOR = ","; //$NON-NLS-1$

	public static String serialize(CapturedEventFilter filter) {
		return new StringBuilder(filter.getItemToFilter().toString()).append(FILTER_PARAM_SEPARATOR)
				.append(filter.getOperator().toString()).append(FILTER_PARAM_SEPARATOR).append(filter.getValue())
				.toString();
	}

	public static CapturedEventFilter deserialize(String filterAsText) {
		String[] filterParams = filterAsText.split(FILTER_PARAM_SEPARATOR);
		if (filterParams.length != 3) {
			return null;
		}
		return new CapturedEventFilter(ItemToFilter.toItem(filterParams[0]), Operator.toOperator(filterParams[1]),
				filterParams[2]);
	}
}
