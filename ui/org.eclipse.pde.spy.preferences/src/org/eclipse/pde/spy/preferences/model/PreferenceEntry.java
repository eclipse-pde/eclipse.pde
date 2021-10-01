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

	public PreferenceEntry(PreferenceEntry parent, String nodePath, String key, String oldValue, String newValue) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
		result = prime * result + ((nodePath == null) ? 0 : nodePath.hashCode());
		result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + (recentlyChanged ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PreferenceEntry other = (PreferenceEntry) obj;
		if (key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!key.equals(other.key)) {
			return false;
		}
		if (newValue == null) {
			if (other.newValue != null) {
				return false;
			}
		} else if (!newValue.equals(other.newValue)) {
			return false;
		}
		if (nodePath == null) {
			if (other.nodePath != null) {
				return false;
			}
		} else if (!nodePath.equals(other.nodePath)) {
			return false;
		}
		if (oldValue == null) {
			if (other.oldValue != null) {
				return false;
			}
		} else if (!oldValue.equals(other.oldValue)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		if (recentlyChanged != other.recentlyChanged) {
			return false;
		}
		return true;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
