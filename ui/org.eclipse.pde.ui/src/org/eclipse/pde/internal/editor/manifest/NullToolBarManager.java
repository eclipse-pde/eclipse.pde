package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.IToolBarManager;

public class NullToolBarManager implements IToolBarManager {

public void add(org.eclipse.jface.action.IAction action) {}
public void add(org.eclipse.jface.action.IContributionItem item) {}
public void appendToGroup(String groupName, org.eclipse.jface.action.IAction action) {}
public void appendToGroup(String groupName, org.eclipse.jface.action.IContributionItem item) {}
public org.eclipse.jface.action.IContributionItem find(String id) {
	return null;
}
public org.eclipse.jface.action.IContributionItem[] getItems() {
	return null;
}
public void insertAfter(String id, org.eclipse.jface.action.IAction action) {}
public void insertAfter(String iD, org.eclipse.jface.action.IContributionItem item) {}
public void insertBefore(String id, org.eclipse.jface.action.IAction action) {}
public void insertBefore(String iD, org.eclipse.jface.action.IContributionItem item) {}
public boolean isDirty() {
	return false;
}
public boolean isEmpty() {
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
public void update(boolean force) {}
}
