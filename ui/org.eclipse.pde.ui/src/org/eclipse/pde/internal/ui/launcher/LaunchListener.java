package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class LaunchListener
	implements ILaunchListener, IDebugEventSetListener {
	private ArrayList managedLaunches;

	public LaunchListener() {
		managedLaunches = new ArrayList();
	}

	public void manage(ILaunch launch) {
		if (managedLaunches.size() == 0)
			hookListener(true);

		if (!managedLaunches.contains(launch))
			managedLaunches.add(launch);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		update(launch, true);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}

	private void update(ILaunch launch, boolean remove) {
		if (managedLaunches.contains(launch)) {
			if (remove || launch.isTerminated()) {
				managedLaunches.remove(launch);
				if (managedLaunches.size() == 0) {
					hookListener(false);
				}
			}
		}
	}

	private void hookListener(boolean add) {
		DebugPlugin debugPlugin = DebugPlugin.getDefault();
		ILaunchManager launchManager = debugPlugin.getLaunchManager();
		if (add) {
			launchManager.addLaunchListener(this);
			debugPlugin.addDebugEventListener(this);
		} else {
			launchManager.removeLaunchListener(this);
			debugPlugin.removeDebugEventListener(this);
		}
	}

	private void doRestart(ILaunch launch) {
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		try {
			config.launch(launch.getLaunchMode(), new NullProgressMonitor());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void shutdown() {
		hookListener(false);
	}
	/**
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent)
	 */
	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			DebugEvent event = events[i];
			Object source = event.getSource();
			if (source instanceof IProcess
				&& event.getKind() == DebugEvent.TERMINATE) {
				IProcess process = (IProcess) source;
				ILaunch launch = process.getLaunch();
				if (launch != null) {
					try {
						launchTerminated(launch, process.getExitValue());
					} catch (DebugException e) {
					}
				}
			}
		}
	}

	private void launchTerminated(ILaunch launch, int returnValue) {
		if (managedLaunches.contains(launch)) {
			update(launch, true);
			if (returnValue == 23)
				doRestart(launch);
		}
	}
}
