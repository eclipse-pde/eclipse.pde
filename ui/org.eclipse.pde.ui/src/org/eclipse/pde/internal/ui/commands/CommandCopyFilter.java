/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import java.util.ArrayList;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public abstract class CommandCopyFilter {

	private static final ArrayList<CommandCopyFilter> fFilters = new ArrayList<>();

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
		return fFilters.toArray(new CommandCopyFilter[fFilters.size()]);
	}

	public static CommandCopyFilter getFilter(int index) {
		return fFilters.get(index);
	}

	public static int indexOf(CommandCopyFilter filter) {

		int index = 0;
		for (CommandCopyFilter f : fFilters) {
			if (f == filter)
				return index;
			index++;
		}
		return -1;
	}

	public static final CommandCopyFilter NONE = new CommandCopyFilter() {
		@Override
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_noFilter;
		}

		@Override
		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_noFilterDesc;
		}

		@Override
		protected String escape(String serializedCommand) {
			return serializedCommand;
		}

		@Override
		protected String markup(String escapedSerializedCommand, String markupLabel) {
			return escapedSerializedCommand;
		}

	};

	public static final CommandCopyFilter HELP = new CommandCopyFilter() {
		@Override
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_help;
		}

		@Override
		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_helpDesc;
		}

		@Override
		protected String escape(String serializedCommand) {
			// TODO: escape for Help
			return serializedCommand;
		}

		@Override
		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuilder sb = new StringBuilder();

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
		@Override
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_cheatsheet;
		}

		@Override
		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_cheatsheetDesc;
		}

		@Override
		protected String escape(String serializedCommand) {
			// TODO: escape for Cheatsheets
			return serializedCommand;
		}

		@Override
		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuilder sb = new StringBuilder();

			sb.append("<command serialization=\""); //$NON-NLS-1$
			sb.append(escapedSerializedCommand);
			sb.append("\"/>"); //$NON-NLS-1$

			return sb.toString();
		}

	};

	public static final CommandCopyFilter INTRO = new CommandCopyFilter() {
		@Override
		public String getLabelText() {
			return PDEUIMessages.CommandCopyFilter_intro;
		}

		@Override
		public String getToolTipText() {
			return PDEUIMessages.CommandCopyFilter_introDesc;
		}

		@Override
		protected String escape(String serializedCommand) {
			// TODO: escape for Intro
			return serializedCommand;
		}

		@Override
		protected String markup(String escapedSerializedCommand, String markupLabel) {
			StringBuilder sb = new StringBuilder();

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
