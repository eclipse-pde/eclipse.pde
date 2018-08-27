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

import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 * Tests the @NoReference annotation on inner classes, enums and interfaces 
 */
public @interface test3 {

	public @interface inner1 {

		/**
		 */
		@NoReference
		public static class Clazz {
		}

		/**
		 */
		@NoReference
		public interface inter {
		}

		/**
		 */
		public int field = 0;

		/**
		 */
		@NoReference
		public @interface annot {

		}
	}

}
