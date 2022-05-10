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
package org.eclipse.pde.spy.event.internal.ui;

import java.util.Collection;

import org.eclipse.pde.spy.event.internal.model.CapturedEventFilter;

public class SpyPartMemento {
	private String baseTopic;

	private Collection<CapturedEventFilter> filters;

	public void setBaseTopic(String baseTopic) {
		this.baseTopic = baseTopic;
	}

	public String getBaseTopic() {
		return baseTopic;
	}

	public void setFilters(Collection<CapturedEventFilter> filters) {
		this.filters = filters;
	}

	public Collection<CapturedEventFilter> getFilters() {
		return filters;
	}
}
