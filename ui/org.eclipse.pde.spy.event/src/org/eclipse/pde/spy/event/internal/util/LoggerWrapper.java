/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.event.internal.util;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.pde.spy.event.Constants;

import jakarta.inject.Inject;

@SuppressWarnings("restriction")
@Creatable
public class LoggerWrapper extends Logger {
	@Optional
	@Inject
	private Logger logger;

	@Override
	public boolean isErrorEnabled() {
		if (logger != null) {
			return logger.isErrorEnabled();
		}
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		if (logger != null) {
			return logger.isTraceEnabled();
		}
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		if (logger != null) {
			return logger.isWarnEnabled();
		}
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		if (logger != null) {
			return logger.isInfoEnabled();
		}
		return false;
	}

	@Override
	public boolean isDebugEnabled() {
		if (logger != null) {
			return logger.isDebugEnabled();
		}
		return false;
	}

	@Override
	public void error(Throwable t, String message) {
		if (logger != null && isErrorEnabled()) {
			logger.error(t, withPluginInfo(message));
		}
	}

	@Override
	public void warn(Throwable t, String message) {
		if (logger != null && isWarnEnabled()) {
			logger.warn(t, withPluginInfo(message));
		}
	}

	@Override
	public void info(Throwable t, String message) {
		if (logger != null && isInfoEnabled()) {
			logger.info(t, withPluginInfo(message));
		}
	}

	@Override
	public void trace(Throwable t, String message) {
		if (logger != null && isTraceEnabled()) {
			logger.trace(t, withPluginInfo(message));
		}
	}

	@Override
	public void debug(Throwable t) {
		if (logger != null && isDebugEnabled()) {
			logger.debug(t);
		}
	}

	@Override
	public void debug(Throwable t, String message) {
		if (logger != null && isDebugEnabled()) {
			logger.debug(t, withPluginInfo(message));
		}
	}

	private String withPluginInfo(String message) {
		return String.format("Plugin '%s': %s", Constants.PLUGIN_ID, message); //$NON-NLS-1$
	}
}
