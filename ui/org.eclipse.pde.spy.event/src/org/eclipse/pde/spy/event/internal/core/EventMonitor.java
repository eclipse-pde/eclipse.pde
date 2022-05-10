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
package org.eclipse.pde.spy.event.internal.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.internal.workbench.UIEventPublisher;
import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.pde.spy.event.internal.model.CapturedEvent;
import org.eclipse.pde.spy.event.internal.model.CapturedEventFilter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

@SuppressWarnings("restriction")
public class EventMonitor {

	public interface NewEventListener {
		void newEvent(CapturedEvent event);
	}

	@SuppressWarnings({ "serial" })
	private static Set<Integer> EVENT_HELPER_CLASSES = new HashSet<Integer>() {
		{
			add(UIEvents.class.getName().hashCode());
			add(UIEventPublisher.class.getName().hashCode());
		}
	};

	private Collection<CapturedEventFilter> filters;

	private final IEventBroker eventBroker;

	private NewEventListener listener;

	private CapturedEventFilterMatcher eventFilterMatcher;

	private final EventHandler eventHandler = new EventHandler() {
		@Override
		public void handleEvent(Event event) {
			if (listener == null) {
				return;
			}

			CapturedEvent capturedEvent = new CapturedEvent();
			capturedEvent.setTopic(event.getTopic());
			capturedEvent.setPublisherClassName(getPublisherClassName());

			for (String propertyName : event.getPropertyNames()) {
				Object value = event.getProperty(propertyName);
				capturedEvent.addParameter(propertyName, value);
				if (value != null && UIEvents.EventTags.ELEMENT.equals(propertyName)) {
					capturedEvent.setChangedElementClassName(value.getClass().getName());
				}
			}

			if (shouldBeCaptured(capturedEvent)) {
				listener.newEvent(capturedEvent);
			}
		}
	};

	public EventMonitor(IEventBroker eventBroker) {
		this.eventBroker = eventBroker;
	}

	public void start(String baseTopic, Collection<CapturedEventFilter> filters) {
		this.filters = filters;
		eventBroker.subscribe(baseTopic, eventHandler);
	}

	public void stop() {
		eventBroker.unsubscribe(eventHandler);
	}

	public void setNewEventListener(NewEventListener listener) {
		this.listener = listener;
	}

	private boolean shouldBeCaptured(CapturedEvent event) {
		if (filters != null) {
			Iterator<CapturedEventFilter> iter = filters.iterator();
			while (iter.hasNext()) {
				if (!getEventFilterMatcher().matches(event, iter.next())) {
					return false;
				}
			}
		}
		return true;
	}

	private String getPublisherClassName() {
		StackTraceElement items[] = Thread.currentThread().getStackTrace();
		boolean foundEventBroker = false;

		for (int i = 0; i < items.length; i++) {
			String clsName = items[i].getClassName();
			if (!foundEventBroker && clsName.equals(EventBroker.class.getName())) {
				foundEventBroker = true;
			} else if (foundEventBroker) {
				if (!EVENT_HELPER_CLASSES.contains(clsName.hashCode())) {
					return String.format("%s (%s:%d)", clsName, items[i].getMethodName(), items[i].getLineNumber()); //$NON-NLS-1$
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

	private CapturedEventFilterMatcher getEventFilterMatcher() {
		if (eventFilterMatcher == null) {
			eventFilterMatcher = new CapturedEventFilterMatcher();
		}
		return eventFilterMatcher;
	}
}
