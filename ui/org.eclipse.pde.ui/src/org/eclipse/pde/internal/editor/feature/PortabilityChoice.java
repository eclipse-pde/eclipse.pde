/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor.feature;
/**
 * @version 	1.0
 * @author
 */
public class PortabilityChoice {
	private String label;
	private String value;
	public PortabilityChoice(String value, String label) {
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
