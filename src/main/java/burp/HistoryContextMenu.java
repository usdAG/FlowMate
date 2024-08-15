package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import gui.GettingStartedView;
import utils.Hashing;
import utils.MessageHashToProxyId;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class HistoryContextMenu implements ContextMenuItemsProvider {

    private MontoyaApi api;
    private GettingStartedView gettingStartedView;

    private boolean isStartSet;
    private boolean isEndSet;
    private int startValue;
    private int endValue;

    public HistoryContextMenu(MontoyaApi api, GettingStartedView gettingStartedView) {
        this.api = api;
        this.gettingStartedView = gettingStartedView;
        this.isStartSet = false;
        this.isEndSet = false;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {

        List<Component> menuItems = new ArrayList<>();
        JMenuItem setStartPointMenu = new JMenuItem("Set as start point for Retroactive Parsing");
        JMenuItem setEndPointMenu = new JMenuItem("Set as end point for Retroactive Parsing");

        menuItems.add(setStartPointMenu);
        menuItems.add(setEndPointMenu);

        setStartPointMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                isStartSet = true;
                HttpRequest selectedRequest = event.selectedRequestResponses().get(0).request();
                String messageHash = Hashing.sha1(event.selectedRequestResponses().get(0).request().toByteArray().getBytes());
                startValue = MessageHashToProxyId.getInstance(api).calculateId(messageHash);
                if (!selectionsAreValid(selectedRequest)) {
                    isStartSet = false;
                    startValue = -1;
                    return;
                }

                gettingStartedView.setStartValue(startValue);
            }
        });

        setEndPointMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                isEndSet = true;
                HttpRequest selectedRequest = event.selectedRequestResponses().get(0).request();
                String messageHash = Hashing.sha1(event.selectedRequestResponses().get(0).request().toByteArray().getBytes());
                endValue = MessageHashToProxyId.getInstance(api).calculateId(messageHash);
                if (!selectionsAreValid(selectedRequest)) {
                    isEndSet = false;
                    endValue = -1;
                    return;
                }

                gettingStartedView.setEndValue(endValue);
            }
        });

        return menuItems;
    }

    private boolean selectionsAreValid(HttpRequest selectedRequest) {
        if (!selectedRequest.isInScope()) {
            JOptionPane.showMessageDialog(null, "Selected request is not in scope!", "Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if ((isEndSet && isStartSet) && endValue < startValue) {
            JOptionPane.showMessageDialog(null, "Start value is greater than End value", "Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
}
