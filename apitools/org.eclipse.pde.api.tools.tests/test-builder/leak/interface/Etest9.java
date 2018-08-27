/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package x.y.z;

import internal.x.y.z.Iinternal;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface Etest9 extends Iinternal {
/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
interface inner extends Iinternal {
	/**
	 * @noimplement This interface is not intended to be implemented by clients.
	 */
	interface inner2 extends Iinternal {
		
	}
}
}
