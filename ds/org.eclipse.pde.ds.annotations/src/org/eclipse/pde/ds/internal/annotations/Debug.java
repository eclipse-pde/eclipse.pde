/*******************************************************************************
 * Copyright (c) 2012, 2016 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class Debug {

	private final DebugTrace trace;

	private final String option;

	private Debug(DebugTrace trace, String option) {
		this.trace = trace;
		this.option = option;
	}

	public static Debug getDebug(String name) {
		if (!name.startsWith("/")) //$NON-NLS-1$
			name = "/" + name; //$NON-NLS-1$

		DebugTrace trace = null;
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		BundleContext ctx;
		if (bundle != null && (ctx = bundle.getBundleContext()) != null) {
			ServiceReference<DebugOptions> ref = ctx.getServiceReference(DebugOptions.class);
			if (ref != null) {
				DebugOptions options = ctx.getService(ref);
				if (options.isDebugEnabled() && options.getBooleanOption(Activator.PLUGIN_ID + name, false)) {
					trace = options.newDebugTrace(Activator.PLUGIN_ID);
				}

				ctx.ungetService(ref);
			}
		}

		return new Debug(trace, name);
	}

	public boolean isDebugging() {
		return trace != null;
	}

	public void trace(String message) {
		if (trace != null)
			trace.trace(option, message);
	}

	public void trace(String message, Throwable error) {
		if (trace != null)
			trace.trace(message, message, error);
	}

	public void traceDumpStack() {
		if (trace != null)
			trace.traceDumpStack(option);
	}

	public void traceEntry() {
		if (trace != null)
			trace.traceEntry(option);
	}

	public void traceEntry(Object methodArgument) {
		if (trace != null)
			trace.traceEntry(option, methodArgument);
	}

	public void traceEntry(Object[] methodArguments) {
		if (trace != null)
			trace.traceEntry(option, methodArguments);
	}

	public void traceExit() {
		if (trace != null)
			trace.traceExit(option);
	}

	public void traceExit(Object result) {
		if (trace != null)
			trace.traceExit(option, result);
	}
}
