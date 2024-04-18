import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.titlebar.DefaultTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class GFallingFurniLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GFallingFurni.fxml"));
        Parent root = loader.load();

        stage.setTitle("GFallingFurni");
        stage.initStyle(StageStyle.TRANSPARENT);
//        stage.getScene().setFill(Color.TRANSPARENT);
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);

        // Ugly way to set the icon (from IDE or from CMD is a headache) >:(
//        String pathName = "/C:/Users/User/IdeaProjects/MazeProgrammer/src/main/resources/imageJ.jfif";
//        File file = new File(pathName);
//        if (file.exists())
//            primaryStage.getIcons().add(new Image(file.toURI().toString()));

        // Best way to set the icon (Works in both IDE and compiled application)
        String resourceName = "imageJ.jfif";
        InputStream inputStream = GFallingFurniLauncher.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream != null) {
            Image image = new Image(inputStream);
            stage.getIcons().add(image);
        }

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GFallingFurniLauncher.class);
    }

}