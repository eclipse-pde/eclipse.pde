/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;

/**
 * Java application to compare two API profiles
 */
public class Compare {
	private static final String BASELINE = "-baseline"; //$NON-NLS-1$
	private static final String PROFILE = "-profile"; //$NON-NLS-1$
	private static final String OUTPUT = "-output"; //$NON-NLS-1$
	private static final String OPTIONS = "-options"; //$NON-NLS-1$
	
	private String baseline;
	private String output;
	private String profile;

	/**
	 * Supported options:
	 * <ul>
	 * <li>-baseline: path to a file that specifies the API profile baseline</li>
	 * <li>-profile: path to a file that specifies the API profile to compare with the baseline profile</li>
	 * <li>-output: path to a folder in which the result files are generated</li>
	 * <li>-options: path to a property file that defines the available options</li>
	 * </ul>
	 * @param args
	 */
	public static void main(String[] args) {
		Compare compare = new Compare();
		try {
			compare.configure(args);
			if (compare.isVerbose()) {
				long time = System.currentTimeMillis();
				compare.process();
				System.out.println("" + (System.currentTimeMillis() - time) + "ms spent"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				compare.process();
			}
		} catch (IllegalArgumentException e) {
			// ignore: correspond to a wrong option
		}
	}
	
	private void configure(String[] args) {
		// command line processing
		/*
		 * Recognized options:
		 * -baseline
		 * -output
		 * -profile
		 * -options
		 */
		final int OPTION_DEFAULT = 0;
		final int OPTION_BASELINE  = 1;
		final int OPTION_PROFILE = 2;
		final int OPTION_OPTIONS = 3;
		final int OPTION_OUTPUT = 4;
		int mode = OPTION_DEFAULT;
		for (int i = 0, max = args.length; i < max; i++) {
			String currentArg = args[i];
			switch (mode) {
				case OPTION_DEFAULT:
					if (BASELINE.equals(currentArg)) {
						mode = OPTION_BASELINE;
						continue;
					}
					if (OUTPUT.equals(currentArg)) {
						mode = OPTION_OUTPUT;
						continue;
					}
					if (PROFILE.equals(currentArg)) {
						mode = OPTION_PROFILE;
						continue;
					}
					if (OPTIONS.equals(currentArg)) {
						mode = OPTION_OPTIONS;
						continue;
					}
					System.err.println("Unknown option : " + currentArg); //$NON-NLS-1$
					break;
				case OPTION_BASELINE:
					if (this.baseline != null) {
						throw new IllegalArgumentException("Cannot set the baseline value more than once"); //$NON-NLS-1$
					}
					this.baseline = currentArg;
					mode = OPTION_DEFAULT;
					break;
				case OPTION_PROFILE:
					if (this.profile != null) {
						throw new IllegalArgumentException("Cannot set the profile value more than once"); //$NON-NLS-1$
					}
					this.profile = currentArg;
					mode = OPTION_DEFAULT;
					break;
				case OPTION_OPTIONS :
					// TODO need to be customized
					break;
				case OPTION_OUTPUT :
					if (this.output != null) {
						throw new IllegalArgumentException("Cannot set the output value more than once"); //$NON-NLS-1$
					}
					this.output = currentArg;
					break;
			}
		}
		if (this.baseline == null || this.profile == null || this.output == null) {
			printUsage();
			throw new IllegalArgumentException("Missing arguments"); //$NON-NLS-1$
		}
	}

	private void printUsage() {
		System.out.println("Usage: Compare -baseline <path> -profile <path> -output <path to xml file>"); //$NON-NLS-1$
	}
	private boolean isVerbose() {
		// this should be customized using options
		return true;
	}

	private void process() {
		// processing the comparison
		BufferedInputStream inputStream = null;
		IApiProfile baseline = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(this.baseline));
			baseline = ApiProfileManager.restoreProfile(inputStream);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} finally {
			try {
				if (inputStream!= null) {
					inputStream.close();
				}
			} catch(IOException e) {
				// ignore
			}
		}
		inputStream = null;
		IApiProfile profile = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(this.profile));
			profile = ApiProfileManager.restoreProfile(inputStream);
		} catch (FileNotFoundException e) {
			ApiPlugin.log(e);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} finally {
			try {
				if (inputStream!= null) {
					inputStream.close();
				}
			} catch(IOException e) {
				// ignore
			}
		}
		File outputFile = new File(this.output);
		if (!outputFile.exists()) {
			File parentFile = outputFile.getParentFile();
			if (parentFile != null) {
				if (!parentFile.mkdirs()) {
					System.err.println("Could not create the output folder for : " + this.output); //$NON-NLS-1$
					return;
				}
			} else {
				System.err.println("Could not retrieve the parent of the output file : " + this.output); //$NON-NLS-1$
				return;
			}
		}
		if (baseline == null) {
			System.err.println("Could not setup the baseline profile : " + this.baseline); //$NON-NLS-1$
			return;
		}
		if (profile == null) {
			System.err.println("Could not setup the profile to compare with the baseline profile : " + this.profile); //$NON-NLS-1$
			return;
		}
		IDelta delta = ApiComparator.compare(baseline, profile, VisibilityModifiers.API);
		if (delta == null) {
			// an error occured during the comparison
			System.err.println("An error occured during the comparison"); //$NON-NLS-1$
			return;
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the resulting XML into the output folder
			BufferedWriter writer = null;
			try {
				if (outputFile.exists()) {
					// delete the file
					// TODO we might want to customize it
					outputFile.delete();
				}
				writer = new BufferedWriter(new FileWriter(outputFile));
				DeltaXmlVisitor visitor = new DeltaXmlVisitor();
				delta.accept(visitor);
				writer.write(visitor.getXML());
				writer.flush();
			} catch (IOException e) {
				ApiPlugin.log(e);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
}
