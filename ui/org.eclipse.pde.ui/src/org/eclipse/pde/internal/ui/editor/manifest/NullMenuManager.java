package org.eclipse.pde.internal.ui.editor.manifest;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;

public class NullMenuManager implements IMenuManager {

	public void add(IAction action) {
	}
	public void add(IContributionItem item) {
	}
	public void addMenuListener(IMenuListener listener) {
	}
	public void appendToGroup(String groupName, IAction action) {
	}
	public void appendToGroup(String groupName, IContributionItem item) {
	}
	public void fill(org.eclipse.swt.widgets.Composite parent) {
	}
	public void fill(org.eclipse.swt.widgets.Menu parent, int index) {
	}
	public void fill(org.eclipse.swt.widgets.ToolBar parent, int index) {
	}
	public IContributionItem find(String id) {
		return null;
	}
	public IMenuManager findMenuUsingPath(String path) {
		return null;
	}
	public IContributionItem findUsingPath(String path) {
		return null;
	}
	public String getId() {
		return null;
	}
	public IContributionItem[] getItems() {
		return null;
	}
	public boolean getRemoveAllWhenShown() {
		return false;
	}
	public void setParent(IContributionManager parent) {
	}
	public void insertAfter(String id, IAction action) {
	}
	public void insertAfter(String iD, IContributionItem item) {
	}
	public void insertBefore(String id, IAction action) {
	}
	public void insertBefore(String iD, IContributionItem item) {
	}
	public boolean isDirty() {
		return false;
	}
	public boolean isDynamic() {
		return false;
	}
	public boolean isEmpty() {
		return false;
	}
	public boolean isEnabled() {
		return false;
	}
	public boolean isGroupMarker() {
		return false;
	}
	public boolean isSeparator() {
		return false;
	}
	public boolean isVisible() {
		return false;
	}
	public void markDirty() {
	}
	public void prependToGroup(String groupName, IAction action) {
	}
	public void prependToGroup(String groupName, IContributionItem item) {
	}
	public IContributionItem remove(String id) {
		return null;
	}
	public IContributionItem remove(IContributionItem item) {
		return null;
	}
	public IContributionManagerOverrides getOverrides() {
		return new IContributionManagerOverrides() {
			public Boolean getEnabled(IContributionItem item) {
				return null;
			}
			public Integer getAccelerator(IContributionItem item) {
				return null;
			}
			public String getAcceleratorText(IContributionItem item) {
				return null;
			}
			public String getText(IContributionItem item) {
				return null;
			}
		};
	}
	public void removeAll() {
	}
	public void removeMenuListener(IMenuListener listener) {
	}
	public void setRemoveAllWhenShown(boolean removeAll) {
	}
	public void setVisible(boolean visible) {
	}
	public void update() {
	}
	public void update(String name) {
	}
	public void update(boolean force) {
	}
	public void updateAll(boolean force) {
	}
	public boolean isEnabledAllowed() {
		return true;
	}
	public void setEnabledAllowed(boolean value) {
	}
}