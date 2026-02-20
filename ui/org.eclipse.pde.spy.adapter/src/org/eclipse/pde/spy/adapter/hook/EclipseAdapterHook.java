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
package org.eclipse.pde.spy.adapter.hook;

import org.eclipse.e4.core.internal.services.EclipseAdapter;

@SuppressWarnings("restriction")
public class EclipseAdapterHook extends EclipseAdapter {

	
	@Override
	public <T> T adapt(Object element, Class<T> adapterType) {
		return super.adapt(element, adapterType);
	}
	
	
}
