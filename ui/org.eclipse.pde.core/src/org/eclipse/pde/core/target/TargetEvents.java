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

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Target events and event topic definitions.
 *
 * <p>
 * The following code is an example of to subscribe to the
 * {@link #TOPIC_TARGET_SAVED} event:
 * </p>
 *
 * <pre>
 * EventHandler eventHandler = event -> {
 * 	if (event.getProperty(IEventBroker.DATA) instanceof ITargetHandle handle) {
 * 		// Work with the target handle...
 * 	}
 * };
 * IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
 * IEventBroker broker = context.get(IEventBroker.class);
 * if (broker != null) {
 * 	broker.subscribe(TargetEvents.TOPIC_TARGET_SAVED, eventHandler);
 * 	// Do not forget to unsubscribe later!
 * }
 * </pre>
 *
 * @see ITargetPlatformService
 * @see IEventBroker#subscribe(String, EventHandler)
 * @see IEventBroker#subscribe(String, String, EventHandler, boolean)
 * @see IEventBroker#unsubscribe(EventHandler)
 * @since 3.13
 */
public class TargetEvents {

	/**
	 * Base topic for all target events.
	 */
	public static final String TOPIC_BASE = "org/eclipse/pde/core/target/TargetEvents"; //$NON-NLS-1$

	/**
	 * Topic for all target events.
	 */
	public static final String TOPIC_ALL = TOPIC_BASE + "/*"; //$NON-NLS-1$

	/**
	 * Sent when workspace target definition is changed.
	 * <p>
	 * The {@link IEventBroker#DATA data} {@link Event#getProperty(String) event
	 * property} of events with this topic is the changed
	 * {@link ITargetDefinition}.
	 * </p>
	 *
	 * @see ITargetPlatformService#getWorkspaceTargetDefinition()
	 */
	public static final String TOPIC_WORKSPACE_TARGET_CHANGED = TOPIC_BASE + "/workspaceTargetChanged"; //$NON-NLS-1$

	/**
	 * Sent when a target is saved.
	 * <p>
	 * The {@link IEventBroker#DATA data} {@link Event#getProperty(String) event
	 * property} of events with this topic is the saved {@link ITargetHandle}.
	 * </p>
	 *
	 * @see ITargetPlatformService#saveTargetDefinition(ITargetDefinition)
	 * @see IEventBroker
	 * @since 3.20
	 */
	public static final String TOPIC_TARGET_SAVED = TOPIC_BASE + "/targetSaved"; //$NON-NLS-1$

	/**
	 * Sent when a target is deleted.
	 * <p>
	 * The {@link IEventBroker#DATA data} {@link Event#getProperty(String) event
	 * property} of events with this topic is the deleted {@link ITargetHandle}.
	 * </p>
	 *
	 * @see ITargetPlatformService#deleteTarget(ITargetHandle)
	 * @since 3.20
	 */
	public static final String TOPIC_TARGET_DELETED = TOPIC_BASE + "/targetDeleted"; //$NON-NLS-1$
}
