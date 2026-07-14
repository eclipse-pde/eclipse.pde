/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

/**
 * Represents version increment settings for a specific segment (major, minor, or micro).
 * 
 * @since 1.2
 */
public class ApiConfigSettings {
	
	/**
	 * Segment types for version increments
	 */
	public enum VersionSegment {
		MAJOR, MINOR, MICRO
	}
	
	/**
	 * Error handling mode
	 */
	public enum ErrorMode {
		ERROR, WARNING, IGNORE, FILTER
	}
	
	/**
	 * Version increment rule for a segment
	 */
	public static class VersionIncrementRule {
		private final VersionSegment targetSegment;
		private final int incrementAmount;
		
		public VersionIncrementRule(VersionSegment targetSegment, int incrementAmount) {
			if (incrementAmount <= 0) {
				throw new IllegalArgumentException("Increment amount must be positive: " + incrementAmount);
			}
			this.targetSegment = targetSegment;
			this.incrementAmount = incrementAmount;
		}
		
		public VersionSegment getTargetSegment() {
			return targetSegment;
		}
		
		public int getIncrementAmount() {
			return incrementAmount;
		}
		
		@Override
		public String toString() {
			return targetSegment.name().toLowerCase() + "+" + incrementAmount;
		}
	}
	
	private VersionIncrementRule majorVersionIncrement;
	private VersionIncrementRule minorVersionIncrement;
	private VersionIncrementRule microVersionIncrement;
	
	private ErrorMode majorVersionError;
	private ErrorMode minorVersionError;
	private ErrorMode microVersionError;
	
	/**
	 * Creates default settings with standard increment behavior
	 */
	public ApiConfigSettings() {
		// Default: increment same segment by 1
		this.majorVersionIncrement = new VersionIncrementRule(VersionSegment.MAJOR, 1);
		this.minorVersionIncrement = new VersionIncrementRule(VersionSegment.MINOR, 1);
		this.microVersionIncrement = new VersionIncrementRule(VersionSegment.MICRO, 1);
		
		this.majorVersionError = ErrorMode.ERROR;
		this.minorVersionError = ErrorMode.ERROR;
		this.microVersionError = ErrorMode.ERROR;
	}
	
	public VersionIncrementRule getMajorVersionIncrement() {
		return majorVersionIncrement;
	}
	
	public void setMajorVersionIncrement(VersionIncrementRule rule) {
		this.majorVersionIncrement = rule;
	}
	
	public VersionIncrementRule getMinorVersionIncrement() {
		return minorVersionIncrement;
	}
	
	public void setMinorVersionIncrement(VersionIncrementRule rule) {
		this.minorVersionIncrement = rule;
	}
	
	public VersionIncrementRule getMicroVersionIncrement() {
		return microVersionIncrement;
	}
	
	public void setMicroVersionIncrement(VersionIncrementRule rule) {
		this.microVersionIncrement = rule;
	}
	
	public ErrorMode getMajorVersionError() {
		return majorVersionError;
	}
	
	public void setMajorVersionError(ErrorMode mode) {
		this.majorVersionError = mode;
	}
	
	public ErrorMode getMinorVersionError() {
		return minorVersionError;
	}
	
	public void setMinorVersionError(ErrorMode mode) {
		this.minorVersionError = mode;
	}
	
	public ErrorMode getMicroVersionError() {
		return microVersionError;
	}
	
	public void setMicroVersionError(ErrorMode mode) {
		this.microVersionError = mode;
	}
}
