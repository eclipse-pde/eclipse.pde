package $packageName$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.*;

@Component
public class DictionaryImpl implements Dictionary {

	private List<String> fWords = new ArrayList<>(Arrays.asList("$word1$", "$word2$", "$word3$"));
	private String fLanguage = "en_US";
	
	@Override
	public String getLanguage() {
		return fLanguage;
	}

	@Override
	public boolean check(String word) {
		return fWords.contains(word);
	}
	
	@Override
	public String toString() {
		return fLanguage;
	}

}
