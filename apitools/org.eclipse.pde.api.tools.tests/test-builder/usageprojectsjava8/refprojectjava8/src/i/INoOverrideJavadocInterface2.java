/*******************************************************************************
 * Copyright (c) May 16, 2014 IBM Corporation and others.
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
package i;

import org.eclipse.pde.api.tools.annotations.NoOverride;

public interface INoOverrideJavadocInterface2 {
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	default public void m1() {
		
	}
}

