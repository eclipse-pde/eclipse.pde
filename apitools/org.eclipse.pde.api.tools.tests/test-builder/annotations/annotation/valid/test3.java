/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
