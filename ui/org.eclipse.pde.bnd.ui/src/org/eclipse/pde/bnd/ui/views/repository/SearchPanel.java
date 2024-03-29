/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.views.repository;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.osgi.resource.Requirement;

public abstract class SearchPanel implements IPersistable {

	public static final String			PROP_VALUE	= "requirement";
	public final String					PROP_ERROR	= "error";

	private final PropertyChangeSupport	propSupport	= new PropertyChangeSupport(this);

	private String						error;
	private Requirement					requirement;

	public abstract Control createControl(Composite parent);

	public abstract void setFocus();

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

	public String getError() {
		return this.error;
	}

	protected void setError(String error) {
		String oldError = this.error;
		this.error = error;
		propSupport.firePropertyChange(PROP_ERROR, oldError, error);
	}

	public Requirement getRequirement() {
		return this.requirement;
	}

	protected void setRequirement(Requirement requirement) {
		Requirement oldRequiremenmt = this.requirement;
		this.requirement = requirement;
		propSupport.firePropertyChange(PROP_VALUE, oldRequiremenmt, requirement);
	}

	public Image createImage(Device device) {
		return null;
	}

	public abstract void restoreState(IMemento memento);

}
