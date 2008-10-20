/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Ant task to run the API freeze check during Eclipse build.
 */
public class APIFreezeTask extends CommonUtilsTask {
	
	public static class APIFreezeDeltaVisitor extends DeltaXmlVisitor {
		private String excludeListLocation;
		private Set excludedElement;
		private List nonExcludedElements;

		public APIFreezeDeltaVisitor(String excludeListLocation) throws CoreException {
			super();
			this.excludeListLocation = excludeListLocation;
			this.nonExcludedElements = new ArrayList();
		}
		protected void processLeafDelta(IDelta delta) {
			if (DeltaProcessor.isCompatible(delta)) {
				switch(delta.getKind()) {
					case IDelta.ADDED :
						int modifiers = delta.getModifiers();
						if (Util.isPublic(modifiers)) {
							// if public, we always want to check @since tags
							switch(delta.getFlags()) {
								case IDelta.TYPE_MEMBER :
								case IDelta.METHOD :
								case IDelta.CONSTRUCTOR :
								case IDelta.ENUM_CONSTANT :
								case IDelta.METHOD_WITH_DEFAULT_VALUE :
								case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
								case IDelta.FIELD :
								case IDelta.TYPE :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
									break;
							}
						} else if (Util.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
							// if protected, we only want to check @since tags if the enclosing class can be subclassed
							switch(delta.getFlags()) {
								case IDelta.TYPE_MEMBER :
								case IDelta.METHOD :
								case IDelta.CONSTRUCTOR :
								case IDelta.ENUM_CONSTANT :
								case IDelta.FIELD :
								case IDelta.TYPE :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
									break;
							}
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.MAJOR_VERSION :
							case IDelta.MINOR_VERSION :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
						}
				}
			} else {
				if (delta.getKind() == IDelta.ADDED) {
					// if public, we always want to check @since tags
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
							// ensure that there is a @since tag for the corresponding member
							if (delta.getKind() == IDelta.ADDED && Util.isVisible(delta)) {
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
							}
					}
				}
			}
		}
		private boolean checkExclude(IDelta delta) {
			if (this.excludedElement == null) {
				initializeExcludedElement();
			}
			return isExcluded(delta);
		}
		private boolean isExcluded(IDelta delta) {
			String typeName = delta.getTypeName();
			StringBuffer buffer = new StringBuffer();
			String componentId = delta.getApiComponentID();
			if (componentId != null) {
				buffer.append(componentId).append(':');
			}
			if (typeName != null) {
				buffer.append(typeName);
			}
			int flags = delta.getFlags();
			switch(flags) {
				case IDelta.TYPE_MEMBER :
					buffer.append('.').append(delta.getKey());
					break;
				case IDelta.METHOD :
				case IDelta.CONSTRUCTOR :
				case IDelta.ENUM_CONSTANT :
				case IDelta.METHOD_WITH_DEFAULT_VALUE :
				case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
				case IDelta.FIELD :
					buffer.append('#').append(delta.getKey());
					break;
				case IDelta.MAJOR_VERSION :
				case IDelta.MINOR_VERSION :
					buffer
						.append(Util.getDeltaFlagsName(flags))
						.append('_')
						.append(Util.getDeltaKindName(delta.getKind()));
					break;
			}
			String excludeListKey = String.valueOf(buffer);
			if (this.excludedElement.contains(excludeListKey)) {
				return true;
			}
			this.nonExcludedElements.add(excludeListKey);
			return false;
		}
		private void initializeExcludedElement() {
			this.excludedElement = new HashSet();
			if (this.excludeListLocation == null) return;
			File file = new File(this.excludeListLocation);
			if (!file.exists()) return;
			InputStream stream = null;
			char[] contents = null;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
				contents = Util.getInputStreamAsCharArray(stream, -1, "ISO-8859-1"); //$NON-NLS-1$
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			if (contents == null) return;
			LineNumberReader reader = new LineNumberReader(new StringReader(new String(contents)));
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#")) continue; //$NON-NLS-1$
					this.excludedElement.add(line);
				}
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		
		public String getPotentialExcludeList() {
			if (this.nonExcludedElements == null) return Util.EMPTY_STRING;
			Collections.sort(this.nonExcludedElements);
			StringWriter stringWriter = new StringWriter();
			PrintWriter writer = new PrintWriter(stringWriter);
			for (Iterator iterator = this.nonExcludedElements.iterator(); iterator.hasNext(); ) {
				writer.println(iterator.next());
			}
			writer.close();
			return String.valueOf(stringWriter.getBuffer());
		}
	}

	private static final String REFERENCE = "reference"; //$NON-NLS-1$
	private static final String CURRENT = "currentProfile"; //$NON-NLS-1$
	private static final String REFERENCE_PROFILE_NAME = "reference_profile"; //$NON-NLS-1$
	private static final String CURRENT_PROFILE_NAME = "current_profile"; //$NON-NLS-1$

	private boolean debug;

	private String referenceLocation;
	private String profileLocation;
	private String reportLocation;
	private String eeFileLocation;
	private String excludeListLocation;

	public void setProfile(String profileLocation) {
		this.profileLocation = profileLocation;
	}
	public void setReference(String referenceLocation) {
		this.referenceLocation = referenceLocation;
	}
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("reference : " + this.referenceLocation); //$NON-NLS-1$
			System.out.println("profile to compare : " + this.profileLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
			if (this.excludeListLocation != null) {
				System.out.println("exclude list location : " + this.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
		}
		if (this.referenceLocation == null
				|| this.profileLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println("Missing arguments :"); //$NON-NLS-1$
			writer.print("reference location :"); //$NON-NLS-1$
			writer.println(this.referenceLocation);
			writer.print("current profile location :"); //$NON-NLS-1$
			writer.println(this.profileLocation);
			writer.print("report location :"); //$NON-NLS-1$
			writer.println(this.reportLocation);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		// unzip reference
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
		}
		File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		
		File referenceInstallDir = new File(tempDir, REFERENCE);
		extractSDK(referenceInstallDir, this.referenceLocation);

		File profileInstallDir = new File(tempDir, CURRENT);
		extractSDK(profileInstallDir, this.profileLocation);
		if (this.debug) {
			System.out.println("Extraction of both archives : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		// run the comparison
		// create profile for the reference
		IApiBaseline referenceProfile = createProfile(REFERENCE_PROFILE_NAME, getInstallDir(tempDir, REFERENCE), this.eeFileLocation);
		IApiBaseline currentProfile = createProfile(CURRENT_PROFILE_NAME, getInstallDir(tempDir, CURRENT), this.eeFileLocation);
		
		IDelta delta = null;
		if (this.debug) {
			System.out.println("Creation of both profiles : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		try {
			delta = ApiComparator.compare(referenceProfile, currentProfile, VisibilityModifiers.API, true);
		} finally {
			if (this.debug) {
				System.out.println("API freeze check : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
			referenceProfile.dispose();
			currentProfile.dispose();
			Util.delete(referenceInstallDir);
			Util.delete(profileInstallDir);
			if (this.debug) {
				System.out.println("Cleanup : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
		}
		if (delta == null) {
			// an error occured during the comparison
			throw new BuildException("An error occured during the comparison"); //$NON-NLS-1$
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			BufferedWriter writer = null;
			File outputFile = new File(this.reportLocation);
			if (outputFile.exists()) {
				// delete the file
				// TODO we might want to customize it
				outputFile.delete();
			} else {
				File outputDir = outputFile.getParentFile();
				if (!outputDir.exists()) {
					if (!outputDir.mkdirs()) {
						throw new BuildException("An error occured creating the parent of the report file : " + this.reportLocation); //$NON-NLS-1$
					}
				}
			}
			try {
				writer = new BufferedWriter(new FileWriter(outputFile));
				APIFreezeDeltaVisitor visitor = new APIFreezeDeltaVisitor(this.excludeListLocation);
				delta.accept(visitor);
				writer.write(visitor.getXML());
				writer.flush();
				if (this.debug) {
					String potentialExcludeList = visitor.getPotentialExcludeList();
					if (potentialExcludeList.length() != 0) {
						System.out.println("Potential exclude list:"); //$NON-NLS-1$
						System.out.println(potentialExcludeList);
					}
				}
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
			if (this.debug) {
				System.out.println("Report generation : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
