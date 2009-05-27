/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import java.util.*;
import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.ListenerList;

public class TagManager {

	private Hashtable fCommandToTags = new Hashtable();
	private ListenerList fListeners = new ListenerList();

	public void update(Command command, String tags) {
		if ((tags == null) || ("".equals(tags))) { //$NON-NLS-1$
			fCommandToTags.remove(command);
		} else {
			fCommandToTags.put(command, tags);
		}
		fireTagManagerChanged();
	}

	private boolean hasTag(String tags, String tag) {
		String[] tagArray = tags.split(","); //$NON-NLS-1$
		for (int i = 0; i < tagArray.length; i++) {
			String trimTag = tagArray[i].trim();
			if (tag.equalsIgnoreCase(trimTag))
				return true;
		}

		return false;
	}

	public String[] getTags() {
		HashSet tagSet = new HashSet();
		for (Iterator i = fCommandToTags.values().iterator(); i.hasNext();) {
			String tags = (String) i.next();

			String[] tagArray = tags.split(","); //$NON-NLS-1$
			for (int j = 0; j < tagArray.length; j++) {
				String trimTag = tagArray[j].trim();
				tagSet.add(trimTag);
			}
		}

		return (String[]) tagSet.toArray(new String[tagSet.size()]);
	}

	public String getTags(Command command) {
		String tags = (String) fCommandToTags.get(command);
		if (tags == null)
			return ""; //$NON-NLS-1$
		return tags;
	}

	public Command[] getCommands(String tag) {
		ArrayList list = new ArrayList();

		for (Iterator i = fCommandToTags.keySet().iterator(); i.hasNext();) {
			Command command = (Command) i.next();
			String tags = (String) fCommandToTags.get(command);
			if (hasTag(tags, tag)) {
				list.add(command);
			}
		}

		return (Command[]) list.toArray(new Command[list.size()]);
	}

	public interface Listener {
		void tagManagerChanged();
	}

	public void addListener(Listener listener) {
		fListeners.add(listener);
	}

	public void removeListener(Listener listener) {
		fListeners.remove(listener);
	}

	private void fireTagManagerChanged() {
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			Listener listener = (Listener) listeners[i];
			listener.tagManagerChanged();
		}
	}

}
