/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.builders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;

public class NativeCodeAttributeValues {

	// ISO 639 CODES ALPHABETIC BY LANGUAGE NAME
	public final static int LANGUAGE_NAME= 0;
	public final static int LANGUAGE_CODE= 1;
	public final static String[][] LANGUAGES=
		{
			{"Abkhazian",       "ab"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Afan (Oromo)",    "om"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Afar",            "aa"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Afrikaans",       "af"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Albanian",        "sq"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Amharic",         "am"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Arabic",          "ar"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Armenian",        "hy"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Assamese",        "as"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Aymara",          "ay"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Azerbaijani",     "az"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Bashkir",         "ba"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Basque",          "eu"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Bengali;Bangla",  "bn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Bhutani",         "dz"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Bihari",          "bh"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Bislama",         "bi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Breton",          "br"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Bulgarian",       "bg"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Burmese",         "my"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Byelorussian",    "be"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Cambodian",       "km"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Catalan",         "ca"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Chinese",         "zh"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Corsican",        "co"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Croatian",        "hr"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Czech",           "cs"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Danish",          "da"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Dutch",           "nl"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"English",         "en"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Esperanto",       "eo"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Estonian",        "et"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Faroese",         "fo"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Fiji",            "fj"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Finnish",         "fi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"French",          "fr"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Frisian",         "fy"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Galician",        "gl"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Georgian",        "ka"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"German",          "de"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Greek",           "el"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Greenlandic",     "kl"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Guarani",         "gn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Gujarati",        "gu"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Hausa",           "ha"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Hebrew",          "he"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Hindi",           "hi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Hungarian",       "hu"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Icelandic",       "is"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Indonesian",      "id"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Interlingua",     "ia"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Interlingue",     "ie"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Inuktitut",       "iu"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Inupiak",         "ik"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Irish",           "ga"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Italian",         "it"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Japanese",        "ja"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Javanese",        "jv"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Kannada",         "kn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kashmiri",        "ks"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kazakh",          "kk"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kinyarwanda",     "rw"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kirghiz",         "ky"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kurundi",         "rn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Korean",          "ko"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Kurdish",         "ku"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Laothian",        "lo"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Latin",           "la"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Latvian;Lettish", "lv"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Lingala",         "ln"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Lithuanian",      "lt"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Macedonian",      "mk"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Malagasy",        "mg"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Malay",           "ms"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Malayalam",       "ml"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Maltese",         "mt"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Maori",           "mi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Marathi",         "mr"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Moldavian",       "mo"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Mongolian",       "mn"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Nauru",           "na"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Nepali",          "ne"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Norwegian",       "no"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Occitan",         "oc"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Oriya",           "or"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Pashto;Pushto",   "ps"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Persian (Farsi)", "fa"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Polish",          "pl"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Portuguese",      "pt"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Punjabi",         "pa"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Quechua",         "qu"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Rhaeto-Romance",  "rm"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Romanian",        "ro"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Russian",         "ru"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Samoan",          "sm"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Sangho",          "sg"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Sanskrit",        "sa"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Scots Gaelic",    "gd"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Serbian",         "sr"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Serbo-Croatian",  "sh"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Sesotho",         "st"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Setswana",        "tn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Shona",           "sn"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Sindhi",          "sd"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Singhalese",      "si"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Siswati",         "ss"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Slovak",          "sk"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Slovenian",       "sl"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Somali",          "so"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Spanish",         "es"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Sundanese",       "su"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Swahili",         "sw"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Swedish",         "sv"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Tagalog",         "tl"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tajik",           "tg"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tamil",           "ta"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tatar",           "tt"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Telugu",          "te"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Thai",            "th"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tibetan",         "bo"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tigrinya",        "ti"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tonga",           "to"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Tsonga",          "ts"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Turkish",         "tr"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Turkmen",         "tk"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Twi",             "tw"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Uigur",           "ug"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Ukrainian",       "uk"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Urdu",            "ur"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Uzbek",           "uz"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Vietnamese",      "vi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Volapuk",         "vo"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Welsh",           "cy"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Wolof",           "wo"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Xhosa",           "xh"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Yiddish",         "yi"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Yoruba",          "yo"},  //$NON-NLS-1$ //$NON-NLS-2$
			
			{"Zhuang",          "za"},  //$NON-NLS-1$ //$NON-NLS-2$
			{"Zulu",            "zu"},  //$NON-NLS-1$ //$NON-NLS-2$
		};
	
	public final static String[] EXCLUDE_FILES= new String[]
		{ ".classpath",  //$NON-NLS-1$
		  ".project", //$NON-NLS-1$
		  "plugin.xml", //$NON-NLS-1$
		  "fragment.xml", //$NON-NLS-1$
		  "build.properties", //$NON-NLS-1$
		  "META-INF/MANIFEST.MF" }; //$NON-NLS-1$
	
	private static final String OSNAME_ALIASES_LOCATION= "org/eclipse/osgi/framework/internal/core/osname.aliases"; //$NON-NLS-1$
	private static final String PROCESSOR_ALIASES_LOCATION= "org/eclipse/osgi/framework/internal/core/processor.aliases"; //$NON-NLS-1$
	
	// Tokenizer constants.
	private static final String SPACE= " "; //$NON-NLS-1$
	private static final String QUOTE= "\""; //$NON-NLS-1$
	private static final String COMMENT= "#"; //$NON-NLS-1$
	
	// OS & Processor Types 
	public static String[] OS_TYPES= new String [0];
	public static String[] ADDITIONAL_OS_ALIASES= new String [0];
	public static String[] PROCESSOR_TYPES= new String [0];
	public static String[] ADDITIONAL_PROCESSOR_ALIASES= new String [0];
	static {
		ZipFile smfJar= null;
		try {
			
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel("org.eclipse.osgi"); //$NON-NLS-1$
			if (model!=null)
			{
				BundleDescription desc = model.getBundleDescription();
				if (desc != null)
				{
					//find path to org.eclipse.osgi plugin
					String path = desc.getLocation();
					smfJar= new ZipFile(path+"\\core.jar");				 //$NON-NLS-1$
					if (smfJar!=null)
					{	
						initializeOSTypes(smfJar);
						initializeProcessorTypes(smfJar);
					}
				}
			}

		} catch (IOException ex) {
			
		} finally {
			try {
				if (smfJar != null)
					smfJar.close();
			} catch (IOException ex) {
			}
		}
	}
	
	static void initializeOSTypes(ZipFile smfJar) {
		try {
			ZipEntry aliasFile= smfJar.getEntry(OSNAME_ALIASES_LOCATION);
			InputStream is= smfJar.getInputStream(aliasFile);
			Properties properties= new Properties();
			properties.load(is);
			SortedSet osTypes= new TreeSet();
			List additionalOSAliases= new ArrayList();
			
			Iterator it= properties.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry= (Map.Entry) it.next();
				osTypes.add(entry.getKey());
				parseAliases((String) entry.getValue(), additionalOSAliases);
			}
			
			OS_TYPES= new String[osTypes.size()];
			osTypes.toArray(OS_TYPES);
			
			ADDITIONAL_OS_ALIASES= new String[additionalOSAliases.size()];
			additionalOSAliases.toArray(ADDITIONAL_OS_ALIASES);
			
		} catch (IOException ex) {
			PDECore.logException(ex);
		}
	}
	
	static void initializeProcessorTypes(ZipFile smfJar) {
		try {
			ZipEntry aliasFile= smfJar.getEntry(PROCESSOR_ALIASES_LOCATION);
			InputStream is= smfJar.getInputStream(aliasFile);
			Properties properties= new Properties();
			properties.load(is);
			SortedSet processorTypes= new TreeSet();
			List additionalProcessorAliases= new ArrayList();
			
			Iterator it= properties.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry= (Map.Entry) it.next();
				processorTypes.add(entry.getKey());
				parseAliases((String) entry.getValue(), additionalProcessorAliases);
			}
			
			PROCESSOR_TYPES= new String[processorTypes.size()];
			processorTypes.toArray(PROCESSOR_TYPES);
			
			ADDITIONAL_PROCESSOR_ALIASES= new String[additionalProcessorAliases.size()];
			additionalProcessorAliases.toArray(ADDITIONAL_PROCESSOR_ALIASES);
			
		} catch (IOException ex) {
			PDECore.logException(ex);
		}
	}
	
	static void parseAliases(String line, List aliases) {
		boolean inQuote= false;
		StringTokenizer outer= new StringTokenizer(line, QUOTE, true);
		while (outer.hasMoreTokens()) {
			String token= outer.nextToken();
			if (token.equals(QUOTE)) {
				inQuote= !inQuote;
			} else if (inQuote) {
				aliases.add(token);
			} else {
				StringTokenizer inner= new StringTokenizer(token, SPACE, false);
				while (inner.hasMoreTokens()) {
					String next= inner.nextToken();
					// Stop processing the line when a comment is detected.
					if (next.startsWith(COMMENT)) return;
					aliases.add(next);
				}
			}
		}
	}
	
}
