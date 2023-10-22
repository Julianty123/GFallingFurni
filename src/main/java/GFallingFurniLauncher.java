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
import javafx.stage.Stage;

public class GFallingFurniLauncher extends ExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GFallingFurni.fxml"));
        Parent root = loader.load();

        stage.setTitle("GFallingFurni");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);

        Theme defaultTheme = ThemeFactory.themeForTitle("G-Earth Dark"); // ThemeFactory.getDefaultTheme()
        DefaultTitleBarConfig config = new DefaultTitleBarConfig(stage, defaultTheme) {
            public boolean displayThemePicker() {
                return false; // For show bar themes (I like to be redundant)
            }
        };

        TitleBarController.create(stage, config); // Idk implementation, but applies the theme to the bar
        Platform.runLater(() -> {
            stage.getScene().getRoot().getStyleClass().add(defaultTheme.title().replace(" ", "-").toLowerCase());
            stage.getScene().getRoot().getStyleClass().add(defaultTheme.isDark() ? "g-dark" : "g-light");
        });

        return loader.getController();
    }

    public static void main(String[] args) {
        runExtensionForm(args, GFallingFurniLauncher.class);
    }

}