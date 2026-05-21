/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.model;

import java.util.Objects;

public class PreferenceEntry extends AbstractModelObject {

	public enum Fields {
		nodePath, key, oldValue, newValue;
	}

	private PreferenceEntry parent;

	private boolean recentlyChanged;

	private String nodePath;

	private String key;

	private String oldValue;

	private String newValue;

	private long time;

	public PreferenceEntry() {
	}

	public PreferenceEntry(String nodePath, String key) {
		this.nodePath = nodePath;
		this.key = key;
	}

	public PreferenceEntry(String nodePath, String key, String oldValue, String newValue) {
		this.nodePath = nodePath;
		this.key = key;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public PreferenceEntry getParent() {
		return parent;
	}

	public void setParent(PreferenceEntry parent) {
		firePropertyChange("parent", this.parent, this.parent = parent);
	}

	public String getNodePath() {
		return nodePath;
	}

	public void setNodePath(String nodePath) {
		firePropertyChange("nodePath", this.nodePath, this.nodePath = nodePath);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		firePropertyChange("key", this.key, this.key = key);
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		firePropertyChange("oldValue", this.oldValue, this.oldValue = oldValue);
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		firePropertyChange("newValue", this.newValue, this.newValue = newValue);
	}

	public boolean isRecentlyChanged() {
		return recentlyChanged;
	}

	public void setRecentlyChanged(boolean recentlyChanged) {
		firePropertyChange("recentlyChanged", this.recentlyChanged, this.recentlyChanged = recentlyChanged);
	}

	// Identity is the (nodePath, key) pair. Mutable fields like oldValue/newValue/recentlyChanged
	// must not participate, because instances are stored in a WritableSet and mutated in place.
	@Override
	public int hashCode() {
		return Objects.hash(nodePath, key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		PreferenceEntry other = (PreferenceEntry) obj;
		return Objects.equals(nodePath, other.nodePath) && Objects.equals(key, other.key);
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
