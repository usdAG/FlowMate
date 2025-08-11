package db;

import burp.RegexMatcher;
import db.entities.InputParameter;
import db.entities.InputValue;
import db.entities.Session;
import db.entities.Url;
import gui.GettingStartedView;
import gui.container.RuleContainer;
import model.SessionViewModel;
import utils.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

// InputParameter Entities are being saved via the corresponding URL Entity where the parameter appeared
// Hence keeping track of the URL Entities is enough because they contain the Parameters
public class ParameterHandler {

    public ConcurrentHashMap<Integer, Url> urlStorage;
    public ConcurrentHashMap<Integer, InputParameter> inputParameterStorage;
    public ConcurrentHashMap<Integer, InputValue> inputValueStorage;
    public List<InputParameter> allInputParametersList;
    private boolean hasActiveSession = false;
    private String sessionName;

    public ParameterHandler() {
        this.urlStorage = new ConcurrentHashMap<>();
        this.inputParameterStorage = new ConcurrentHashMap<>();
        this.inputValueStorage = new ConcurrentHashMap<>();
        this.allInputParametersList = Collections.synchronizedList(new ArrayList<InputParameter>());
        loadUrls();
        loadParameters();
        loadParameterOccurrences();
    }

    public void loadUrls() {
        List<Url> urlEntityList = DBModel.loadAllUrlEntities().stream().toList();
        List<Integer> identifiers = urlEntityList.stream().map(Url::getIdentifier).toList();
        urlStorage.putAll(combineListsIntoUrlEntityMap(identifiers, urlEntityList));
    }

    public void loadParameters() {
       List<InputParameter> inputParameterEntityList = DBModel.loadAllParameters().stream().toList();
       List<Integer> identifiers = inputParameterEntityList.stream().map(InputParameter::getIdentifier).toList();
       inputParameterStorage.putAll(combineListsIntoParameterEntityMap(identifiers, inputParameterEntityList));
       allInputParametersList.addAll(inputParameterEntityList);
    }

    public void loadParameterOccurrences() {
        List<InputValue> inputValueEntityList = DBModel.loadAllParameterOccurrenceEntities().stream().toList();
        List<Integer> identifiers = inputValueEntityList.stream().map(InputValue::getIdentifier).toList();
        inputValueStorage.putAll(combineListsIntoParameterOccurrenceMap(identifiers, inputValueEntityList));
    }

    Map<Integer, InputParameter> combineListsIntoParameterEntityMap(List<Integer> keys, List<InputParameter> values) {
        if (keys.size() != values.size()) {
            IllegalArgumentException exception = new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
        Map<Integer, InputParameter> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    Map<Integer, Url> combineListsIntoUrlEntityMap(List<Integer> keys, List<Url> values) {
        if (keys.size() != values.size()) {
            IllegalArgumentException exception = new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
        Map<Integer, Url> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    Map<Integer, InputValue> combineListsIntoParameterOccurrenceMap(List<Integer> keys, List<InputValue> values) {
        if (keys.size() != values.size()) {
            IllegalArgumentException exception = new IllegalArgumentException("Cannot combine lists with dissimilar sizes");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
        Map<Integer, InputValue> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    /**
     * Creates and saves new UrlEntity with the corresponding InputParameter
     * to the DB
        @param parameterHelper InputParameter to be added
     */
    private List<Object> addParameterToDB(ParameterHelperClass parameterHelper, String messageHash) {

        List<Object> returnList = new ArrayList<>();

        String name = parameterHelper.getName();
        String type = parameterHelper.getType().getName();
        String domain = parameterHelper.getDomain();
        String url = parameterHelper.getUrlFound();

        InputParameter newInputParameterEntity = new InputParameter(name, type, domain);
        Url newUrlEntity = new Url(url);
        InputValue newInputValue = new InputValue(parameterHelper.getValue(), url, type, messageHash);
        if (this.hasActiveSession) {
            newInputValue = new InputValue(parameterHelper.getValue(), url, type, messageHash, this.sessionName);
            Session session = SessionViewModel.sessionTable.get(this.sessionName);
            if (session != null) {
                session.addInputValue(newInputValue);
                SessionViewModel.sessionTable.put(this.sessionName, session);
            }
        }
        RegexMatcher.excludeParameter(newInputParameterEntity);
        RegexMatcher.excludeInputValue(newInputValue);

        // Check if the URL Entity already exists in the DB
        // If not, add it to the list of known Urls and save the Url + InputParameter in the DB
        if (!urlExistsInDB(newUrlEntity)) {
            if (!newInputValue.getValue().isEmpty()) {
                newInputParameterEntity.addOccurrence(newInputValue);
                this.inputValueStorage.put(newInputValue.getIdentifier(), newInputValue);
            }
            newUrlEntity.addParameterFoundInUrl(newInputParameterEntity);
            this.urlStorage.put(newUrlEntity.getIdentifier(), newUrlEntity);
            inputParameterStorage.put(newInputParameterEntity.getIdentifier(), newInputParameterEntity);
            this.allInputParametersList.add(newInputParameterEntity);
            returnList.add(newInputParameterEntity);
            returnList.add(newUrlEntity);
        } else {
            // If the URL Entity already exists get the correct Url Entity where the InputParameter Entity is to be added
            Url relatedUrlEntity = getUrlEntityByParameterUrl(parameterHelper.getUrlFound());
            // Check if Relationship to InputParameter already exists, this is the case if the URLs get loaded at start
            if (!parameterExistsInUrlEntity(relatedUrlEntity, newInputParameterEntity)) {
                if(!occurrenceAlreadyExists(newInputValue, newInputParameterEntity) && !newInputValue.getValue().isEmpty()) {
                    newInputParameterEntity.addOccurrence(newInputValue);
                    this.inputValueStorage.put(newInputValue.getIdentifier(), newInputValue);
                }
                relatedUrlEntity.addParameterFoundInUrl(newInputParameterEntity);
                inputParameterStorage.put(newInputParameterEntity.getIdentifier(), newInputParameterEntity);
                this.allInputParametersList.add(newInputParameterEntity);
                returnList.add(newInputParameterEntity);
                returnList.add(relatedUrlEntity);
            } else {
                InputParameter existingEntity = getExistingParameter(newInputParameterEntity.getIdentifier());
                if (!occurrenceAlreadyExists(newInputValue, existingEntity) && !newInputValue.getValue().isEmpty()) {
                    existingEntity.addOccurrence(newInputValue);
                    this.inputValueStorage.put(newInputValue.getIdentifier(), newInputValue);
                    this.allInputParametersList.add(newInputParameterEntity);
                    returnList.add(existingEntity);
                }
            }
        }

        return returnList;
    }

    class AddParametersCallable implements Callable<List<Object>> {
        private ParameterHelperClass parameter;
        private String messageHash;

        AddParametersCallable(ParameterHelperClass parameter, String messageHash) {
            this.parameter = parameter;
            this.messageHash = messageHash;
        }

        @Override
        public List<Object> call() {
            return addParameterToDB(parameter, messageHash);
        }
    }

    public List<Object> addParametersThreaded(Collection<ParameterHelperClass> oldParameterCollection, String messageHash) {
        List<Future<List<Object>>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (ParameterHelperClass oldParameter : oldParameterCollection) {
            Future<List<Object>> future = executor.submit(new AddParametersCallable(oldParameter, messageHash));
            futures.add(future);
        }
        List<Object> results = new ArrayList<>();
        for (Future<List<Object>> future : futures) {
            try {
                results.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        GettingStartedView.numberOfParameters.setText(String.valueOf(inputParameterStorage.size()));
        GettingStartedView.numberOfParameterValues.setText(String.valueOf(inputValueStorage.size()));
        GettingStartedView.numberOfUrls.setText(String.valueOf(urlStorage.size()));

        return results;
    }

    public List<Object> addParameters(Collection<ParameterHelperClass> parameters, String messageHash) {
        List<Object> returnList = new ArrayList<>();
        for (ParameterHelperClass param : parameters) {
            returnList.addAll(addParameterToDB(param, messageHash));
        }

        GettingStartedView.numberOfParameters.setText(String.valueOf(inputParameterStorage.size()));
        GettingStartedView.numberOfParameterValues.setText(String.valueOf(inputValueStorage.size()));
        GettingStartedView.numberOfUrls.setText(String.valueOf(urlStorage.size()));

        return returnList;
    }

    private InputParameter getExistingParameter(int identifier) {
        return inputParameterStorage.get(identifier);
    }

    private boolean occurrenceAlreadyExists(InputValue newInputValueEntity, InputParameter newInputParameterEntity) {
        return newInputParameterEntity.getOccurrenceEntities().parallelStream().anyMatch(inputValue -> inputValue.getIdentifier() == newInputValueEntity.getIdentifier());
    }

    private boolean parameterExistsInUrlEntity(Url relatedUrlEntity, InputParameter newInputParameterEntity) {
        int identifier = newInputParameterEntity.getIdentifier();
        List<InputParameter> list = Collections.synchronizedList(new ArrayList<>());
        list.addAll(relatedUrlEntity.getFoundInParameterList());

        return list.stream().parallel().map(InputParameter::getIdentifier).collect(Collectors.toList()).contains(identifier);
    }

    private boolean urlExistsInDB(Url urlEntity) {
        int identifier = urlEntity.getIdentifier();
        return this.urlStorage.containsKey(identifier);
    }

    private Url getUrlEntityByParameterUrl(String url) {
        int identifier = Objects.hash(url);
        return urlStorage.get(identifier);
    }

    public ConcurrentHashMap<Integer, Url> getUrlStorage() {
        return urlStorage;
    }

    public List<InputParameter> getAllParameters(){
        return inputParameterStorage.values().stream().toList();
    }
    public List<InputParameter> getRelevant(String domain){
        var all = this.getAllParameters();
        //Ignore all parameters not from same domain and with empty value
        return all.stream().filter(param -> {return param.getDomain().equals(domain);}).collect(Collectors.toList());
    }

    public void setHasActiveSession (boolean isActive) {
        this.hasActiveSession = isActive;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public void clearAllMatchRelatedObjectsFromStorage() {
        for (Url url : urlStorage.values()) {
            url.setFound(new ArrayList<>());
        }
    }

    public void clearAllStorages() {
        inputParameterStorage.clear();
        inputValueStorage.clear();
        urlStorage.clear();
        allInputParametersList.clear();
    }

    public void updateParameterExclusion(RuleContainer ruleContainer) {
        RegexMatcher.excludeParametersForSingleRule(this.allInputParametersList, ruleContainer);
        if (hasActiveSession) {
            // Update exclusion on InputValues linked to Sessions
            var keys = SessionViewModel.sessionTable.keys().asIterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Session session = SessionViewModel.sessionTable.get(key);
                List<InputValue> inputValues = session.getInputValuesRelatedToSession();
                RegexMatcher.excludeInputValuesForSingleRule(inputValues, ruleContainer);
                session.setInputValuesRelatedToSession(inputValues);
                SessionViewModel.sessionTable.put(key, session);
            }
        }
    }
}

