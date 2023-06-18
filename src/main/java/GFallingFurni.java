import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.LogManager;


@ExtensionInfo(
        Title = "GFallingFurni",
        Description = "Classic extension, enjoy it!",
        Version = "1.2.4",
        Author = "Julianty"
)

public class GFallingFurni extends ExtensionForm implements NativeKeyListener {
    public ArrayList<Integer> listPoisonFurniture = new ArrayList<>();
    public ArrayList<Integer> listSpecificFurniture = new ArrayList<>();
    public ArrayList<HPoint> listEqualsCoords = new ArrayList<>();

    public ToggleGroup Mode;
    public TextField fieldDelay;
    public Button buttonStart, buttonDeleteSpecific;
    public CheckBox checkPoison, checkAutodisable, checkSpecificPoint, checkSpecificFurni;
    public RadioButton radioEqualsCoords, radioCurrent, radioSpecificPoint, radioCoordFurni, radioWalk;

    public HPoint walkTo;
    public String yourName;
    public int yourIndex = -1;
    public int userId, newXCoordFurni, newYCoordFurni, xSpecificPoint, ySpecificPoint;

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        if(NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()).equals("Ctrl")){
            Platform.runLater(() -> { buttonStart.setText("---ON---"); buttonStart.setTextFill(Color.GREEN); });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        if(NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()).equals("Ctrl")){
            Platform.runLater(this::turnOffButton);
        }
    }

    @Override
    protected void onShow() {
        sendToServer(new HPacket("{out:InfoRetrieve}"));    // When sent to the server, the client responds!
        sendToServer(new HPacket("{out:AvatarExpression}{i:0}"));   // With this it's not necessary to restart the room

        LogManager.getLogManager().reset();
        try {
            if(!GlobalScreen.isNativeHookRegistered()){
                GlobalScreen.registerNativeHook();
                System.out.println("Hook enabled");
            }
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }
        GlobalScreen.addNativeKeyListener(GFallingFurni.this);
    }

    @Override
    protected void onHide() {   // Runs this when the GUI is closed
        Platform.runLater(this::turnOffButton); // Platform.exit();
        listSpecificFurniture.clear(); listPoisonFurniture.clear();
        radioCurrent.setSelected(true);
        yourIndex = -1;

        try {
            GlobalScreen.unregisterNativeHook();
            System.out.println("Hook disabled");
        } catch (NativeHookException | RejectedExecutionException nativeHookException) {
            nativeHookException.printStackTrace();
        }
        GlobalScreen.removeNativeKeyListener(this);
    }


    @Override
    protected void initExtension() {
        Mode.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            RadioButton radioMode = (RadioButton) toggle.getToggleGroup().getSelectedToggle();
            String currentTxtRadio = radioMode.getText();
            if(currentTxtRadio.contains("Equals")){
                radioCoordFurni.setDisable(false);   radioWalk.setDisable(false);
            }
            else{
                radioCoordFurni.setDisable(true);    radioWalk.setDisable(true);
            }
        });

        radioCoordFurni.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if(radioCoordFurni.isSelected()){
                listEqualsCoords.clear();   radioCoordFurni.setText("CoordFurni (0)");
            }
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

        // Intercepts the client's response and does something ...
        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            // Be careful, the data must be obtained in the order of the packet
            userId = hMessage.getPacket().readInteger();
            yourName = hMessage.getPacket().readString();
        });

        // Response of packet AvatarExpression (gets userIndex)
        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            // First integer is index in room, second is animation id, i think
            if(primaryStage.isShowing() && yourIndex == -1){ // this could avoid any bug
                yourIndex = hMessage.getPacket().readInteger();
            }
        });

        intercept(HMessage.Direction.TOSERVER, "ClickFurni", this::methodOneorDoubleClick);
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", this::methodOneorDoubleClick);

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", hMessage -> {
            if(checkSpecificPoint.isSelected()){
                xSpecificPoint = hMessage.getPacket().readInteger();    ySpecificPoint = hMessage.getPacket().readInteger();
                Platform.runLater(() -> checkSpecificPoint.setText("(" + xSpecificPoint + ", " + ySpecificPoint + ")"));
                hMessage.setBlocked(true);
                checkSpecificPoint.setSelected(false);
            }
            else if(radioCoordFurni.isSelected() && !radioCoordFurni.isDisable()){
                listEqualsCoords.add(new HPoint(hMessage.getPacket().readInteger(), hMessage.getPacket().readInteger()));
                Platform.runLater(() -> radioCoordFurni.setText("CoordFurni (" + listEqualsCoords.size() + ")"));
                hMessage.setBlocked(true);
            }
            else if(radioWalk.isSelected() && !radioWalk.isDisable()){
                walkTo = new HPoint(hMessage.getPacket().readInteger(), hMessage.getPacket().readInteger());
                Platform.runLater(() -> radioWalk.setText("Walk to (" + walkTo.getX() + ", " + walkTo.getY() + ")"));
                radioWalk.setSelected(false);   hMessage.setBlocked(true);
            }
        });

        // Intercept this packet when any user enters the room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity: roomUsersList){
                    if(hEntity.getName().equals(yourName)){    // In another room, the index changes
                        yourIndex = hEntity.getIndex();
                    }
                }
            } catch (Exception ignored) { }
        });

        // Intercepts when users in the room move
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
                try {
                    int CurrentIndex = hEntityUpdate.getIndex();
                    if(yourIndex == CurrentIndex){
                        if(checkAutodisable.isSelected()){
                            HPoint currentHPoint = new HPoint(hEntityUpdate.getMovingTo().getX(), hEntityUpdate.getMovingTo().getY());

                            if((newXCoordFurni == currentHPoint.getX() && newYCoordFurni == currentHPoint.getY()) ||
                                    (walkTo.getX() == currentHPoint.getX() && walkTo.getY() == currentHPoint.getY()) ||
                                    (xSpecificPoint == currentHPoint.getX() && ySpecificPoint == currentHPoint.getY())){
                                Platform.runLater(this::turnOffButton);
                                break;
                            }
                        }
                    }
                }
                catch (Exception ignored) { }
            }
        });

        // Runs this instruction when the furni is added to the room
        intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::DoSomething);

        // Runs this instruction when the furni is moved
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::DoSomething);

        // Intercepts when a furni is moved from one place to another with wired!
        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", hMessage -> {
            if("---ON---".equals(buttonStart.getText())){
                int oldX = hMessage.getPacket().readInteger();  int oldY = hMessage.getPacket().readInteger();
                newXCoordFurni = hMessage.getPacket().readInteger();    newYCoordFurni = hMessage.getPacket().readInteger();
                int NotUse = hMessage.getPacket().readInteger();
                int furnitureId = hMessage.getPacket().readInteger();

                threadToSleep(furnitureId);
            }
        });
    }

    private void methodOneorDoubleClick(HMessage hMessage) {
        int furniId = hMessage.getPacket().readInteger();
        if(checkSpecificFurni.isSelected()){
            if(!listPoisonFurniture.contains(furniId)){
                if(!listSpecificFurniture.contains(furniId)){
                    listSpecificFurniture.add(furniId);
                    Platform.runLater(() -> checkSpecificFurni.setText("Specific Furnis (" + listSpecificFurniture.size() + ")"));
                    String SaySomething = "The furni has been added successfully";
                    // Packet Structure: {in:Whisper}{i:1956}{s:"Whatever thing here"}{i:0}{i:34}{i:0}{i:-1}{i:1956}
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
                }
            }
            else{
                String SaySomething = "You cant select this furni because its on the poison list!";
                sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
            }
        }
        if(checkPoison.isSelected()){
            if(!listSpecificFurniture.contains(furniId)){
                if(!listPoisonFurniture.contains(furniId)){
                    listPoisonFurniture.add(furniId);
                    Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + listPoisonFurniture.size() + ")"));
                    String SaySomething = "The furni with ID "+ furniId +" has been added successfully";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
                }
            }
            else{
                String SaySomething = "You cant select this furni because its on the specific furni list!";
                sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
            }
        }
    }

    private void DoSomething(HMessage hMessage) {
        if("---ON---".equals(buttonStart.getText())){
            int furnitureId = hMessage.getPacket().readInteger();   int withOutUse = hMessage.getPacket().readInteger();
            newXCoordFurni = hMessage.getPacket().readInteger();    newYCoordFurni = hMessage.getPacket().readInteger();

            threadToSleep(furnitureId);
        }
    }

    private void threadToSleep(int furnitureId){
        // A thread is created, this is necessary to avoid "Lagging" when its used the Thread.Sleep
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(Integer.parseInt(fieldDelay.getText())); // The time that the thread will sleep
            }catch (InterruptedException ignored){}

            if(listSpecificFurniture.size() > 0){
                if (listSpecificFurniture.contains(furnitureId)){ SitOnTheChair(); }
            }
            else { // When is equals to 0
                if (!listPoisonFurniture.contains(furnitureId)){ SitOnTheChair(); }
            }
        });
        t1.start(); // Thread started
    }

    private void SitOnTheChair(){
        RadioButton radioCurrent = (RadioButton) Mode.getSelectedToggle();
        String txtRadio = radioCurrent.getText();

        if(txtRadio.contains("Current")){
            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, newXCoordFurni, newYCoordFurni));
        }
        else if(txtRadio.contains("Equals")){
            for(HPoint equalsCoords: listEqualsCoords){
                if(newXCoordFurni == equalsCoords.getX() && newYCoordFurni == equalsCoords.getY()){
                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, walkTo.getX(), walkTo.getY()));
                    break;
                }
            }
        }
        else if(txtRadio.contains("Specific")){
            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xSpecificPoint, ySpecificPoint));
        }
    }

    public void handleButtonStart(){
        if("---OFF---".equals(buttonStart.getText())){
            buttonStart.setText("---ON---");    buttonStart.setTextFill(Color.GREEN);
        }
        else {
            turnOffButton();
        }
    }

    public void turnOffButton(){
        buttonStart.setText("---OFF---"); buttonStart.setTextFill(Color.RED);
    }

    public void handleErasePoisons() {
        listPoisonFurniture.clear();
        Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + listPoisonFurniture.size() + ")"));
    }

    public void handleDeleteSpecific() {
        listSpecificFurniture.clear();
        Platform.runLater(() -> checkSpecificFurni.setText("Specific Furnis (" + listSpecificFurniture.size() + ")"));
    }
}

/*
  Logic in table, for better understanding
  Example:

  | Coord Furni   | Walk To       |
  |---------------|---------------|
  | AnyTile       | Coord Furni   | Custom Mode
  | SpecificTile  | SpecificTile  | Equals Mode - mix here
  | AnyTile       | SpecificTile  | Specific Point Mode
*/
