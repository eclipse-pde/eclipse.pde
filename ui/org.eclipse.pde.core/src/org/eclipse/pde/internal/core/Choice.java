/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.core;
/**
 * @version 	1.0
 * @author
 */
public class Choice {
	private String label;
	private String value;
	public Choice(String value, String label) {
		this.value = value;
		this.label = label;
	}
	
	public String getValue() {
		return value;
	}
	public String getLabel() {
		return label;
	}
	public String toString() {
		return label;
	}
}
