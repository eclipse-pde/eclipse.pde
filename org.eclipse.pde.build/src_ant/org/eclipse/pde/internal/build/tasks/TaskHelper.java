package org.eclipse.pde.internal.build.tasks;

import org.eclipse.core.runtime.IStatus;

public class TaskHelper {
	public static StringBuffer statusToString(IStatus status, StringBuffer b) {
		IStatus[] nestedStatus = status.getChildren();
		if (b == null)
			b = new StringBuffer();
		b.append(status.getMessage());
		for (int i = 0; i < nestedStatus.length; i++) {
			b.append('\n');
			b.append(statusToString(nestedStatus[i], b));
		}
		return b;
	}
}
