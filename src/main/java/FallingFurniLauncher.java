import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class FallingFurniLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FallingFurni.fxml"));
        Parent root = loader.load();

        stage.setTitle("FallingFurni");
        stage.initStyle(StageStyle.TRANSPARENT);
//        stage.getScene().setFill(Color.TRANSPARENT);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);

        // Best way to set the icon (Works in both IDE and compiled application)
        String resourceName = "imageJ.jfif";
        InputStream inputStream = FallingFurniLauncher.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {
            Image image = new Image(inputStream);
            stage.getIcons().add(image);
        }

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, FallingFurniLauncher.class);
    }
}