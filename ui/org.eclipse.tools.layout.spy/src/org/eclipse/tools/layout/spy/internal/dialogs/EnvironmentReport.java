/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

/**
 * Describes the environment a report was taken in. Absolute pixel values cannot
 * be interpreted without the scaling factor and the windowing backend, so a
 * report is of little use without this header.
 */
final class EnvironmentReport {

	private EnvironmentReport() {
	}

	static void append(StringBuilder builder, Display display) {
		builder.append("SWT ").append(swtVersion()); //$NON-NLS-1$
		builder.append(" (").append(SWT.getPlatform()).append(", ").append(windowingBackend()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String gtkVersion = System.getProperty("org.eclipse.swt.internal.gtk.version"); //$NON-NLS-1$
		if (gtkVersion != null) {
			builder.append(", GTK ").append(gtkVersion); //$NON-NLS-1$
		}
		builder.append("\n"); //$NON-NLS-1$

		builder.append("OS: ").append(System.getProperty("os.name")).append(" ") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				.append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				.append("), Java ").append(System.getProperty("java.version")).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		Point dpi = display.getDPI();
		builder.append("DPI: ").append(dpi.x).append("x").append(dpi.y); //$NON-NLS-1$ //$NON-NLS-2$
		builder.append(", zoom ").append(dpi.x * 100 / 96).append("%"); //$NON-NLS-1$ //$NON-NLS-2$
		appendProperty(builder, ", swt.autoScale=", "swt.autoScale"); //$NON-NLS-1$ //$NON-NLS-2$
		appendProperty(builder, ", swt.autoScale.method=", "swt.autoScale.method"); //$NON-NLS-1$ //$NON-NLS-2$
		builder.append("\n"); //$NON-NLS-1$

		builder.append("Theme: ").append(Display.isSystemDarkTheme() ? "dark" : "light"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		builder.append("\n"); //$NON-NLS-1$

		Monitor[] monitors = display.getMonitors();
		builder.append("Monitors: ").append(monitors.length); //$NON-NLS-1$
		for (Monitor monitor : monitors) {
			Rectangle bounds = monitor.getBounds();
			builder.append(" ").append(bounds); //$NON-NLS-1$
		}
		builder.append("\n"); //$NON-NLS-1$
	}

	/** {@link SWT#getVersion()} packs major and revision into one number. */
	private static String swtVersion() {
		int version = SWT.getVersion();
		return version / 1000 + "." + version % 1000; //$NON-NLS-1$
	}

	/**
	 * Returns the windowing backend actually in use. On Linux this decides whether
	 * pixel coordinates and scaling behave the way the report suggests.
	 */
	private static String windowingBackend() {
		String backend = System.getenv("GDK_BACKEND"); //$NON-NLS-1$
		if (backend != null && !backend.isBlank()) {
			return backend;
		}
		if (System.getenv("WAYLAND_DISPLAY") != null) { //$NON-NLS-1$
			return "wayland"; //$NON-NLS-1$
		}
		String sessionType = System.getenv("XDG_SESSION_TYPE"); //$NON-NLS-1$
		if (sessionType != null && !sessionType.isBlank()) {
			return sessionType;
		}
		return "unknown"; //$NON-NLS-1$
	}

	private static void appendProperty(StringBuilder builder, String label, String key) {
		String value = System.getProperty(key);
		if (value != null && !value.isBlank()) {
			builder.append(label).append(value);
		}
	}
}
