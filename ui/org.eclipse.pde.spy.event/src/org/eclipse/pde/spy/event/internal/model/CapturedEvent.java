/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package org.eclipse.pde.spy.event.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapturedEvent implements IEventItem {
	private String topic;

	private String publisherClassName = ""; //$NON-NLS-1$

	private String changedElementClassName = ""; //$NON-NLS-1$

	private List<Parameter> parameters;

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public void setPublisherClassName(String publisherClassName) {
		this.publisherClassName = publisherClassName;
	}

	public String getPublisherClassName() {
		return publisherClassName;
	}

	public void setChangedElementClassName(String changedElementClassName) {
		this.changedElementClassName = changedElementClassName;
	}

	public String getChangedElementClassName() {
		return changedElementClassName;
	}

	public void addParameter(String name, Object value) {
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		parameters.add(new Parameter(name, value));
	}

	public List<Parameter> getParameters() {
		return parameters != null ? parameters : Collections.emptyList();
	}

	public boolean hasParameters() {
		return parameters != null;
	}

	@Override
	public String toString() {
		return topic;
	}

	@Override
	public String getName() {
		return getTopic();
	}

	@Override
	public String getParam1() {
		return getPublisherClassName();
	}

	@Override
	public String getParam2() {
		return getChangedElementClassName();
	}
}
