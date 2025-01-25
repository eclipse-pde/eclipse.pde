/*******************************************************************************
 * Copyright (c)  2017 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.markers;

/**
 * Interface defining relevance values for API tools resolutions
 */

public interface IApiToolProposalRelevance {


	public static final int CONFIGURE_PROBLEM_SEVERITY = -10;
	public static final int REMOVE_UNUSED_FILTER = 10;
	public static final int FILTER_PROBLEM_WITH_COMMENT = 5;

}
