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
package a.b.c;

import org.eclipse.pde.api.tools.annotations.NoExtend;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;
import org.eclipse.pde.api.tools.annotations.NoReference;
import org.eclipse.pde.api.tools.annotations.NoOverride;
import org.eclipse.pde.api.tools.annotations.NoImplement;

/**
 * Tests all tags are invalid when parent class is private or package default
 */
public class test12 {

	static class inner1 {

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
		@NoImplement
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
		
		/**
		 */
		@NoOverride
		@NoReference
		public void method(){
			
		}

	}

}
