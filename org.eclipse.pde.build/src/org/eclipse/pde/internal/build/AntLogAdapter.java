package org.eclipse.pde.internal.build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

public class AntLogAdapter implements ILog {
	private Object antLog;
	private Method log;
	
	public AntLogAdapter(Object antLog) throws NoSuchMethodException{
		this.antLog = antLog;
		try {
			log = antLog.getClass().getMethod("log", new Class[] { String.class, int.class }); //$NON-NLS-1$
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addLogListener(ILogListener listener) {
		throw new UnsupportedOperationException();
	}

	public Bundle getBundle() {
		return BundleHelper.getDefault().getBundle();
	}

	public void log(IStatus status) {
		try {
			log.invoke(antLog, new Object[] { status.getMessage(), new Integer(mapLogLevels(status.getSeverity()))} );
			IStatus[] nestedStatus = status.getChildren();
			if (nestedStatus != null) {
				for (int i = 0; i < nestedStatus.length; i++) {
					log(nestedStatus[i]);
				}
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int mapLogLevels(int iStatusLevel) {
		switch (iStatusLevel) {
			case IStatus.ERROR :
				return 0;
			case IStatus.OK:
				return 2;
			case IStatus.INFO:
				return 2;
			case IStatus.WARNING:
				return 1;
			default:
				return 1;
		}
	}
	
	public void removeLogListener(ILogListener listener) {
		throw new UnsupportedOperationException();
	}

}
