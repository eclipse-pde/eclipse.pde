package $packageName$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DictionaryImpl implements Dictionary {

	private List fWords = new ArrayList(Arrays.asList("$word1$", "$word2$", "$word3$"));
	private String fLanguage = "en_US";
	
	public String getLanguage() {
		return fLanguage;
	}

	public boolean check(String word) {
		return fWords.contains(word);
	}
	
	public String toString() {
		return fLanguage;
	}

}
