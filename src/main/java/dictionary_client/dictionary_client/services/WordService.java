package dictionary_client.dictionary_client.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dictionary_client.dictionary_client.dto.WordDTO;
import dictionary_client.dictionary_client.models.Translation;
import dictionary_client.dictionary_client.models.Word;
import dictionary_client.dictionary_client.repositories.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WordService {
    @Value("${progressAtATime}")
    int progressAtATimme;

   private final WordRepository wordRepository;
   private final TranslationService translationService;

    @Autowired
    public WordService(WordRepository wordRepository, TranslationService translationService) {
        this.wordRepository = wordRepository;
        this.translationService = translationService;
    }

    public Optional<Word>findById(int id) {
        return wordRepository.findById(id);
    }


    public List<Word> findAllWords(){
     //   System.out.println(wordRepository.findAll());
        return wordRepository.findAll();
    }

    public List<String> findAllOriginal(){

      //  System.out.println("find all originals started");
        List<Word> words = findAllWords();
        List<String> originalWords = new ArrayList<>();
        for (Word word : words) {
            originalWords.add(word.getName());
        }
        return originalWords;
    }


    public List<String> getTranslationOneWord(Word word){
        List<String> list = new ArrayList<>();
        for(Translation translation:word.getTranslationList()){
            list.add(translation.getName());
        }
        return list;
    }



    public Optional<Word> getWordById(int id){
        return wordRepository.findById(id);
    }




    @Transactional()
    public void save(Word word){
        word.setRegistrationDate(new Date());
        wordRepository.save(word);
    }

    @Transactional
    public void edit(Word word, int id){
         word.setId(id);
        wordRepository.save(word);
    }


    public boolean listContainWord(String word){
        return findAllOriginal().contains(word);
    }

    public boolean addWord(String original, List<String> translationsString){

        if(listContainWord(original)){

            return false;
        }
        else {
            Word word = new Word();
            word.setName(original);
            List<Translation> translations = new ArrayList<>();
            word.setProgress(0);
            save(word);
            for (String translation : translationsString){
                Translation translationObj = new Translation();
                translationObj.setName(translation);
                translationObj.setWord(word);
                translations.add(translationObj);
                translationObj.setRegistrationDate(new Date());
                translationService.save(translationObj);
            }
            word.setTranslationList(translations);
            save(word);

            return true;
        }
    }


    @Transactional
    public void changeName(String name, Word word){
        word.setName(name);
        wordRepository.save(word);
    }

    @Transactional
    public void deleteWord(int id){
        wordRepository.deleteById(id);
    }

    @Transactional
    public void deleteOneTranslation(int wordId, String translationName){
        Word word = wordRepository.findById(wordId).get();
        Translation translation =translationService.findTranslationByName(word.getTranslationList(), translationName).get();
        translationService.deleteTranslation(translation.getId());

    }

    @Transactional
    public void addOneTranslation(String name, String translationName){
        Word word = wordRepository.findWordsByName(name).get();
        translationService.addTranslation(word, translationName);
    }



    @Transactional
    public void increaseProgress(Word word){
        word.setProgress(word.getProgress() + progressAtATimme);
        wordRepository.save(word);
    }

    @Transactional
    public void decreaseProgress(Word word){
        word.setProgress(word.getProgress() - progressAtATimme);
        wordRepository.save(word);
    }

    public String getResponseFromServer(String wordName){
        RestTemplate template = new RestTemplate();
        String url = "http://localhost:9090/api/giveWord";
        String response;
        try {
            response = template.postForObject(url, wordName, String.class);
        }
        catch (HttpClientErrorException e) {
            System.out.println("Слово не было найдено");
            return null;
        }
        catch (ResourceAccessException e){
            System.out.println("Не удалось подключиться к серверу");
            return null;
        }
        return response;
    }
    public WordDTO getWordFromServer(String wordName) throws JsonProcessingException {
        String response = getResponseFromServer(wordName);
        System.out.println("response: " + response);
        if (response.equals("")||response==null){
            return null;
        }
       // String response = template.postForObject(url, wordName, String.class);
        System.out.println("response " + response);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode obj = mapper.readTree(response);
        String nameFromResponse = obj.get("name").asText();
        JsonNode listNode = obj.get("translations");
        List<String> stringList = new ArrayList<>();
        for (JsonNode node : listNode){
            stringList.add(node.asText());
        }

       // List<String> stringList = obj.get("translations").asLi;
        System.out.println("nameFromResponse: " + nameFromResponse);
        System.out.println("stringList: " + stringList);
        WordDTO wordDTO = new WordDTO();
        wordDTO.setName(nameFromResponse);
        wordDTO.setTranslations(stringList);
        return wordDTO;
    }

    public void saveWordFromServer(WordDTO wordDTO, boolean needSave){

        if (needSave==false||wordDTO==null)
        {
            return;
        }
        addWord(wordDTO.getName(), wordDTO.getTranslations());
    }









}
