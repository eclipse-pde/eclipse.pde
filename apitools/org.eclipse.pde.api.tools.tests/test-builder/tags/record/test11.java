/*******************************************************************************
 * Copyright (c) 2025 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/

/**
 * Tests all tags are invalid when parent record is package default
 */
public record test11(int a) {

	record inner1(int a) {

		/**
		 * @noreference
		 */
		public record inner2() {

		}

	}

}
