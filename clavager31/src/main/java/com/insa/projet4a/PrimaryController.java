package com.insa.projet4a;

import java.io.IOException;
import javafx.fxml.FXML;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        GUI.setRoot("login_screen");
    }
}
