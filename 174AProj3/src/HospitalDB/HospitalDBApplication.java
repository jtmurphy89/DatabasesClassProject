package HospitalDB;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class HospitalDBApplication extends Application {


    public static void main(String[] args) {
        Application.launch(HospitalDBApplication.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        HospitalDBController.getHealthMessagesConnection();
        HospitalDBController.getHospitalDBConnection();
        HospitalDBController.initializeHospitalDB();
        Parent root = FXMLLoader.load(getClass().getResource("HospitalDBView.fxml"));
        primaryStage.setTitle("Ceci n'est pas un hôpital...");
        primaryStage.getIcons().add(new Image("http://files.softicons.com/download/medical-icons/vista-medical-icons-by-icons-land/png/32x32/Caduceus.png"));
        primaryStage.setScene(new Scene(root, 1000, 555));
        primaryStage.show();
    }




    @Override
    public void stop() throws Exception {
        super.stop();
        HospitalDBController.endConnection();
    }
}
