package com.insa.projet4a;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

    @FXML private Button changeIdentityButton;
    @FXML private Label identityLabel;

    @FXML private VBox messageContainer;
    @FXML private TextField messageField;
    @FXML private ScrollPane scrollMessage; 

    @FXML private VBox connectedContainer;

    Alert alert = new Alert(AlertType.ERROR,
                        "Vous n'avez pas de discussion active, veuillez choisir un utilisateur avec qui communiquer.", 
                        ButtonType.OK);

    @FXML private Button eraseHistory;

    BDDManager bdd;

    // S'execute toujours au lancement de la fenetre
    @FXML
    protected void initialize() throws IOException, SQLException {

        this.bdd = new BDDManager("test");
        this.bdd.initHistory();
        App.controller = this;

        identityLabel.setText(App.pseudo);
        addConnected("localhost");
        addConnected("localhost1");
        addConnected("localhost2");

        removeConnected("localhost");
        addConnected("localhost3");
        addConnected("localhost");

        ArrayList<Message> list = new ArrayList<Message>();
        list.add(new Message(true,currentDate(),"Bienvenue dans Clavager31"));
        list.add(new Message(true,currentDate(),"Pour envoyer un message veuillez ajouter un utilisateur à vos discussions actives\nSelectionnez ensuite dans cette liste un utilisateur avec qui discuter."));
        loadMessages(list);
    }

    // Pour changer de pseudo
    @FXML
    private void changeIdentity() throws IOException {
        App.setRoot("login_screen");
        App.changeSize(650, 450);
    }

    // Ajoute un message à l'affichage actuel
    // Cette fonction n'est jamais utilisé d'elle même
    // On utilise à chaque fois 
        // addMessageFrom ou
        // addMessageTo
    @FXML
    private void addMessage(String date, String content, String path) throws IOException{
        FXMLLoader loader = new FXMLLoader();   
        AnchorPane pane = loader.load(getClass().getResource(path).openStream());
        VBox vbox = (VBox) pane.getChildren().get(0);

        AnchorPane pane1 = (AnchorPane) vbox.getChildren().get(0);
        Label messageLabel = (Label) pane1.getChildren().get(0);
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

    public void addMessageFrom(String date, String content) throws IOException{
        addMessage(date, content, "components/messageFrom.fxml");
    }

    public void addMessageTo(String date, String content) throws IOException{
        addMessage(date, content, "components/messageTo.fxml");
    }

    // Efface tous les messages de la discussion actuelle
    public void resetMessage(){
        messageContainer.getChildren().clear();
    }

    // Quand on envoie un message on récupére la date du jour
    // On la formate sous une forme jolie
    public String currentDate(){
        Date date = new Date();
        SimpleDateFormat formater = new SimpleDateFormat("'Le' dd MMMM 'à' HH:mm");;
        return formater.format(date);
    }

    // Rajoute un message à la discussion actuelle
    // -> Append dans l'historique correspondant
    // -> Transmettre par TCP le message
    @FXML
    private void sendMessage(KeyEvent key) throws IOException, SQLException {
        if(key.getCode() == KeyCode.ENTER){
            String messageText = messageField.getText();

            if (!messageText.isEmpty()){

                // On vérifie qu'on discute bien avec quelqu'un
                // Pour éviter d'avoir supprimé quelqu'un et continuer de discuter avec

                if (!App.currentDiscussionIp.equals("")){
                    String date = currentDate();
                    addMessageTo(date,messageText);
                    messageField.clear();

                    String name = App.getCurrentUserName();
                    this.bdd.insertHistory(name, false, messageText, date);

                    incrementNotif("localhost");
                }
                else{
                    alert.show();
                }
            }  
        }
    }

    // Utilisé quand on change de fenetre ou quand on prend l'historique
    private void loadMessages(ArrayList<Message> list) throws IOException{
        for(Message m : list){
            Boolean from = m.from;
            String content = m.content;
            String date = m.date;

            if (from){
                addMessageFrom(date,content);
            }
            else{
                addMessageTo(date,content);
            }
        }
    }

    private void paneSetText(AnchorPane pane, String text){
        Label notificationLabel = (Label) pane.getChildren().get(0);
        notificationLabel.setText(text);
    }

    private void incrementNotif(String ip){
        HBox hbox = (HBox)connectedContainer.lookup("#"+ip);
        Pane largePane = (Pane)hbox.getChildren().get(0);
        AnchorPane pane = (AnchorPane)largePane.getChildren().get(1);

        Label notificationLabel = (Label) pane.getChildren().get(0);
        Integer nbNotif = Integer.parseInt(notificationLabel.getText()) + 1;
        notificationLabel.setText(nbNotif.toString());

        notificationLabel.setId("notif_true");
    }

    public void addConnected(String ip) throws IOException{
        FXMLLoader loader = new FXMLLoader();   
        HBox hbox = loader.load(getClass().getResource("components/connected.fxml").openStream());
        Pane pane = (Pane)hbox.getChildren().get(0);

        String name = App.getUserCorresp(ip);
        paneSetText((AnchorPane)pane.getChildren().get(0), name); // Le pseudo
        paneSetText((AnchorPane)pane.getChildren().get(1), "0");  // Les notifications

        // Les éléments ne peuvent avoir comme ID que des strings (comme en html)
        hbox.setId(ip);
        hbox.setOnMouseClicked(e -> {
            try {
                updateCurrentDiscussion(e);
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        } );
        connectedContainer.getChildren().add(hbox);
    }

    public void removeConnected(String ip){
        HBox hbox = (HBox)connectedContainer.lookup("#"+ip);
        connectedContainer.getChildren().remove(hbox);
    }

    // Quand on clique sur la listeView cela choisi un user (surligné en bleu)
    // A chaque clique on regarde quel est l'utilisateur choisi
    // -> Load l'historique correspondant
    @FXML
    private void updateCurrentDiscussion(Event e) throws SQLException, IOException{

            HBox hbox = (HBox)e.getSource();
            App.currentDiscussionIp = hbox.getId();

            resetMessage();
            
            String name = App.getCurrentUserName();
            ArrayList<Message> history = this.bdd.showHistory(name);
            loadMessages(history);

            // 1 seul pane à l'id "actif"
            // Quand on clique sur un pane on enlève l'id de l'ancien actif à ""
            // Et on mets actif au nouveau
            Pane oldpane = (Pane)connectedContainer.lookup("#actif");
            if(oldpane != null){oldpane.setId("inactif");}
            Pane newpane = (Pane)hbox.getChildren().get(0);
            newpane.setId("actif");

            // On enlève aussi les notifications si il y'en avait
            AnchorPane pane = (AnchorPane)newpane.getChildren().get(1);
            Label notifLabel = (Label)pane.getChildren().get(0);
            notifLabel.setText("0");
            notifLabel.setId("notif_false");
    }

    @FXML 
    private void clearHistory() throws SQLException{

        if (!App.currentDiscussionIp.equals("")){

            resetMessage();

            // MODIFIER METTRE IP AU LIEU DE NOM
            // FAUT CORRESP IP/NOM
            this.bdd.clearHistory(App.getCurrentUserName());
        }
        else{
            alert.show();
        }
    }
}