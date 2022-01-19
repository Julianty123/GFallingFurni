import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import java.util.LinkedList;


@ExtensionInfo(
        Title = "GFallingFurni",
        Description = "Enjoy!",
        Version = "1.2.0",
        Author = "Julianty"
)

public class GFallingFurni extends ExtensionForm {
    public LinkedList<String> arrayList = new LinkedList<>(); // LinkedList is better than ArrayList
    public LinkedList<Integer> poisonFurniList = new LinkedList<>();
    public LinkedList<Integer> specificFurniList = new LinkedList<>();
    //public Thread thread = null;

    public CheckBox checkPoison, checkCoords, checkAutodisable, checkSpecificPoint, checkSpecificFurni;
    public RadioButton radioCoords, radioCurrent, radioSpecificPoint;
    public TextField fieldDelay;
    public Button buttonStart, buttonDeleteSpecific;
    public int FurniID, UserID, GetCoordX, GetCoordY, XCoord, YCoord, XSpecificPoint, YSpecificPoint;

    @Override
    protected void initExtension() {

        /* Ignore this ...
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

        // Runs this when the GUI is closed
        primaryStage.setOnCloseRequest(e -> {
            Platform.runLater(() -> buttonStart.setText("---OFF---")); // Platform.exit();
            arrayList.clear(); poisonFurniList.clear(); radioCurrent.setSelected(true);
        });

        // Runs when the text field changes!
        fieldDelay.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Integer.parseInt(fieldDelay.getText());
            } catch (NumberFormatException e) {
                if("".equals(fieldDelay.getText())){
                    fieldDelay.setText("1");
                }
                else {
                    fieldDelay.setText(oldValue);
                }
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
            if(checkSpecificFurni.isSelected()){
                if(poisonFurniList.contains(FurniID)){
                    String SaySomething = "You cant select this furni because its on the poison list!";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                }
                if(!poisonFurniList.contains(FurniID)){
                    if(!specificFurniList.contains(FurniID)){
                        specificFurniList.add(FurniID);
                        Platform.runLater(() -> checkSpecificFurni.setText("Specific Furnis (" + specificFurniList.size() + ")"));
                        String SaySomething = "The furni has been added successfully";
                        sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                    }
                }
            }
            if(checkPoison.isSelected()){
                if(!specificFurniList.contains(FurniID)){
                    if(!poisonFurniList.contains(FurniID)){
                        poisonFurniList.add(FurniID);
                        Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + poisonFurniList.size() + ")"));
                        // Packet Structure
                        // {in:Whisper}{i:1956}{s:"Whatever thing here"}{i:0}{i:34}{i:0}{i:-1}{i:1956}
                        String SaySomething = "The furni with ID "+ FurniID +" has been added successfully";
                        sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                    }
                }
                if(specificFurniList.contains(FurniID)){
                    String SaySomething = "You cant select this furni because its on the specific furni list!";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, UserID, SaySomething, 0, 34, 0, -1, UserID));
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            if(checkCoords.isSelected()){
                XCoord = hMessage.getPacket().readInteger();
                YCoord = hMessage.getPacket().readInteger();
                Platform.runLater(() -> checkCoords.setText("(" + XCoord + ", " + YCoord + ")"));
                hMessage.setBlocked(true);
                checkCoords.setSelected(false);
            }
            else if(checkSpecificPoint.isSelected()){
                XSpecificPoint = hMessage.getPacket().readInteger();
                YSpecificPoint = hMessage.getPacket().readInteger();
                Platform.runLater(() -> checkSpecificPoint.setText("(" + XSpecificPoint + ", " + YSpecificPoint + ")")); // Platform.exit();
                hMessage.setBlocked(true);
                checkSpecificPoint.setSelected(false);
            }
        });

        // Runs this instruction when the furni is added to the room
        intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::DoSomething);

        // Runs this instruction when the furni is moved
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::DoSomething);
    }

    private void DoSomething(HMessage hMessage) {
        if("---ON---".equals(buttonStart.getText())){
            FurniID = hMessage.getPacket().readInteger();
            int WithoutUse = hMessage.getPacket().readInteger();
            GetCoordX = hMessage.getPacket().readInteger();
            GetCoordY = hMessage.getPacket().readInteger();

            // A thread is created, this is necessary to avoid "Lagging" when its used the Thread.Sleep
            Thread t1 = new Thread(() -> {
                try {
                    Thread.sleep(Integer.parseInt(fieldDelay.getText())); // The time that the thread will sleep
                }catch (InterruptedException ignored){}

                if(specificFurniList.size() > 0){
                    if (specificFurniList.contains(FurniID)){ SitOnTheChair(); }
                }
                else { // When is equals to 0
                    if (!poisonFurniList.contains(FurniID)){ SitOnTheChair(); }
                }
            });
            t1.start(); // Thread started
        }
    }

    private void SitOnTheChair(){
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
        if(checkSpecificFurni.isSelected()){
            if(specificFurniList.contains(FurniID)){
                sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, GetCoordX, GetCoordY));
            }
        }
        if(checkAutodisable.isSelected()){
            Platform.runLater(() -> buttonStart.setText("---OFF---"));
        }
    }

    public void handleButtonStart(){
        if("---OFF---".equals(buttonStart.getText())){
            buttonStart.setText("---ON---");
        }
        else {
            buttonStart.setText("---OFF---");
        }
    }

    /*public synchronized void Hola(Thread thread){
        new Thread(() -> {
        });
        Thread currentThread = new Thread(()->{
                });
                currentThread.start();
                currentThread.join();
    }*/

    public void handleErasePoisons() {
        poisonFurniList.clear();
        Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + poisonFurniList.size() + ")"));
    }

    public void handleDeleteSpecific() {
        specificFurniList.clear();
        Platform.runLater(() -> checkSpecificFurni.setText("Specific Furnis (" + specificFurniList.size() + ")"));
    }
}