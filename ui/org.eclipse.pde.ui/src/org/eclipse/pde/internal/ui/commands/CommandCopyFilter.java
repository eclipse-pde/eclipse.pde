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

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public abstract class CommandCopyFilter {

	private static final ArrayList fFilters = new ArrayList();

	private CommandCopyFilter() {
		fFilters.add(this);
	}

	public final String filter(String serializedCommand, boolean surroundWithMarkup, String markupLabel) {
		if (surroundWithMarkup)
			return markup(escape(serializedCommand), markupLabel);
		return escape(serializedCommand);
	}

	protected abstract String escape(String serializedCommand);

	protected abstract String markup(String escapedSerializedCommand, String markupLabel);

	public abstract String getLabelText();

	public abstract String getToolTipText();

	public static CommandCopyFilter[] getFilters() {
		return (CommandCopyFilter[]) fFilters.toArray(new CommandCopyFilter[fFilters.size()]);
	}

	public static CommandCopyFilter getFilter(int index) {
		return (CommandCopyFilter) fFilters.get(index);
	}

	public static int indexOf(CommandCopyFilter filter) {

		int index = 0;
		for (Iterator i = fFilters.iterator(); i.hasNext();) {
			CommandCopyFilter f = (CommandCopyFilter) i.next();
			if (f == filter)
				return index;
			index++;
		}
		return -1;
	}

	public static final CommandCopyFilter NONE = new CommandCopyFilter() {
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_noFilter;
		}

		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_noFilterDesc;
		}

		protected String escape(String serializedCommand) {
			return serializedCommand;
		}

		protected String markup(String escapedSerializedCommand, String markupLabel) {
			return escapedSerializedCommand;
		}

	};

	public static final CommandCopyFilter HELP = new CommandCopyFilter() {
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_help;
		}

		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_helpDesc;
		}

		protected String escape(String serializedCommand) {
			// TODO: escape for Help
			return serializedCommand;
		}

		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuffer sb = new StringBuffer();

			sb.append("<a href='javascript:executeCommand(\""); //$NON-NLS-1$
			sb.append(escapedSerializedCommand);
			sb.append("\")'>"); //$NON-NLS-1$
			if (markupLabel != null)
				sb.append(markupLabel);
			sb.append("</a>"); //$NON-NLS-1$

			return sb.toString();
		}

	};

	public static final CommandCopyFilter CHEATSHEET = new CommandCopyFilter() {
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_cheatsheet;
		}

		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_cheatsheetDesc;
		}

		protected String escape(String serializedCommand) {
			// TODO: escape for Cheatsheets
			return serializedCommand;
		}

		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuffer sb = new StringBuffer();

			sb.append("<command serialization=\""); //$NON-NLS-1$
			sb.append(escapedSerializedCommand);
			sb.append("\"/>"); //$NON-NLS-1$

			return sb.toString();
		}

	};

	public static final CommandCopyFilter INTRO = new CommandCopyFilter() {
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_intro;
		}

		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_introDesc;
		}

		protected String escape(String serializedCommand) {
			// TODO: escape for Intro
			return serializedCommand;
		}

		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuffer sb = new StringBuffer();

			sb.append("<link\n"); //$NON-NLS-1$
			if (markupLabel != null) {
				sb.append("label=\""); //$NON-NLS-1$
				sb.append(markupLabel);
				sb.append("\"\n"); //$NON-NLS-1$
			}

			sb.append("id=\"TODO\"\n"); //$NON-NLS-1$

			sb.append("url=\""); //$NON-NLS-1$
			sb.append(escapedSerializedCommand);
			sb.append("\"\n"); //$NON-NLS-1$

			sb.append("<text>TODO</text>\n"); //$NON-NLS-1$

			sb.append("</link>"); //$NON-NLS-1$

			return sb.toString();
		}

	};

}
