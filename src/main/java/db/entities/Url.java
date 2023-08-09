package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NodeEntity
public class Url {
    @Id
    private int identifier;

    public String getUrl() {
        return url;
    }

    private String url;

    @Relationship(type = "FOUND", direction = Relationship.Direction.OUTGOING)
    private List<ParameterMatch> found = new ArrayList<ParameterMatch>();

    /* Direction UNDIRECTED has the same effect as INCOMING but fixes NullPointerException in EntityGraphMapper due to
       tgtClass.getName() being null

       EntityGraphMapper:606 -->
       if (relationshipBuilder.hasDirection(Direction.INCOMING)) {
                if (this.metaData.isRelationshipEntity(tgtClass.getName())) {
                    srcClass = tgtClass;
                    String start = this.metaData.classInfo(tgtClass.getName()).getStartNodeReader().getTypeDescriptor();
                    tgtClass = DescriptorMappings.getType(start);
                }
     */
    @Relationship(type="FOUND_PARAMETER",direction = Relationship.Direction.UNDIRECTED)
    private List<InputParameter> foundInInputParameterList = new ArrayList<InputParameter>();

    // Empty Constructor needed for neo4J
    public Url() {}

    public Url(String url) {
        this.url = url;
        this.identifier = Objects.hash(url);
    }

    public List<ParameterMatch> getFound() {
        return found;
    }

    public List<InputParameter> getFoundInParameterList() {
        return foundInInputParameterList;
    }

    public void addParameterFoundInUrl(InputParameter inputParameterEntity) {
        this.foundInInputParameterList.add(inputParameterEntity);
    }
    public void addFound(ParameterMatch foundParameterMatchEntity) {
        found.add(foundParameterMatchEntity);
    }

    public int getIdentifier(){
        return identifier;
    }

    @Override
    public String toString() {
        return "Url{" +
                "\nidentifier=\n'" + identifier + '\'' +
                "\nurl='" + url + '\'' +
                "\n found=" + found +
                "\n foundInInputParameterList=" + foundInInputParameterList +
                '}';
    }
}
