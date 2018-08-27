/*******************************************************************************
 * Copyright (c) 2016 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *     Patrik Suzzi <psuzzi@gmail.com> - Bug 499226
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.tools.layout.spy.internal.dialogs.messages"; //$NON-NLS-1$
	public static String LayoutSpyDialog_shell_text;
	public static String LayoutSpyDialog_button_open_child;
	public static String LayoutSpyDialog_button_open_parent;
	public static String LayoutSpyDialog_button_select_control;
	public static String LayoutSpyDialog_button_show_overlay;
	public static String LayoutSpyDialog_button_show_coloring;
	public static String LayoutSpyDialog_label_children;
	public static String LayoutSpyDialog_label_layout;
	public static String LayoutSpyDialog_label_no_parent_control_selected;
	public static String LayoutSpyDialog_warning_bounds_outside_parent;
	public static String LayoutSpyDialog_warning_control_overlaps_siblings;
	public static String LayoutSpyDialog_warning_control_partially_clipped;
	public static String LayoutSpyDialog_warning_grab_horizontally_scrolling;
	public static String LayoutSpyDialog_warning_grab_vertical_scrolling;
	public static String LayoutSpyDialog_warning_hint_for_horizontally_scrollable;
	public static String LayoutSpyDialog_warning_hint_for_vertically_scrollable;
	public static String LayoutSpyDialog_warning_not_grabbing_horizontally;
	public static String LayoutSpyDialog_warning_not_grabbing_vertically;
	public static String LayoutSpyDialog_warning_prefix;
	public static String LayoutSpyDialog_warning_shorter_than_preferred_size;
	public static String LayoutSpyDialog_warning_unexpected_compute_size;
	public static String LayoutSpyDialog_warning_zero_size;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
