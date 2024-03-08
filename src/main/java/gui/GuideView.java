package gui;

import net.miginfocom.swing.MigLayout;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GuideView extends JTabbedPane {

    public GuideView() {
        renderMarkdownBrowserGuide();
        renderMarkdownGraphGuide();
        this.addTab("Additional Queries", new AdditionalQueriesTab());
    }

    private void renderMarkdownBrowserGuide() {
        String html = browserGuideMd();
        Parser parser = Parser.builder().build();
        Node document = parser.parse(html);
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        JEditorPane renderedDocument = new JEditorPane();
        renderedDocument.setContentType("text/html");
        renderedDocument.setText(htmlRenderer.render(document));
        String defaultFontName = renderedDocument.getFont().getFontName();
        renderedDocument.setFont(new Font(defaultFontName, Font.PLAIN, 16));
        renderedDocument.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(renderedDocument);

        this.addTab("Browser Guide", scrollPane);
    }

    private void renderMarkdownGraphGuide() {
        String html = graphGuide();
        Parser parser = Parser.builder().build();
        Node document = parser.parse(html);
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        JEditorPane renderedDocument = new JEditorPane();
        renderedDocument.setContentType("text/html");
        renderedDocument.setText(htmlRenderer.render(document));
        String defaultFontName = renderedDocument.getFont().getFontName();
        renderedDocument.setFont(new Font(defaultFontName, Font.PLAIN, 16));
        renderedDocument.setEditable(false);

        JEditorPane additionalQueriesHeader = new JEditorPane();
        additionalQueriesHeader.setContentType("text/html");
        additionalQueriesHeader.setOpaque(true);
        additionalQueriesHeader.setText("<html><h2>Additional Neo4J Cypher Queries:</h2></html>");

        JPanel panel = new JPanel(new MigLayout());
        panel.add(renderedDocument, "wrap");
        panel.add(additionalQueriesHeader, "wrap");
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        this.addTab("Graph Guide", scrollPane);
    }

    private String browserGuideMd() {
        String connect = Objects.requireNonNull(GuideView.class.getResource("/neo4j_browser.png")).toString();
        String databaseInformation = Objects.requireNonNull(GuideView.class.getResource("/Database_Information.png")).toString();
        String queryBar = Objects.requireNonNull(GuideView.class.getResource("/Query_Bar.png")).toString();
        String queryData = Objects.requireNonNull(GuideView.class.getResource("/Match_all.png")).toString();
        String changeStyle = Objects.requireNonNull(GuideView.class.getResource("/Change_Node_Style.png")).toString();
        return "# Neo4J Browser Guide:\n" +
                "\n" +
                "**Note: To record data you first have to add the domain to your scope in Burp Suite.**  \n" +
                "\n" +
                "\n" +
                "### How to connect\n" +
                "\n" +
                // TODO: Fix link to be clickable inside Burp
                "Open your Webbrowser and connect to <a href=http://localhost:7474>http://localhost:7474</a>  \n" +
                "![Neo4J Browser connection settings](" + connect + ")  \n" +
                "Set Connect URL to **bolt://localhost:5555**  \n" +
                "Standard Username/Password is **neo4j/neo4J**" +
                "\n" +
                "\n" +
                "## Browser Overview:\n" +
                "\n" +
                "### Database Information:  \n" +
                "![Database Information Menu](" + databaseInformation + ")  \n" +
                "The left menu bar shows Database Information, like which Nodes exist, which relationship types exist and all existing property keys.\n" +
                "\n" +
                "### Query Bar: \n" +
                "![Query Bar](" + queryBar + ")  \n" +
                "The Query Bar is used to enter a Cypher Query. It is used to query data from the database.  \n" +
                "\n" +
                "### Querying Data:\n" +
                "![Example Query](" + queryData + ")  \n" +
                "The screenshot shows an example query where all of the stored data in the database is retrieved. Beside providing a graph view, \n" +
                "you can also view your query results as a Table or in Text format.  \n" +
                "Additionally Neo4J provides a function to export the graph/text.  \n" +
                "\n" +
                "### Changing the style\n" +
                "![Styling](" + changeStyle + ")  \n" +
                "By clicking on a node in the Overview menu on the right side, it is possible to change the appearance of the graph.  \n" +
                "It lets you change the node color, size and caption text. ";

    }

    private String graphGuide() {
        String exampleGraph = Objects.requireNonNull(GuideView.class.getResource("/Graph_description.png")).toString();
        String browserURL = Objects.requireNonNull(GuideView.class.getResource("/Browser_URL.png")).toString();
        String searchMatch = Objects.requireNonNull(GuideView.class.getResource("/Search_Match.png")).toString();
        String matchEntry = Objects.requireNonNull(GuideView.class.getResource("/MatchEntry.png")).toString();
        return "# How to read the Graph:\n" +
                "\n" +
                "## Nodes:\n" +
                "There are 5 types of Nodes:  \n" +
                "**URL:** The URL under which an InputParameter/ParameterMatch was found.  \n" +
                "**InputParameter:** The InputParameter that was found.  \n" +
                "**InputValue:** Represents with which value the InputParameter occurred.  \n" +
                "**ParameterMatch:** Represents for which InputParameter and InputParameter value a ParameterMatch was found. \"ParameterMatch\" means, the InputValue sent at a given point reappeared in the HTTP Response from the webserver.  \n" +
                "**MatchValue:** Each reappearance of an InputValue is represented by a MatchValue node.  \n" +
                "\n" +
                "\n" +
                "## Relationships:\n" +
                "The nodes are connected with each other via Relationships.\n" +
                "\n" +
                "The **URL Node** illustrates the center of a data flow graph. It is connected to an **InputParameter Node** and **ParameterMatch Node**.  \n" +
                "When an **InputParameter** occurs within an URL it gets detected by the extension and is created and connected to the **URL Node** in the database.  \n" +
                "Additionally, if the **InputParameter** occurred with a value, an **InputValue Node** is created and connected to it's corresponding **InputParameter Node**.  \n" +
                "\n" +
                "If an **InputParameter** and it's corresponding value appears some where on the Webapplication, a **ParameterMatch Node** is created and connected to the **URL Node** under which the **ParameterMatch** was found.  \n" +
                "Additionally, for each appearance of the **InputParameter** value, a **MatchValue Node** is created and connected to the **ParameterMatch Node**. The **MatchValue Node** contains additional information about the **ParameterMatch**,  \n" +
                "like the **matchProof** String, which is an HTML code snippet from the part of the HTTP Response where the **ParameterMatch** occurred.  \n" +
                "\n" +
                "## Example:\n" +
                "![Example](" + exampleGraph + ")  \n" +
                "The example screenshot shows the graph for a search with the keyword \"test\" on a test website.\n" +
                "\n" +
                "![Browser URL](" + browserURL + ")  \n" +
                "The search function on the website calls the php file *search.php* with the InputParameter *keywords*. In this case the InputValue is *test*.  \n" +
                "The extension detects the URL, InputParameter and InputValue and creates the corresponding nodes in the database as shown in the example graph.  \n" +
                "\n" +
                "The HTTP Response from the Website contains the searched InputValue:  \n" +
                "![Search ParameterMatch](" + searchMatch + ")  \n" +
                "\n" +
                "The extension searches the HTTP Response for the InputValue and detects a ParameterMatch. It creates a new ParameterMatch node and connects it to the URL where the ParameterMatch occurred.  \n" +
                "For each ParameterMatch that is related to the same InputParameter name and InputValue, a MatchValue node is created and connected to the ParameterMatch node, which contains additional information about the ParameterMatch.  \n" +
                "As shown below, one of the MatchValue nodes contains the HTML code snippet for the screenshot above.  \n" +
                "\n" +
                "![MatchValue Node properties](" + matchEntry + ")  \n";
    }
}
