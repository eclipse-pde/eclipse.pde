package org.eclipse.pde.internal.editor;

import org.eclipse.core.runtime.*;
import org.eclipse.ui.internal.registry.*;
import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import java.util.*;


public class SubActionBars implements IActionBars
{
	private IActionBars parent;
	private boolean active = false;
	private Map actionHandlers;
	private SubMenuManager menuMgr;
	private SubStatusLineManager statusLineMgr;
	private SubToolBarManager toolBarMgr;
	private ListenerList propertyChangeListeners = new ListenerList();
	private boolean actionHandlersChanged;

	/** Property constant for changes to action handlers. */
	public static final String P_ACTION_HANDLERS = "org.eclipse.ui.internal.actionHandlers";

public SubActionBars(IActionBars parent) {
	this.parent = parent;
}
public void activate() {
	setActive(true);
}
public void addPropertyChangeListener(IPropertyChangeListener listener) { 
	propertyChangeListeners.add(listener);
}
public void clearGlobalActionHandlers() {
	if (actionHandlers != null) {
		actionHandlers.clear();
		actionHandlersChanged = true;
	}
}
public void deactivate() {
	setActive(false);
}
public void dispose() {
	if (actionHandlers != null)
		actionHandlers.clear();
	if (menuMgr != null)
		menuMgr.removeAll();
	if (statusLineMgr != null)
		statusLineMgr.removeAll();
	if (toolBarMgr != null)
		toolBarMgr.removeAll();
}
protected void firePropertyChange(PropertyChangeEvent event) {
	Object[] listeners = propertyChangeListeners.getListeners();
	for (int i = 0; i < listeners.length; ++i) {
		((IPropertyChangeListener) listeners[i]).propertyChange(event);
	}
}
public IAction getGlobalActionHandler(String actionID) {
	if (actionHandlers == null)
		return null;
	return (IAction) actionHandlers.get(actionID);
}
public Map getGlobalActionHandlers() {
	return actionHandlers;
}
public IMenuManager getMenuManager() {
	if (menuMgr == null) {
		menuMgr = new SubMenuManager(parent.getMenuManager());
		menuMgr.setVisible(active);
	}
	return menuMgr;
}
public IStatusLineManager getStatusLineManager() {
	if (statusLineMgr == null) {
		statusLineMgr = new SubStatusLineManager(parent.getStatusLineManager());
		statusLineMgr.setVisible(active);
	}
	return statusLineMgr;
}
public IToolBarManager getToolBarManager() {
	if (toolBarMgr == null) {
		toolBarMgr = new SubToolBarManager(parent.getToolBarManager());
		toolBarMgr.setVisible(active);
	}
	return toolBarMgr;
}
public void partChanged(IWorkbenchPart part) {
}
public void removePropertyChangeListener(IPropertyChangeListener listener) {
	propertyChangeListeners.remove(listener);
}
private void setActive(boolean set) {
	active = set;
	if (menuMgr != null)
		menuMgr.setVisible(set);
	if (statusLineMgr != null)
		statusLineMgr.setVisible(set);
	if (toolBarMgr != null)
		toolBarMgr.setVisible(set);
}
public void setGlobalActionHandler(String actionID, IAction handler) {
	if (handler != null) {
		if (actionHandlers == null)
			actionHandlers = new HashMap(11);
		actionHandlers.put(actionID, handler);
	} else {
		if (actionHandlers != null)
			actionHandlers.remove(actionID);
	}
	actionHandlersChanged = true;
}
public void updateActionBars() {
	parent.updateActionBars();
	if (actionHandlersChanged) {
		// Doesn't actually pass the old and new values
		firePropertyChange(new PropertyChangeEvent(this, P_ACTION_HANDLERS, null, null));
		actionHandlersChanged = false;
	}
}
}
