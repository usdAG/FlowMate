package utils;

import burp.HttpResponse;
import db.MatchHelperClass;
import db.entities.InputParameter;

import java.util.List;

public interface IParser {

    boolean initialize(HttpResponse response);

    List<MatchHelperClass> matchAllOccurrences(InputParameter inputParameterEntity, String messageHash);
}
