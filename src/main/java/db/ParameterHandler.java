package db;

import burp.RegexMatcher;
import db.entities.InputParameter;
import db.entities.InputValue;
import db.entities.Url;
import gui.GettingStartedView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.stream.Collectors;

// InputParameter Entities are being saved via the corresponding URL Entity where the parameter appeared
// Hence keeping track of the URL Entities is enough because they contain the Parameters
public class ParameterHandler {

    public Hashtable<Integer, Url> urlStorage;
    public Hashtable<Integer, InputParameter> parameterStorage;
    public Hashtable<Integer, InputValue> parameterValueStorage;
    public ObservableList<InputParameter> observableInputParameterList;
    public ObservableList<InputParameter> observableInputParameterListSession;

    private boolean hasActiveSession = false;
    private String sessionName;
    public ParameterHandler() {
        this.urlStorage = new Hashtable<>();
        this.parameterStorage = new Hashtable<>();
        this.parameterValueStorage = new Hashtable<>();
        this.observableInputParameterList = FXCollections.observableArrayList();
        this.observableInputParameterListSession = FXCollections.observableArrayList();
        loadUrls();
        loadParameters();
        loadParameterOccurrences();
        this.observableInputParameterList = FXCollections.observableArrayList(RegexMatcher.excludeParameters(observableInputParameterList));
    }

    public void loadUrls() {
        List<Url> urlEntityList = DBModel.loadAllUrlEntities().stream().toList();
        List<Integer> identifiers = urlEntityList.stream().map(Url::getIdentifier).toList();
        urlStorage.putAll(combineListsIntoUrlEntityMap(identifiers, urlEntityList));
    }

    public void loadParameters() {
       List<InputParameter> inputParameterEntityList = DBModel.loadAllParameters().stream().toList();
       List<Integer> identifiers = inputParameterEntityList.stream().map(InputParameter::getIdentifier).toList();
       parameterStorage.putAll(combineListsIntoParameterEntityMap(identifiers, inputParameterEntityList));
       observableInputParameterList.addAll(inputParameterEntityList);
    }

    public void loadParameterOccurrences() {
        List<InputValue> inputValueEntityList = DBModel.loadAllParameterOccurrenceEntities().stream().toList();
        List<Integer> identifiers = inputValueEntityList.stream().map(InputValue::getIdentifier).toList();
        parameterValueStorage.putAll(combineListsIntoParameterOccurrenceMap(identifiers, inputValueEntityList));
    }

    Map<Integer, InputParameter> combineListsIntoParameterEntityMap(List<Integer> keys, List<InputParameter> values) {
        if (keys.size() != values.size())
            throw new IllegalArgumentException ("Cannot combine lists with dissimilar sizes");
        Map<Integer, InputParameter> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    Map<Integer, Url> combineListsIntoUrlEntityMap(List<Integer> keys, List<Url> values) {
        if (keys.size() != values.size())
            throw new IllegalArgumentException ("Cannot combine lists with dissimilar sizes");
        Map<Integer, Url> map = new Hashtable<>();
        for (int i=0; i<keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    Map<Integer, InputValue> combineListsIntoParameterOccurrenceMap(List<Integer> keys, List<InputValue> values) {
        if (keys.size() != values.size())
            throw new IllegalArgumentException ("Cannot combine lists with dissimilar sizes");
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
    private void addParameterToDB(ParameterHelperClass parameterHelper, String messageHash) {
        String name = parameterHelper.getName();
        String type = parameterHelper.getType().getName();
        String domain = parameterHelper.getDomain();
        String url = parameterHelper.getUrlFound();

        InputParameter newInputParameterEntity = new InputParameter(name, type, domain);
        Url newUrlEntity = new Url(url);
        InputValue newInputValue = new InputValue(parameterHelper.getValue(), url, type, messageHash);
        if (this.hasActiveSession) {
            newInputValue = new InputValue(parameterHelper.getValue(), url, type, messageHash, this.sessionName);
        }

        // Check if the URL Entity already exists in the DB
        // If not, add it to the list of known Urls and save the Url + InputParameter in the DB
        if (!urlExistsInDB(newUrlEntity)) {
            newInputParameterEntity.addOccurence(newInputValue);
            this.parameterValueStorage.put(newInputValue.getIdentifier(), newInputValue);
            newUrlEntity.addParameterFoundInUrl(newInputParameterEntity);
            this.urlStorage.put(newUrlEntity.getIdentifier(), newUrlEntity);
            parameterStorage.put(newInputParameterEntity.getIdentifier(), newInputParameterEntity);
            GettingStartedView.numberOfParameterValues.setText(String.valueOf(parameterValueStorage.size()));
            GettingStartedView.numberOfParameters.setText(String.valueOf(parameterStorage.size()));
            GettingStartedView.numberOfUrls.setText(String.valueOf(urlStorage.size()));
            DBModel.saveParameter(newInputParameterEntity);
            DBModel.saveURL(newUrlEntity);
            this.observableInputParameterList.add(newInputParameterEntity);
            if (newInputValue.getSession().equals(sessionName)) {
                this.observableInputParameterListSession.add(newInputParameterEntity);
            }
        } else {
            // If the URL Entity already exists get the correct Url Entity where the InputParameter Entity is to be added
            Url relatedUrlEntity = getUrlEntityByParameterUrl(parameterHelper.getUrlFound());
            // Check if Relationship to InputParameter already exists, this is the case if the URLs get loaded at start
            if (!parameterExistsInUrlEntity(relatedUrlEntity, newInputParameterEntity)) {
                if(!occurrenceAlreadyExists(newInputValue)) {
                    newInputParameterEntity.addOccurence(newInputValue);
                    this.parameterValueStorage.put(newInputValue.getIdentifier(), newInputValue);
                    GettingStartedView.numberOfParameterValues.setText(String.valueOf(parameterValueStorage.size()));
                    if (newInputValue.getSession().equals(sessionName)) {
                        this.observableInputParameterListSession.add(newInputParameterEntity);
                    }
                }
                relatedUrlEntity.addParameterFoundInUrl(newInputParameterEntity);
                parameterStorage.put(newInputParameterEntity.getIdentifier(), newInputParameterEntity);
                GettingStartedView.numberOfParameters.setText(String.valueOf(parameterStorage.size()));
                DBModel.saveParameter(newInputParameterEntity);
                DBModel.saveURL(relatedUrlEntity);
                this.observableInputParameterList.add(newInputParameterEntity);
            } else {
                InputParameter existingEntity = getExistingParameter(newInputParameterEntity.getIdentifier());
                if (!occurrenceAlreadyExists(newInputValue)) {
                    existingEntity.addOccurence(newInputValue);
                    this.parameterValueStorage.put(newInputValue.getIdentifier(), newInputValue);
                    GettingStartedView.numberOfParameterValues.setText(String.valueOf(parameterValueStorage.size()));
                    DBModel.saveParameter(existingEntity);
                    this.observableInputParameterList.add(newInputParameterEntity);
                    if (newInputValue.getSession().equals(sessionName)) {
                        this.observableInputParameterListSession.add(newInputParameterEntity);
                    }
                }
            }
        }
    }

    public void addParameters(Collection<ParameterHelperClass> oldParameterCollection, String messageHash) {
        for (ParameterHelperClass oldParameter : oldParameterCollection) {
            addParameterToDB(oldParameter, messageHash);
        }
    }

    private InputParameter getExistingParameter(int identifier) {
        return parameterStorage.get(identifier);
    }

    private boolean occurrenceAlreadyExists(InputValue newInputValueEntity) {
        return this.parameterValueStorage.containsKey(newInputValueEntity.getIdentifier());
    }

    private boolean parameterExistsInUrlEntity(Url relatedUrlEntity, InputParameter newInputParameterEntity) {
        int identifier = newInputParameterEntity.getIdentifier();
        return relatedUrlEntity.getFoundInParameterList().stream().map(InputParameter::getIdentifier).toList().contains(identifier);
    }

    private boolean urlExistsInDB(Url urlEntity) {
        int identifier = urlEntity.getIdentifier();
        return this.urlStorage.containsKey(identifier);
    }

    private Url getUrlEntityByParameterUrl(String url) {
        int identifier = Objects.hash(url);
        return urlStorage.get(identifier);
    }

    public Hashtable<Integer, Url> getUrlStorage() {
        return urlStorage;
    }

    public List<InputParameter> getAllParameters(){
        return parameterStorage.values().stream().toList();
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
        parameterStorage.clear();
        parameterValueStorage.clear();
        urlStorage.clear();
        observableInputParameterList.clear();
        observableInputParameterListSession.clear();
    }

    public void updateParameterExclusion() {
        this.observableInputParameterList = FXCollections.observableArrayList(RegexMatcher.excludeParameters(this.observableInputParameterList));
    }
}

