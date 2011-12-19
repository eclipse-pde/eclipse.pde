/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal;

import org.eclipse.osgi.service.debug.DebugTrace;

/**
 * This wrapper class helps to avoid the <code>java.lang.IllegalArgumentException</code>
 * thrown by the various trace methods when they try to format the message and it has the
 * class names followed by {}  
 */
class Trace implements DebugTrace {

	private DebugTrace trace;

	public void setDebugTrace(DebugTrace newDebugTrace) {
		this.trace = newDebugTrace;
	}

	private String quotify(String option) {
		if (option != null) {
			option = "'" + option + "'"; //$NON-NLS-1$//$NON-NLS-2$
		}
		return option;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#trace(java.lang.String, java.lang.String)
	 */
	public void trace(String option, String message) {
		if (trace == null)
			return;
		trace.trace(option, quotify(message));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#trace(java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	public void trace(String option, String message, Throwable error) {
		if (trace == null)
			return;
		trace.trace(option, quotify(message), error);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceDumpStack(java.lang.String)
	 */
	public void traceDumpStack(String option) {
		if (trace == null)
			return;
		trace.traceDumpStack(option);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceEntry(java.lang.String)
	 */
	public void traceEntry(String option) {
		if (trace == null)
			return;
		trace.traceEntry(option);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceEntry(java.lang.String, java.lang.Object)
	 */
	public void traceEntry(String option, Object methodArgument) {
		if (trace == null)
			return;
		trace.traceEntry(option, quotify(String.valueOf(methodArgument)));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceEntry(java.lang.String, java.lang.Object[])
	 */
	public void traceEntry(String option, Object[] methodArguments) {
		if (trace == null)
			return;
		if (methodArguments != null && methodArguments.length > 0) {
			Object[] methodArgs = new Object[methodArguments.length];
			for (int i = 0; i < methodArgs.length; i++) {
				methodArgs[i] = quotify(String.valueOf(methodArguments[i]));
			}
			trace.traceEntry(option, methodArgs);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceExit(java.lang.String)
	 */
	public void traceExit(String option) {
		if (trace == null)
			return;
		trace.traceExit(option);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugTrace#traceExit(java.lang.String, java.lang.Object)
	 */
	public void traceExit(String option, Object result) {
		if (trace == null)
			return;
		trace.traceExit(option, quotify(String.valueOf(result)));
	}

}