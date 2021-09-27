import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.util.LinkedList;


@ExtensionInfo(
        Title = "GFallingFurni",
        Description = "Enjoy!",
        Version = "1.0.0",
        Author = "Julianty"
)

public class GFallingFurni extends ExtensionForm {
    public LinkedList<String> arrayList = new LinkedList(); // LinkedList es mas eficiente que ArrayList
    public LinkedList<Integer> poisonFurniList = new LinkedList();
    public LinkedList<Integer> specificFurniList = new LinkedList();

    public CheckBox checkPoison, checkCoords, checkAutodisable, checkSpecificPoint;
    public RadioButton radioCoords, radioCurrent, radioSpecificPoint, radioSpecificFurnis;
    public TextField fieldDelay;
    public Button buttonStart, buttonDeleteSpecific;
    public int XCoord, YCoord, XSpecificPoint, YSpecificPoint;
    public int UserID;

    @Override
    protected void initExtension() {

        /*
        Ignore this ...
        arrayList.add("Julianty");

        // Runs when the extension GUI is opened
        primaryStage.setOnShowing(e -> {
            if(!arrayList.contains(NameUser)){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning!");
                alert.setHeaderText("You're not on the list!");
                alert.setContentText("Sorry, but you can't use this module, i don't want cheating people.");

                alert.showAndWait();
                Platform.exit(); // Ignore, i don't really understand so good and i don't know if this is necessary
                System.exit(0);
            }
        });*/

        // Runs this when the extension GUI is closed
        primaryStage.setOnCloseRequest(e -> {
            Platform.runLater(() -> {
                buttonStart.setText("---OFF---");
            }); // Platform.exit();
            arrayList.clear(); poisonFurniList.clear(); radioCurrent.setSelected(true);
        });

        // Se ejecuta cuando el texto del textField cambia
        fieldDelay.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Integer.parseInt(fieldDelay.getText());
            } catch (NumberFormatException e) {
                fieldDelay.setText(oldValue);
            }
        });

        // The packet is sent to the server and a response is obtained from the CLIENT !!
        sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            UserID = hMessage.getPacket().readInteger();
        });

        // Intercept the double click on the furni
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            int FurniID = hMessage.getPacket().readInteger();
            if(radioSpecificFurnis.isSelected()){
                if(poisonFurniList.contains(FurniID)){
                    String SaySomething = "You cant select this furni because its on the poison list!";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                }
                if(!poisonFurniList.contains(FurniID)){
                    if(!specificFurniList.contains(FurniID)){
                        specificFurniList.add(FurniID);
                        Platform.runLater(() -> {
                            radioSpecificFurnis.setText("Specific Furnis (" + specificFurniList.size() + ")");
                        });
                        String SaySomething = "The furni has been added successfully";
                        sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                    }
                }
            }
            if(checkPoison.isSelected()){
                if(!poisonFurniList.contains(FurniID)){
                    poisonFurniList.add(FurniID);
                    Platform.runLater(() -> {
                        checkPoison.setText("Poison Furnis (" + poisonFurniList.size() + ")");
                    });
                    // Packet Structure
                    // {in:Whisper}{i:1956}{s:"Whatever thing here"}{i:0}{i:34}{i:0}{i:-1}{i:1956}
                    String SaySomething = "The furni with ID "+ FurniID +" has been added successfully";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                }
                else{
                    String SaySomething = "The furni has already been added!";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                }
            }
        });

        // Runs this instruction when the furni is added to the room
        intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", hMessage -> {
            try {
                DoSomething(hMessage);
            } catch (InterruptedException e) { e.printStackTrace(); }
        });

        // Runs this instruction when the furni is moved
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", hMessage -> {
            try {
                DoSomething(hMessage);
            } catch (InterruptedException e) { e.printStackTrace(); }
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            if(checkCoords.isSelected()){
                XCoord = hMessage.getPacket().readInteger();
                YCoord = hMessage.getPacket().readInteger();
                Platform.runLater(() -> {
                    checkCoords.setText("(" + XCoord + ", " + YCoord + ")");
                }); // Platform.exit();
                hMessage.setBlocked(true);
                checkCoords.setSelected(false);
            }
            else if(checkSpecificPoint.isSelected()){
                XSpecificPoint = hMessage.getPacket().readInteger();
                YSpecificPoint = hMessage.getPacket().readInteger();
                Platform.runLater(() -> {
                    checkSpecificPoint.setText("(" + XSpecificPoint + ", " + YSpecificPoint + ")");
                }); // Platform.exit();
                hMessage.setBlocked(true);
                checkSpecificPoint.setSelected(false);
            }
        });
    }

    private void DoSomething(HMessage hMessage) throws InterruptedException {
        if("---ON---".equals(buttonStart.getText())){
            int FurniID = hMessage.getPacket().readInteger();
            int WithoutUse = hMessage.getPacket().readInteger();
            int GetCoordX = hMessage.getPacket().readInteger();
            int GetCoordY = hMessage.getPacket().readInteger();
            System.out.println("...");

            System.out.println("Before Delay");
            Thread.sleep(Integer.parseInt(fieldDelay.getText())); // The time that the thread will sleep
            System.out.println("After Delay");

            // Analizar bien la logica aca tener cuidado
            if (!poisonFurniList.contains(FurniID)){
                if(radioCurrent.isSelected()){
                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, GetCoordX, GetCoordY));
                }
                if(radioCoords.isSelected()){
                    if(GetCoordX == XCoord && GetCoordY == YCoord){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, XCoord, YCoord));
                    }
                }
                if(radioSpecificPoint.isSelected()){
                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, XSpecificPoint, YSpecificPoint));
                }
                if(radioSpecificFurnis.isSelected()){
                    if(specificFurniList.contains(FurniID)){
                        sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, GetCoordX, GetCoordY));
                    }
                }
                if(checkAutodisable.isSelected()){
                    Platform.runLater(() -> {
                        buttonStart.setText("---OFF---");
                    }); // Platform.exit();
                }
            }
        }
    }

    public void handleButtonStart(ActionEvent actionEvent){
        if("---OFF---".equals(buttonStart.getText())){
            buttonStart.setText("---ON---");
        }
        else {
            buttonStart.setText("---OFF---");
        }
    }

    public void handleErasePoisons(ActionEvent actionEvent) {
        poisonFurniList.clear();
        Platform.runLater(() -> {
            checkPoison.setText("Poison Furnis (" + poisonFurniList.size() + ")");
        }); // Platform.exit();
    }

    public void handleDeleteSpecific(ActionEvent actionEvent) {
        specificFurniList.clear();
        Platform.runLater(() -> {
            radioSpecificFurnis.setText("Specific Furnis (" + specificFurniList.size() + ")");
        });
    }
}