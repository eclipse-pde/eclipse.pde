/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

/**
 * Tests that tags are scanned correctly off an anonymous inner type field
 * @since
 */
public class TestField6 {

	public Runnable runner = new Runnable() {
		/**
		 * @noreference
		 */
		protected int number = -1;
		public void run() {
		}
	};
}
