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

import org.eclipse.pde.api.tools.annotations.NoExtend;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;
import org.eclipse.pde.api.tools.annotations.NoReference;

/**
 * Tests all tags are invalid when parent annotation is private or package default
 */
public @interface test13 {

	@interface inner1 {

		/**
		 */
		@NoExtend
		@NoInstantiate
		@NoReference
		public static class Clazz {

		}

		/**
		 */
		@NoExtend
		@NoInstantiate
		@NoReference
		public interface inter {

		}

		/**
		 */
		@NoReference
		public int field = 0;

		/**
		 */
		@NoReference
		public @interface annot {

		}

		/**
		 */
		@NoReference
		enum enu {

		}
	}

}
