/*******************************************************************************
 * Copyright (c) Apr 2, 2014 IBM Corporation and others.
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
package a.b.c;

import org.eclipse.pde.api.tools.annotations.NoOverride;
import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 *
 */
public interface test1 {
	@NoOverride
	@NoReference
	default int m1() {
		return 1;
	}
	
	interface inner {
		@NoOverride
		@NoReference
		default int m1() {
			return 1;
		}
	}
}
