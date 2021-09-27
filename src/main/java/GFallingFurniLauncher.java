import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GFallingFurniLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GFallingFurni.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("GFallingFurni");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GFallingFurniLauncher.class);
    }

}