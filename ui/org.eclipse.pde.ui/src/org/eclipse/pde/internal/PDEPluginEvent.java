package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

public class PDEPluginEvent {
	public static final int EXTERNAL_PLUGINS_CHANGED = 1;

	private int type;
	private Object data;
	
	public PDEPluginEvent(int type) {
		this.type = type;
	}
	
	public PDEPluginEvent(int type, Object data) {
		this(type);
		this.data = data;
	}
	
	public int getEventType() {
		return type;
	}
	
	public Object getData() {
		return data;
	}
}
