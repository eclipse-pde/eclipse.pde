/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;

public class AntLogAdapter implements ILog {
	private final Object antLog;
	private Method log;

	public AntLogAdapter(Object antLog) throws NoSuchMethodException {
		this.antLog = antLog;
		try {
			log = antLog.getClass().getMethod("log", String.class, int.class); //$NON-NLS-1$
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addLogListener(ILogListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getBundle() {
		return BundleHelper.getDefault().getBundle();
	}

	@Override
	public void log(IStatus status) {
		try {
			String statusMessage = status.getMessage();
			String exceptionMessage = status.getException() != null ? status.getException().getMessage() : null;

			log.invoke(antLog, statusMessage, Integer.valueOf(mapLogLevels(status.getSeverity())));
			if (exceptionMessage != null && !exceptionMessage.equals(statusMessage)) {
				log.invoke(antLog, exceptionMessage, Integer.valueOf(mapLogLevels(status.getSeverity())));
			}
			IStatus[] nestedStatus = status.getChildren();
			if (nestedStatus != null) {
				for (IStatus element : nestedStatus) {
					log(element);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int mapLogLevels(int iStatusLevel) {
		switch (iStatusLevel) {
			case IStatus.ERROR :
				return 0;
			case IStatus.OK :
				return 2;
			case IStatus.INFO :
				return 2;
			case IStatus.WARNING :
				return 1;
			default :
				return 1;
		}
	}

	@Override
	public void removeLogListener(ILogListener listener) {
		throw new UnsupportedOperationException();
	}

}
