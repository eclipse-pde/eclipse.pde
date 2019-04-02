/********************************************************************************
 * Copyright (c) 2019 ArSysOp and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 ********************************************************************************/
package org.eclipse.pde.core.target;

/**
 * Target events and event topic definitions
 *
 * @since 3.13
 */
public class TargetEvents {

	/**
	 * Base topic for all Target events
	 */
	public static final String TOPIC_BASE = "org/eclipse/pde/core/target/TargetEvents"; //$NON-NLS-1$

	/**
	 * Topic for all Target events
	 */
	public static final String TOPIC_ALL = TOPIC_BASE + "/*"; //$NON-NLS-1$

	/**
	 * Sent when workspace target definition is changed
	 *
	 * @see ITargetPlatformService#getWorkspaceTargetDefinition()
	 */
	public static final String TOPIC_WORKSPACE_TARGET_CHANGED = TOPIC_BASE + "/workspaceTargetChanged"; //$NON-NLS-1$

}
