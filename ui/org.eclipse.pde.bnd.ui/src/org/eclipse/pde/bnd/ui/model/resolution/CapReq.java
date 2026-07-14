/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import java.util.Collection;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public record CapReq(Map<String, Collection<Capability>> capabilities,
		Map<String, Collection<Requirement>> requirements) {

}
