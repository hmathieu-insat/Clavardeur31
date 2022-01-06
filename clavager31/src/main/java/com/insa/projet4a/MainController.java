package com.insa.projet4a;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private Button changeIdentityButton;
    @FXML
    private Label identityLabel;

    @FXML
    private VBox messageContainer;
    @FXML
    private TextField messageField;
    @FXML
    private ScrollPane scrollMessage;

    @FXML
    private VBox connectedContainer;

    Alert alert = new Alert(AlertType.ERROR,
            "Vous n'avez pas de discussion active, veuillez choisir un utilisateur avec qui communiquer.",
            ButtonType.OK);

    @FXML
    private Button eraseHistory;

    BDDManager bdd;

    /**
     * S'execute toujours au lancement de la fenetre.
     */
    @FXML
    protected void initialize() throws IOException, SQLException {

        this.bdd = new BDDManager("test");
        this.bdd.initHistory();

        for (String ip : App.getUserCorresp().keySet()) {
            this.addConnected(ip);
        }

        App.controller = this;
        identityLabel.setText(App.getPseudo());

        ArrayList<Message> list = new ArrayList<Message>();
        list.add(new Message(true, currentDate(), "Bienvenue dans Clavager31"));
        list.add(new Message(true, currentDate(),
                "Pour envoyer un message veuillez ajouter un utilisateur à vos discussions actives \n Selectionnez ensuite dans cette liste un utilisateur avec qui discuter."));
        loadMessages(list);
        App.isMainControllerInit = true;
        App.currentDiscussionIp = "";
    }

    // Pour changer de pseudo
    /**
     * Is called when the user wishes the change screen
     * <p>
     * 
     * @throws IOException
     */
    @FXML
    private void changeIdentity() throws IOException {
        App.setRoot("login_screen");
        App.changeSize(650, 450);
    }

    private String formatMessage(String messageText){
        int maxSize = 45;
        int i = 0;
        String newMessage = "";
        String phrase     = "";
        String[] mots = messageText.split(" ");

        while(i < mots.length) {

            while(phrase.length() < maxSize && i < mots.length){
                if(phrase.length() > 0){
                    phrase += " ";
                }
                if(mots[i].equals("\n")){
                    newMessage += phrase + "\n";
                    phrase = "";
                }
                else{
                    phrase += mots[i];
                }
                i++;
            }
            newMessage += phrase + "\n";
            phrase = "";
        }

        return newMessage;
    }

    /**
     * Ajoute un message à l'affichage actuel.
     * <p>
     * Cette fonction n'est jamais utilisé d'elle même. Invoquée à chaque fois
     * par {@code addMessageFrom} et {@code addMessageTo}
     * 
     * @param date    Date du message
     * @param content Contenu du message
     * @param path    Path de la ressource ".fxml"
     * @throws IOException
     */
    @FXML
    private void addMessage(String date, String content, String path) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        AnchorPane pane = loader.load(getClass().getResource(path).openStream());
        VBox vbox = (VBox) pane.getChildren().get(0);

        AnchorPane pane1 = (AnchorPane) vbox.getChildren().get(0);
        Label messageLabel = (Label) pane1.getChildren().get(0);
        content = formatMessage(content);
        messageLabel.setText(content);

        AnchorPane pane2 = (AnchorPane) vbox.getChildren().get(1);
        Label dateLabel = (Label) pane2.getChildren().get(0);
        dateLabel.setText(date);

        messageContainer.getChildren().add(pane);

        // bizarre il se trigger avant le message ?
        scrollMessage.applyCss();
        scrollMessage.layout();
        scrollMessage.setVvalue(1.0);
    }

    /**
     * Adds a message received to discussion display
     * 
     * @param date    Date of the message
     * @param content Content of the message
     * @throws IOException
     */
    public void addMessageFrom(String date, String content) throws IOException {
        addMessage(date, content, "components/messageFrom.fxml");
    }

    /**
     * Adds a message sent to discussion display
     * 
     * @param date    Date of the message
     * @param content Content of the message
     * @throws IOException
     */
    public void addMessageTo(String date, String content) throws IOException {
        addMessage(date, content, "components/messageTo.fxml");
    }

    /**
     * Efface tous les messages de la discussion actuelle
     */
    public void resetMessage() {
        messageContainer.getChildren().clear();
    }

    /**
     * Récupére la date et temps et les formatte.
     * 
     * @return La date et heure formattée sous forme lisible
     */
    public String currentDate() {
        Date date = new Date();
        SimpleDateFormat formater = new SimpleDateFormat("'Le' dd MMMM 'à' HH:mm");
        ;
        return formater.format(date);
    }

    /**
     * Is called when {@code ENTER} key is pressed after a message.
     * <p>
     * Append the message in display
     * 
     * @param key
     * @throws IOException
     * @throws SQLException
     */
    @FXML
    private void sendMessage(KeyEvent key) throws IOException, SQLException {
        if (key.getCode() == KeyCode.ENTER) {
            String messageText = messageField.getText();

            if (!messageText.isEmpty()) {

                // On vérifie qu'on discute bien avec quelqu'un
                // Pour éviter d'avoir supprimé quelqu'un et continuer de discuter avec

                if (!App.currentDiscussionIp.equals("")) {
                    String date = currentDate();
                    addMessageTo(date, messageText);
                    messageField.clear();

                    String ip = App.currentDiscussionIp;
                    this.bdd.insertHistory(ip, false, messageText, date);

                    App.transmitMessage(messageText, InetAddress.getByName(ip));
                } else {
                    alert.show();
                }
            }
        }
    }

    public void receiveMessage(String ip, String messageText) throws SQLException, IOException{
        String date = currentDate();
        this.bdd.insertHistory(ip, true, messageText, date);
        
        if (App.currentDiscussionIp.equals(ip)){
            addMessageFrom(date, messageText);
        }
        else{
            incrementNotif(ip);
        }
    }

    /**
     * Utilisé quand on change de fenetre ou quand on prend l'historique
     * 
     * @param list Liste des messages associés à la discussion
     * @throws IOException
     */
    private void loadMessages(ArrayList<Message> list) throws IOException {
        for (Message m : list) {
            Boolean from = m.from;
            String content = m.content;
            String date = m.date;

            if (from) {
                addMessageFrom(date, content);
            } else {
                addMessageTo(date, content);
            }
        }
    }

    private void paneSetText(AnchorPane pane, String text) {
        Label notificationLabel = (Label) pane.getChildren().get(0);
        notificationLabel.setText(text);
    }

    private void incrementNotif(String ip) {
        HBox hbox = lookup(ip);
        Pane largePane = (Pane) hbox.getChildren().get(0);
        AnchorPane pane = (AnchorPane) largePane.getChildren().get(1);

        Label notificationLabel = (Label) pane.getChildren().get(0);
        Integer nbNotif = Integer.parseInt(notificationLabel.getText()) + 1;
        notificationLabel.setText(nbNotif.toString());

        notificationLabel.setId("notif_true");
    }

    /**
     * Adds the user in the GUI corresponding to {@code ip} to the list of online
     * users.
     * 
     * @param ip IP address of the new connected user
     * @throws IOException
     */
    public void addConnected(String ip) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        HBox hbox = loader.load(getClass().getResource("components/connected.fxml").openStream());
        Pane pane = (Pane) hbox.getChildren().get(0);

        String name = App.getPseudoFromIP(ip);
        paneSetText((AnchorPane) pane.getChildren().get(0), name); // Le pseudo
        paneSetText((AnchorPane) pane.getChildren().get(1), "0"); // Les notifications

        // Les éléments ne peuvent avoir comme ID que des strings (comme en html)
        hbox.setId(ip);
        hbox.setOnMouseClicked(e -> {
            try {
                updateCurrentDiscussion(e);
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        });
        connectedContainer.getChildren().add(hbox);
    }

    /**
     * Removes the user in the GUI corresponding {@code ip} from the list of online
     * users, it then select the previous user in the connected list. 
     * I there isn't any the default page is shown instead.
     * 
     * @param ip IP address of the user who disconnected
     * @throws IOException
     */
    public void removeConnected(String ip) throws IOException {

        HBox old_current = null;
        ObservableList<Node> connected_list = connectedContainer.getChildren();

        int i;
        for (i=0; i < connected_list.size() ; i++) {
            HBox connected = (HBox)connected_list.get(i);
            if (connected.getId().equals(ip)){
                old_current = connected;
            }
        }

        if(old_current != null){ // si l'user à enlever est affiché
            connectedContainer.getChildren().remove(old_current);
            HBox new_current = (HBox)connected_list.get(i-1);
            if(new_current != null){ // on prend celui d'au dessus 
                new_current.fireEvent(new ActionEvent());
            }
            else{ // si y'en a pas au dessus on remet l'écran d'acceuil
                App.currentDiscussionIp = "";
                resetMessage();
                ArrayList<Message> list = new ArrayList<Message>();
                list.add(new Message(true, currentDate(), "Bienvenue dans Clavager31"));
                list.add(new Message(true, currentDate(),
                        "Pour envoyer un message veuillez ajouter un utilisateur à vos discussions actives \n Selectionnez ensuite dans cette liste un utilisateur avec qui discuter."));
                loadMessages(list);
            }
        }
    }

    /**
     * Updates the username corresponding to {@code ip} in the list of online users
     * when the username has changed pseudo
     * 
     * @param ip IP address of the user who changed his pseudo
     */
    public void updateConnected(String ip) {
        HBox hbox = lookup(ip);
        if (hbox != null) {
            Pane pane = (Pane) hbox.getChildren().get(0);
            String new_name = App.getPseudoFromIP(ip);
            paneSetText((AnchorPane) pane.getChildren().get(0), new_name);
        }
    }

    /**
     * Quand on clique sur la listeView cela choisi un user (surligné en bleu). A
     * chaque clique on regarde quel est l'utilisateur choisi
     * <p>
     * -> Load l'historique correspondant
     * 
     * @param e
     * @throws SQLException
     * @throws IOException
     */
    @FXML
    private void updateCurrentDiscussion(Event e) throws SQLException, IOException {

        HBox hbox = (HBox) e.getSource();
        App.currentDiscussionIp = hbox.getId();

        resetMessage();

        String ip = App.currentDiscussionIp;
        ArrayList<Message> history = this.bdd.showHistory(ip);
        loadMessages(history);

        // 1 seul pane à l'id "actif"
        // Quand on clique sur un pane on enlève l'id de l'ancien actif à ""
        // Et on mets actif au nouveau
        Pane oldpane = (Pane) connectedContainer.lookup("#actif");
        if (oldpane != null) {
            oldpane.setId("inactif");
        }
        Pane newpane = (Pane) hbox.getChildren().get(0);
        newpane.setId("actif");

        // On enlève aussi les notifications si il y'en avait
        AnchorPane pane = (AnchorPane) newpane.getChildren().get(1);
        Label notifLabel = (Label) pane.getChildren().get(0);
        notifLabel.setText("0");
        notifLabel.setId("notif_false");
    }

    /**
     * Clear the selected conversation history
     * 
     * @throws SQLException
     */
    @FXML
    private void clearHistory() throws SQLException {

        if (!App.currentDiscussionIp.equals("")) {

            resetMessage();

            // MODIFIER METTRE IP AU LIEU DE NOM
            // FAUT CORRESP IP/NOM
            this.bdd.clearHistory(App.currentDiscussionIp);
        } else {
            alert.show();
        }
    }

    private HBox lookup(String ip) {
        HBox hbox = null;
        for (Node child : connectedContainer.getChildren()) {
            if (((HBox) child).getId().equals(ip)){
                hbox = (HBox) child;
            }
        }
        return hbox;
    }
}