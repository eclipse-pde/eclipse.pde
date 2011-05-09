/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;

/** 
 * Internal task.
 * This task aims at replacing the generic ids used into a feature.xml by another value, and also replace the feature version number if necessary.  
 * @since 3.0
 */
public class LicenseReplaceTask extends Task {
	// Path of the file where we are replacing the values
	private String filePath;

	// Path to license text
	private String licensePath;

	private class Feature {
		private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
		private static final String FEATURE_START_TAG = "<feature";//$NON-NLS-1$
		private static final String LICENSE_START_TAG = "<license"; //$NON-NLS-1$;
		private static final String LICENSE_END_TAG = "</license>"; //$NON-NLS-1$;
		private static final String URL_ATTR = "url";//$NON-NLS-1$
		private static final String DOUBLE_QUOTE = "\""; //$NON-NLS-1$
		private static final String COMMENT_START_TAG = "<!--"; //$NON-NLS-1$
		private static final String COMMENT_END_TAG = "-->"; //$NON-NLS-1$

		private final String featureFilePath;
		private String urlText;
		private String license;
		private StringBuffer buffer;
		private int startLicenseText = -1;
		private int endLicenseText = -1;
		private int startURLText = -1;
		private int endURLText = -1;
		private int startURLWord = -1;
		private int endURLWord = -1;
		private int insertionPoint = -1;
		private boolean contentChanged;

		public String getUrl() {
			if (contentChanged) {
				throw new IllegalStateException(TaskMessages.error_noCallAfterReplace);
			}
			return urlText;
		}

		public String getLicenseText() {
			if (contentChanged) {
				throw new IllegalStateException(TaskMessages.error_noCallAfterReplace);
			}
			return license;
		}

		public void replace(String licenseURL, String licenseText) {
			if (contentChanged) {
				throw new IllegalStateException(TaskMessages.error_noCallAfterReplace);
			}

			if (startLicenseText > 0 && endLicenseText > startLicenseText) {
				// Replace license text
				buffer.replace(startLicenseText, endLicenseText, licenseText);
				contentChanged = true;
			} else if (insertionPoint > -1) {
				//insert new license after <feature>
				StringBuffer newLicense = new StringBuffer();
				newLicense.append('\n');
				newLicense.append(LICENSE_START_TAG + " " + URL_ATTR + "="); //$NON-NLS-1$//$NON-NLS-2$
				if (licenseURL != null)
					newLicense.append(licenseURL);
				newLicense.append(" >"); //$NON-NLS-1$
				newLicense.append(licenseText);
				newLicense.append(LICENSE_END_TAG);

				buffer.insert(insertionPoint, newLicense.toString());
				contentChanged = true;
				return;
			} else {
				return;
			}

			if (startURLText == endURLText) {
				// Replace empty payload URL
				if (licenseURL == null) {
					// with empty license URL
					// No-op
				} else {
					// with non-empty license URL
					buffer.replace(startURLText, endURLText + 1, " url=" + licenseURL + ">"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else {
				// Replace non-empty payload URL
				if (licenseURL == null) {
					// with empty license URL
					buffer.replace(startURLWord, endURLText + 1, ""); //$NON-NLS-1$
				} else {
					// with non-empty license URL
					buffer.replace(startURLText, endURLText + 1, licenseURL);
				}
			}

			int start = buffer.indexOf("license-feature="); //$NON-NLS-1$
			if (start != -1) {
				int end = buffer.indexOf("\"", start); //$NON-NLS-1$
				if (end < buffer.length()) {
					end = buffer.indexOf("\"", end + 1); //$NON-NLS-1$
					if (end != -1) {
						buffer.replace(start, end + 1, ""); //$NON-NLS-1$
					}
				}
			}

			start = buffer.indexOf("license-feature-version="); //$NON-NLS-1$
			if (start != -1) {
				int end = buffer.indexOf("\"", start); //$NON-NLS-1$
				if (end < buffer.length()) {
					end = buffer.indexOf("\"", end + 1); //$NON-NLS-1$
					if (end != -1) {
						buffer.replace(start, end + 1, ""); //$NON-NLS-1$
					}
				}
			}
		}

		public void write() {
			if (!contentChanged)
				return;

			try {
				OutputStreamWriter w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(featureFilePath)), UTF_8);
				w.write(buffer.toString());
				w.close();
			} catch (FileNotFoundException e) {
				// ignore
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}

		Feature(String featureFilePath) {
			super();
			this.featureFilePath = featureFilePath + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR;
			parse();
		}

		private void parse() {
			buffer = null;
			try {
				buffer = readFile(new File(featureFilePath));
			} catch (IOException e) {
				throw new BuildException(e);
			}

			int startFeature = scanNoComment(buffer, 0, FEATURE_START_TAG, true);
			if (startFeature == -1)
				return;

			int endFeature = scan(buffer, startFeature, ">"); //$NON-NLS-1$
			insertionPoint = endFeature + 1;

			int startLicense = scanNoComment(buffer, 0, LICENSE_START_TAG, false);
			if (startLicense == -1)
				return;

			int endLicense = scan(buffer, startLicense, ">"); //$NON-NLS-1$

			boolean urlFound = false;
			while (!urlFound) {
				startURLWord = scan(buffer, startLicense, URL_ATTR);
				if (startURLWord == -1 || startURLWord > endLicense) {
					startURLText = startLicense + LICENSE_START_TAG.length();
					endURLText = startURLText;
				} else {

					if (!Character.isWhitespace(buffer.charAt(startURLWord - 1))) {
						startLicense = startURLWord + URL_ATTR.length();
						continue;
					}

					//Verify that the word url found is the actual attribute
					endURLWord = startURLWord + URL_ATTR.length();
					while (Character.isWhitespace(buffer.charAt(endURLWord)) && endURLWord < endLicense) {
						endURLWord++;
					}
					if (endURLWord > endLicense) { //id has not been found
						System.err.println("Could not find the tag 'id' in the license header, file: " + featureFilePath); //$NON-NLS-1$
						return;
					}

					if (buffer.charAt(endURLWord) != '=') {
						startLicense = endURLWord;
						continue;
					}
					startURLText = scan(buffer, startURLWord + 1, DOUBLE_QUOTE);
					endURLText = scan(buffer, startURLText + 1, DOUBLE_QUOTE);
					urlText = (buffer.substring(startURLText, endURLText + 1));
				}
				urlFound = true;
			}
			startLicenseText = scan(buffer, endURLText, ">") + 1; //$NON-NLS-1$
			endLicenseText = scan(buffer, startLicenseText, LICENSE_END_TAG, true) - 1;
			license = buffer.substring(startLicenseText, endLicenseText);
		}

		private StringBuffer readFile(File targetName) throws IOException {
			InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(targetName)), UTF_8);
			StringBuffer result = new StringBuffer();
			char[] buf = new char[4096];
			int count;
			try {
				count = reader.read(buf, 0, buf.length);
				while (count != -1) {
					result.append(buf, 0, count);
					count = reader.read(buf, 0, buf.length);
				}
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore exceptions here
				}
			}
			return result;
		}

		private int scan(StringBuffer buf, int start, String targetName) {
			return scan(buf, start, new String[] {targetName}, false);
		}

		private int scan(StringBuffer buf, int start, String targetName, boolean wholeWord) {
			return scan(buf, start, new String[] {targetName}, wholeWord);
		}

		private int scan(StringBuffer buf, int start, String[] targets, boolean wholeWord) {
			for (int i = start; i < buf.length(); i++) {
				for (int j = 0; j < targets.length; j++) {
					if (i < buf.length() - targets[j].length()) {
						String candidate = targets[j];
						String match = buf.substring(i, i + candidate.length());
						if (candidate.equalsIgnoreCase(match)) {
							if (!wholeWord || Character.isWhitespace(buf.charAt(i + candidate.length())))
								return i;
						}
					}
				}
			}
			return -1;
		}

		private int scanNoComment(StringBuffer bug, int start, String target, boolean wholeWord) {
			int startComment = scan(buffer, start, COMMENT_START_TAG);
			int endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
			int startTarget = scan(buffer, start, target, wholeWord);

			while (startComment != -1 && startTarget > startComment && startTarget < endComment) {
				startTarget = scan(buffer, endComment, target, wholeWord);
				startComment = scan(buffer, endComment, COMMENT_START_TAG);
				endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
			}
			return startTarget;
		}
	}

	/**
	 * The directory containing the feature 
	 * @param path
	 */
	public void setFeatureFilePath(String path) {
		filePath = path;
	}

	/**
	 * The directory containing the license feature
	 * @param path
	 */
	public void setLicenseFilePath(String path) {
		licensePath = path;
	}

	public void execute() {
		Feature payloadFeature = new Feature(filePath);
		Feature licenseFeature = new Feature(licensePath);

		// Replace license information in target feature.xml
		payloadFeature.replace(licenseFeature.getUrl(), licenseFeature.getLicenseText());
		payloadFeature.write();

		// Append license feature_*.properties files to target feature_*.properties files
		FileSet fileSet = new FileSet();
		fileSet.setProject(getProject());
		fileSet.setDir(new File(licensePath));
		NameEntry fileInclude = fileSet.createInclude();
		fileInclude.setName("feature*.properties"); //$NON-NLS-1$

		String[] propertyFiles = fileSet.getDirectoryScanner().getIncludedFiles();

		for (int i = 0; i < propertyFiles.length; i++) {
			String propertyFile = propertyFiles[i];

			File featurePropertyFile = new File(filePath, propertyFile);
			File licensePropertyFile = new File(licensePath, propertyFile);
			FileInputStream fis = null;
			if (featurePropertyFile.exists()) {
				try {
					fis = new FileInputStream(licensePropertyFile);
					Properties licenseProperties = new Properties();
					licenseProperties.load(fis);
					fis.close();

					fis = new FileInputStream(featurePropertyFile);
					Properties featureProperties = new Properties();
					featureProperties.load(fis);
					fis.close();

					Enumeration licenseKeys = licenseProperties.keys();
					while (licenseKeys.hasMoreElements()) {
						String licenseKey = (String) licenseKeys.nextElement();
						if (featureProperties.containsKey(licenseKey)) {
							throw new BuildException(NLS.bind(TaskMessages.error_conflictingProperties, new String[] {licenseKey, licensePropertyFile.getAbsolutePath(), featurePropertyFile.getAbsolutePath()}));
						}
					}
				} catch (FileNotFoundException e) {
					// DO Nothing
				} catch (IOException e) {
					throw new BuildException(e);
				}
			}

			// Now append (or create) necessary feature_*.properties files

			try {
				FileWriter featurePropertyWriter = new FileWriter(featurePropertyFile, true);
				FileReader licensePropertyReader = new FileReader(licensePropertyFile);

				char[] buffer = new char[1024];
				int bytesRead = licensePropertyReader.read(buffer);
				while (bytesRead > -1) {
					featurePropertyWriter.write(buffer, 0, bytesRead);
					bytesRead = licensePropertyReader.read(buffer);
				}

				featurePropertyWriter.close();
				licensePropertyReader.close();
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}
	}
}
