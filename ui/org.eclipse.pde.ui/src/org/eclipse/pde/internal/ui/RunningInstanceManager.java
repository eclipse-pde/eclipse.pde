package org.eclipse.pde.internal.ui;

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IProcess;

public class RunningInstanceManager implements IDebugEventSetListener {
	ArrayList instances = new ArrayList();
	
	class Instance {
		IPath data;
		ILaunch launch;
		Instance(ILaunch launch, IPath data) {
			this.launch = launch;
			this.data = data;
		}
	}
	
	public RunningInstanceManager() {
	}
	
	public void register(ILaunch launch, IPath data) {
		instances.add(new Instance(launch, data));
	}
	
	Instance findInstance(ILaunch launch) {
		for (int i = 0; i<instances.size(); i++) {
			Instance instance = (Instance)instances.get(i);
			if (instance.launch.equals(launch)) return instance;
		}
		return null;
	}
	
	Instance findInstance(IPath data) {
		for (int i = 0; i<instances.size(); i++) {
			Instance instance = (Instance)instances.get(i);
			if (instance.data.equals(data)) return instance;
		}
		return null;
	}
	
	public void handleDebugEvents(DebugEvent [] events) {
		for (int i=0; i<events.length; i++) {
			handleDebugEvent(events[i]);
		}
	}
	public void handleDebugEvent(DebugEvent e) {
		if (instances.size() == 0)
			return;
		Object obj = e.getSource();
		if (obj instanceof IProcess) {
			if ((e.getKind() & DebugEvent.TERMINATE) != 0) {
				ILaunch launch = ((IProcess) obj).getLaunch();
				if (launch != null) {
					Instance instance = findInstance(launch);
					if (instance!=null && instance.launch.isTerminated()) {
						instances.remove(instance);
					}
				}
			}
		}
	}
	public boolean isRunning(IPath data) {
		if (data==null)
			return instances.size()>0;
		else {
			Instance instance = findInstance(data);
			return instance != null;
		}
	}
	public void clear() {
		instances.clear();
	}
}