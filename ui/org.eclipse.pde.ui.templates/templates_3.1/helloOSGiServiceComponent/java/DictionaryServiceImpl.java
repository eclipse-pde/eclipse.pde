package $packageName$;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.*;

@Component
public class DictionaryServiceImpl implements DictionaryService {

	private List<Dictionary> fDictionaries = new ArrayList<>();
	
	@Reference
	@Override
    public void registerDictionary(Dictionary dictionary) {
    	fDictionaries.add(dictionary);
    }
	
	@Override
    public void unregisterDictionary(Dictionary dictionary) {
    	fDictionaries.remove(dictionary);
    }

	@Override
	public boolean check(String word) {
		for (int i = 0; i < fDictionaries.size(); i++ ) {
			Dictionary dictionary = fDictionaries.get(i);
			if(dictionary.check(word))
				return true;
		}
		return false;
	}
	
    public String[] getLanguages() {
    	List<String> languages = new ArrayList<>();
    	for (int i = 0; i < fDictionaries.size(); i++ ) {
			Dictionary dictionary = fDictionaries.get(i);
			languages.add(dictionary.getLanguage());
		}
    	return (String[]) languages.toArray(new String[fDictionaries.size()]);
    }

}
