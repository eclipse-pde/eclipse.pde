package org.eclipse.pde.internal.editor.manifest;

public class NullMenuManager implements org.eclipse.jface.action.IMenuManager {

public void add(org.eclipse.jface.action.IAction action) {}
public void add(org.eclipse.jface.action.IContributionItem item) {}
public void addMenuListener(org.eclipse.jface.action.IMenuListener listener) {}
public void appendToGroup(String groupName, org.eclipse.jface.action.IAction action) {}
public void appendToGroup(String groupName, org.eclipse.jface.action.IContributionItem item) {}
public void fill(org.eclipse.swt.widgets.Composite parent) {}
public void fill(org.eclipse.swt.widgets.Menu parent, int index) {}
public void fill(org.eclipse.swt.widgets.ToolBar parent, int index) {}
public org.eclipse.jface.action.IContributionItem find(String id) {
	return null;
}
public org.eclipse.jface.action.IMenuManager findMenuUsingPath(String path) {
	return null;
}
public org.eclipse.jface.action.IContributionItem findUsingPath(String path) {
	return null;
}
public String getId() {
	return null;
}
public org.eclipse.jface.action.IContributionItem[] getItems() {
	return null;
}
public boolean getRemoveAllWhenShown() {
	return false;
}
public void insertAfter(String id, org.eclipse.jface.action.IAction action) {}
public void insertAfter(String iD, org.eclipse.jface.action.IContributionItem item) {}
public void insertBefore(String id, org.eclipse.jface.action.IAction action) {}
public void insertBefore(String iD, org.eclipse.jface.action.IContributionItem item) {}
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
public void markDirty() {}
public void prependToGroup(String groupName, org.eclipse.jface.action.IAction action) {}
public void prependToGroup(String groupName, org.eclipse.jface.action.IContributionItem item) {}
public org.eclipse.jface.action.IContributionItem remove(String id) {
	return null;
}
public org.eclipse.jface.action.IContributionItem remove(org.eclipse.jface.action.IContributionItem item) {
	return null;
}
public void removeAll() {}
public void removeMenuListener(org.eclipse.jface.action.IMenuListener listener) {}
public void setRemoveAllWhenShown(boolean removeAll) {}
public void setVisible(boolean visible) {}
public void update() {}
public void update(boolean force) {}
public void updateAll(boolean force) {}
}
