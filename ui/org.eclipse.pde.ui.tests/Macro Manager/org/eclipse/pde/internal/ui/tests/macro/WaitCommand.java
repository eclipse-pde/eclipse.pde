/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

public class WaitCommand extends MacroCommand {
	public static final String TYPE = "wait";
	private static final WidgetIdentifier nullIdentifier = new WidgetIdentifier(new Path(""), new Path(""));
	
	private static class JobListener extends JobChangeAdapter {
		private int counter=0;
		private IProgressMonitor monitor;
		private Thread t;
		
		public JobListener(IProgressMonitor monitor, Thread t, int number) {
			this.counter = number;
			this.monitor = monitor;
			this.t = t;
		}
		private synchronized void change(int increment) {
			this.counter += increment;
			if (counter==0) { 
				monitor.subTask("");
				synchronized (t) {
					t.interrupt();
				}
			}
		}
		public void running(IJobChangeEvent event) {
			Job job = event.getJob();
			if (!job.isSystem()) 
				change(1);
		}
		public void done(IJobChangeEvent event) {
			Job job = event.getJob();
			if (!job.isSystem()) 
				change(-1);
		}
	}

	public WaitCommand() {
		super(nullIdentifier);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.MacroCommand#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.MacroCommand#processEvent(org.eclipse.swt.widgets.Event)
	 */
	public void processEvent(Event e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<command type=\"");
		writer.print(getType());
		writer.print("\"");
		writer.println("/>");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.macro.IPlayable#playback(org.eclipse.swt.widgets.Composite)
	 */
	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		if (parent.isDisposed())
			return false;
		IJobManager jobManager = Platform.getJobManager();
		int nrunning = getNumberOfRunningJobs(jobManager);
		if (nrunning==0) return true;
		String message = "Waiting for the background jobs...";
		JobListener listener = new JobListener(monitor, Thread.currentThread(), nrunning);
		jobManager.addJobChangeListener(listener);
		monitor.subTask(message);
		try {
			Thread.sleep(30000);
		}
		catch (InterruptedException e) {
		}
		jobManager.removeJobChangeListener(listener);
		return true;
	}
	private int getNumberOfRunningJobs(IJobManager manager) {
		int count = 0;
		Job[] jobs = manager.find(null);
		for (int i=0; i<jobs.length; i++) {
			if (!jobs[i].isSystem() && jobs[i].getState()==Job.RUNNING)
				count++;
		}
		return count;
	}
}