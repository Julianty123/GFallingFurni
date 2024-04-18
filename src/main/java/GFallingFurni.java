import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import javafx.stage.Stage;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.LogManager;

@ExtensionInfo(
        Title = "GFallingFurni",
        Description = "Classic extension, enjoy it!",
        Version = "1.3.0",
        Author = "Julianty"
)

public class GFallingFurni extends ExtensionForm implements NativeKeyListener {

    public AnchorPane anchorPane;
    public RadioButton radioDiagonal;
    public CheckBox checkListEqual;
    public ListView<HPoint> listViewEquals;
    public Label labelStatus;
    public ToggleGroup Mode, Em;
    public TextField fieldDelay;
    public Button buttonStart, buttonDeleteSpecific;
    public CheckBox checkSquare, checkSquareTo, checkPoison, checkAutoDisable, checkSpecificPoint, cbSpecificFurniture;
    public RadioButton radioVertical, radioHorizontal, radioTriangle, radioSquare, radioSquareTo,
            radioEqualsCoords, radioCurrent, radioSpecificPoint, radioFree, radioFree_WalkTo;

    public HPoint hPointFreeWalkTo = new HPoint(-1, -1);
    public HPoint hPointSquareTo = new HPoint(-1, -1);
    public String yourName;
    public int yourIndex = -1;
    public int userId, xFurniture, yFurniture, xSpecificPoint, ySpecificPoint;

    public ArrayList<String> squareSelected = new ArrayList<>();
    public HashSet<Integer> listPoisonFurniture = new HashSet<>(); // HashSet dont allow duplicates elements
    public HashSet<Integer> listSpecificFurniture = new HashSet<>();
    public HPoint startSquare = new HPoint(-1, -1);
    public HPoint endSquare = new HPoint(-1, -1);
    private double xFrame, yFrame;

    private static final HashMap<String, String> hostToDomain = new HashMap<>();
    static {
        hostToDomain.put("game-es.habbo.com", "https://www.habbo.es/gamedata/furnidata_json/1");
        hostToDomain.put("game-br.habbo.com", "https://www.habbo.com.br/gamedata/furnidata_json/1");
        hostToDomain.put("game-tr.habbo.com", "https://www.habbo.com.tr/gamedata/furnidata_json/1");
        hostToDomain.put("game-us.habbo.com", "https://www.habbo.com/gamedata/furnidata_json/1");
        hostToDomain.put("game-de.habbo.com", "https://www.habbo.de/gamedata/furnidata_json/1");
        hostToDomain.put("game-fi.habbo.com", "https://www.habbo.fi/gamedata/furnidata_json/1");
        hostToDomain.put("game-fr.habbo.com", "https://www.habbo.fr/gamedata/furnidata_json/1");
        hostToDomain.put("game-it.habbo.com", "https://www.habbo.it/gamedata/furnidata_json/1");
        hostToDomain.put("game-nl.habbo.com", "https://www.habbo.nl/gamedata/furnidata_json/1");
        hostToDomain.put("game-s2.habbo.com", "https://sandbox.habbo.com/gamedata/furnidata_json/1");
    }

    private static final HashMap<String, Integer> mapPoisonClassnameToUniqueId = new HashMap<>();
    static {
        mapPoisonClassnameToUniqueId.put("hween13_tile1", -1);  // Teleport pica roja
        mapPoisonClassnameToUniqueId.put("hween13_tile2", -1);  // Teleport pica negra
        mapPoisonClassnameToUniqueId.put("bb_rnd_tele", -1); // Teleport banzai
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        int key = nativeKeyEvent.getKeyCode();  // System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(key));
        if(NativeKeyEvent.getKeyText(key).equals("Ctrl")){ // Mayús Ctrl
            Platform.runLater(() -> { buttonStart.setText("---ON---"); buttonStart.setTextFill(Color.GREEN); });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        int key = nativeKeyEvent.getKeyCode();  // System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(key));
        if(NativeKeyEvent.getKeyText(key).equals("Ctrl")){ // Mayús Ctrl
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


    public int tilesX;
    public int tilesY;

    @Override
    protected void initExtension() {
        onConnect((host, port, APIVersion, versionClient, client) -> getGameData(host)); // Example: host = game-fr.habbo.com

        Mode.selectedToggleProperty().addListener((observableValue, oldToggle, newToggle) -> {
            RadioButton radioMode = (RadioButton) Mode.getSelectedToggle();

            // i need to found way for avoid do this
            if(radioMode == null) {
                System.out.println("radioMode is null");    return;
            }
            String currentTxtRadio = radioMode.getText();

            radioDiagonal.setDisable(!currentTxtRadio.contains("Equals"));
            radioVertical.setDisable(!currentTxtRadio.contains("Equals"));
            radioHorizontal.setDisable(!currentTxtRadio.contains("Equals"));
            radioTriangle.setDisable(!currentTxtRadio.contains("Equals"));
            radioFree.setDisable(!currentTxtRadio.contains("Equals"));
            radioFree_WalkTo.setDisable(!currentTxtRadio.contains("Equals"));
        });

        radioFree_WalkTo.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            if(newValue) checkListEqual.setSelected(false);
        });

        radioFree.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            if(newValue) checkListEqual.setSelected(true);
        });

        // Runs when the text field changes!
        fieldDelay.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Integer.parseInt(fieldDelay.getText());
            } catch (NumberFormatException e) {
                if(fieldDelay.getText().isEmpty()) fieldDelay.setText("1");
                else fieldDelay.setText(oldValue);
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "HeightMap",  hMsg -> tilesX = hMsg.getPacket().readInteger()); // Number of tiles
        intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::handleFloorHeightMap);
        intercept(HMessage.Direction.TOCLIENT, "UserObject", this::interceptUserObject); // When InfoRetrieve is sent to the server
        intercept(HMessage.Direction.TOCLIENT, "Expression", this::interceptExpression); // Response of packet AvatarExpression

        intercept(HMessage.Direction.TOSERVER, "ClickFurni", this::methodOneOrDoubleClick);
        intercept(HMessage.Direction.TOSERVER, "UseFurniture", this::methodOneOrDoubleClick);

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::interceptMoveAvatar);
        intercept(HMessage.Direction.TOCLIENT, "Users", this::interceptUsers); // Intercept this packet when any user enters the room
        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::interceptUserUpdate); // Intercepts when users walking in the room
        intercept(HMessage.Direction.TOCLIENT, "Objects", this::handleObjects);

        intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::doSomething); // Intercepts when the furniture is added
        intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::doSomething); // Intercepts when the furniture is moved

        intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::interceptSlideObjectBundle); // Intercepts when the furniture is moved with wired
    }

    private void interceptSlideObjectBundle(HMessage hMessage) {
        if("---ON---".equals(buttonStart.getText())){
            int oldX = hMessage.getPacket().readInteger();  int oldY = hMessage.getPacket().readInteger();
            xFurniture = hMessage.getPacket().readInteger();    yFurniture = hMessage.getPacket().readInteger();
            int NotUse = hMessage.getPacket().readInteger();
            int furnitureId = hMessage.getPacket().readInteger();

            if (listSpecificFurniture.contains(furnitureId)) sitOnTheChair();
            else if (!listPoisonFurniture.contains(furnitureId)) sitOnTheChair();
        }
    }

    private void interceptMoveAvatar(HMessage hMessage) {
        int x = hMessage.getPacket().readInteger();
        int y = hMessage.getPacket().readInteger();

        if(checkSpecificPoint.isSelected()){
            xSpecificPoint = x;    ySpecificPoint = y;
            Platform.runLater(() -> checkSpecificPoint.setText("(" + xSpecificPoint + ", " + ySpecificPoint + ")"));
            hMessage.setBlocked(true);
            checkSpecificPoint.setSelected(false);
        }
        if(checkPoison.isSelected()) hMessage.setBlocked(true);

        if(checkSquare.isSelected()){
            if(!startSquare.toString().equals("(-1,-1,0.0)") && !endSquare.toString().equals("(-1,-1,0.0)"))
                startSquare = new HPoint(-1, -1);	endSquare = new HPoint(-1, -1);

            if(startSquare.getX() == -1 && startSquare.getY() == -1) startSquare = new HPoint(x, y);
            else if(endSquare.getX() == -1 && endSquare.getY() == -1){
                endSquare = new HPoint(x, y);

                squareSelected.clear();
                for(int yCoord = startSquare.getY(); yCoord <= endSquare.getY(); yCoord++){
                    for(int xCoord = startSquare.getX(); xCoord <= endSquare.getX(); xCoord++){
                        // if(walkableTiles.contains(new HPoint(xCoord, yCoord)))
                        squareSelected.add(new HPoint(xCoord, yCoord).toString());
                    }
                }
                checkSquare.setSelected(false);
            }

            Platform.runLater(() -> checkSquare.setText(String.format("(%d, %d) - (%d, %d)",
                    startSquare.getX(), startSquare.getY(), endSquare.getX(), endSquare.getY())));
            hMessage.setBlocked(true);
        }
        else if(checkSquareTo.isSelected()){
            hPointSquareTo = new HPoint(x, y);
            Platform.runLater(() -> checkSquareTo.setText("Walk to (" + hPointSquareTo.getX() + ", " + hPointSquareTo.getY() + ")"));
            checkSquareTo.setSelected(false);  hMessage.setBlocked(true);
        }
        else if(radioFree.isSelected() && checkListEqual.isSelected()){
            if(checkListEqual.isSelected()){
                if(!listViewEquals.getItems().contains(new HPoint(x, y))) {
                    Platform.runLater(()-> listViewEquals.getItems().add(new HPoint(x, y)));
                }
                hMessage.setBlocked(true);
            }
        }
        else if(radioFree_WalkTo.isSelected() && !radioFree_WalkTo.isDisable()){
            hPointFreeWalkTo = new HPoint(x, y);
            Platform.runLater(() -> radioFree_WalkTo.setText("Walk to (" + hPointFreeWalkTo.getX() + ", " + hPointFreeWalkTo.getY() + ")"));
            radioFree_WalkTo.setSelected(false);   hMessage.setBlocked(true);
        }
        else if(radioDiagonal.isSelected() && checkListEqual.isSelected()){
            ArrayList<HPoint> listEqualsCoords = new ArrayList<>();

            // Diagonal izquierda abajo
            int xReference = x;
            int yReference = y;
            for(int i = xReference; i >= 0; i--) {
                if(!listViewEquals.getItems().contains(new HPoint(i, yReference)))
                    listEqualsCoords.add(new HPoint(i, yReference));

                yReference = yReference + 1;
            }

            // Diagonal derecha arriba
            xReference = x;
            yReference = y;
            for(int i = xReference; i < tilesX; i++) {
                if(!listViewEquals.getItems().contains(new HPoint(i, yReference)))
                    listEqualsCoords.add(new HPoint(i, yReference));

                yReference = yReference - 1;
            }

            // Diagonal izquierda arriba
            xReference = x;
            yReference = y;
            for(int i = xReference; i >= 0; i--) {
                if(!listViewEquals.getItems().contains(new HPoint(i, yReference)))
                    listEqualsCoords.add(new HPoint(i, yReference));

                yReference = yReference - 1;
            }

            // Diagonal derecha abajo
            xReference = x;
            yReference = y;
            for(int i = xReference; i < tilesX; i++) {
                if(!listViewEquals.getItems().contains(new HPoint(i, yReference)))
                    listEqualsCoords.add(new HPoint(i, yReference));

                yReference = yReference + 1;
            }

            Platform.runLater(()-> listViewEquals.getItems().addAll(listEqualsCoords));
            hMessage.setBlocked(true);  checkListEqual.setSelected(false);
        }
        else if(radioVertical.isSelected() && checkListEqual.isSelected()){ // currentRadioEqualsCoords.getText().equals("Vertical Line")
            ArrayList<HPoint> listEqualsCoords = new ArrayList<>();
            for(int i = 0; i < tilesY; i++) {
                if(!listViewEquals.getItems().contains(new HPoint(x, i))) listEqualsCoords.add(new HPoint(x, i));
            }
            Platform.runLater(()-> listViewEquals.getItems().addAll(listEqualsCoords));

            hMessage.setBlocked(true); checkListEqual.setSelected(false);
        }
        else if(radioHorizontal.isSelected() && checkListEqual.isSelected()){
            ArrayList<HPoint> listEqualsCoords = new ArrayList<>();
            for(int i = 0; i < tilesX; i++) {
                if(!listViewEquals.getItems().contains(new HPoint(i, y))) listEqualsCoords.add(new HPoint(i, y));
            }
            Platform.runLater(()-> listViewEquals.getItems().addAll(listEqualsCoords));

            hMessage.setBlocked(true); checkListEqual.setSelected(false);
        }
        else if(radioTriangle.isSelected() && checkListEqual.isSelected()){
            if(!listViewEquals.getItems().contains(new HPoint(x, y))){
                Platform.runLater(()-> listViewEquals.getItems().add(new HPoint(x, y)));
            }

            hMessage.setBlocked(true); checkListEqual.setSelected(false);
        }
    }

    private void interceptUsers(HMessage hMessage) {
        try {
            HPacket hPacket = hMessage.getPacket();
            HEntity[] roomUsersList = HEntity.parse(hPacket);
            for (HEntity hEntity: roomUsersList){
                if(hEntity.getName().equals(yourName)) yourIndex = hEntity.getIndex(); // In another room, the index changes
            }
        } catch (Exception ignored) { }
    }

    private void interceptUserUpdate(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        for (HEntityUpdate hEntityUpdate: HEntityUpdate.parse(hPacket)){
            try {
                int currentIndex = hEntityUpdate.getIndex();
                // if(yourIndex == currentIndex && checkAutoDisable.isSelected())
                if(yourIndex != currentIndex || !checkAutoDisable.isSelected()) continue;
                HPoint currentHPoint = new HPoint(hEntityUpdate.getMovingTo().getX(), hEntityUpdate.getMovingTo().getY());

                if((xFurniture == currentHPoint.getX() && yFurniture == currentHPoint.getY()) ||
                        (hPointFreeWalkTo.getX() == currentHPoint.getX() && hPointFreeWalkTo.getY() == currentHPoint.getY()) ||
                        (xSpecificPoint == currentHPoint.getX() && ySpecificPoint == currentHPoint.getY())){
                    Platform.runLater(this::turnOffButton);
                    break;
                }
            }
            catch (Exception ignored) { }
        }
    }

    private void interceptExpression(HMessage hMessage) {
        // first integer: your index in room, second: animation id
        if(primaryStage.isShowing() && yourIndex == -1)  // To avoid any bug
            yourIndex = hMessage.getPacket().readInteger();
    }

    private void interceptUserObject(HMessage hMessage) {
        userId = hMessage.getPacket().readInteger();    yourName = hMessage.getPacket().readString();
    }

    private void handleFloorHeightMap(HMessage hMessage){
        boolean idk1 = hMessage.getPacket().readBoolean();  int idk2 = hMessage.getPacket().readInteger();
        String map = hMessage.getPacket().readString();
        String[] floorRows = map.split("\\r");
        tilesY = floorRows.length;
    }

    private void handleObjects(HMessage hMessage){
        HPacket packet = hMessage.getPacket();
        for (HFloorItem hFloorItem: HFloorItem.parse(packet)){
            try{
                if(mapPoisonClassnameToUniqueId.containsValue(hFloorItem.getTypeId())) listPoisonFurniture.add(hFloorItem.getId());
            }catch (Exception ignored){}
        }

        Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + listPoisonFurniture.size() + ")"));
    }

    private void getGameData(String host){
        new Thread(() -> {
            try{
                String url = hostToDomain.get(host); // "https://assets.habboon.pw/nitro//gamedata/FurnitureData.json";
                System.out.println("Getting game-data from: " + url);
                URLConnection connection = (new URL(url)).openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                connection.connect();
                JSONObject object = new JSONObject(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));

                JSONArray floorJson = object.getJSONObject("roomitemtypes").getJSONArray("furnitype");
                floorJson.forEach(o -> {
                    JSONObject item = (JSONObject)o;
                    int id = item.getInt("id"); // typeId or UniqueId
                    String classname = item.getString("classname");

                    // replace -1 with the real id
                    if(mapPoisonClassnameToUniqueId.containsKey(classname)) mapPoisonClassnameToUniqueId.put(classname, id);
                });

                Platform.runLater(()-> labelStatus.setText("Connected to: " + url));
                sendToServer(new HPacket("{out:GetHeightMap}")); // Get Objects, Items, etc. Without restart the room

            }catch (Exception e){
                Platform.runLater(()-> labelStatus.setText("Connected to: " + e.getMessage()));
            }

            anchorPane.setDisable(false);
        }).start();
    }

    private void methodOneOrDoubleClick(HMessage hMessage) {
        int furnitureId = hMessage.getPacket().readInteger();
        if(cbSpecificFurniture.isSelected()){
            if(!listPoisonFurniture.contains(furnitureId)){
                if(!listSpecificFurniture.contains(furnitureId)){
                    listSpecificFurniture.add(furnitureId);
                    Platform.runLater(() -> cbSpecificFurniture.setText("Specific Furnis (" + listSpecificFurniture.size() + ")"));
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
            if(!listSpecificFurniture.contains(furnitureId)){
                if(!listPoisonFurniture.contains(furnitureId)){
                    listPoisonFurniture.add(furnitureId);
                    Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + listPoisonFurniture.size() + ")"));
                    String SaySomething = "The furni with ID "+ furnitureId +" has been added successfully";
                    sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
                }
            }
            else{
                String SaySomething = "You cant select this furni because its on the specific furni list!";
                sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, userId, SaySomething, 0, 34, 0, -1, userId));
            }
        }
    }

    private void doSomething(HMessage hMessage) {
        int furnitureId = hMessage.getPacket().readInteger();   int uniqueId = hMessage.getPacket().readInteger();
        if(mapPoisonClassnameToUniqueId.containsValue(uniqueId)) listPoisonFurniture.add(furnitureId);
        Platform.runLater(() -> checkPoison.setText("Poison Furnis (" + listPoisonFurniture.size() + ")"));

        if("---ON---".equals(buttonStart.getText())){
            xFurniture = hMessage.getPacket().readInteger();    yFurniture = hMessage.getPacket().readInteger();

            if (listSpecificFurniture.contains(furnitureId)) sitOnTheChair();
            else if (!listPoisonFurniture.contains(furnitureId)) sitOnTheChair();
        }
    }

    private void sitOnTheChair(){
        // A thread is created, this is necessary to avoid "Lagging" when its used the Thread.Sleep()
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(Integer.parseInt(fieldDelay.getText())); // The time that the thread will sleep
            }catch (InterruptedException ignored){}

            RadioButton radioCurrent = (RadioButton) Mode.getSelectedToggle();
            String txtRadio = radioCurrent.getText();

            if(txtRadio.equals("Normal")){
                sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xFurniture, yFurniture));
            }
            else if(txtRadio.contains("Equals")){
                for(HPoint equalsCoords: listViewEquals.getItems()){
                    if(xFurniture == equalsCoords.getX() && yFurniture == equalsCoords.getY()){
                        if(radioFree.isSelected()){
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, hPointFreeWalkTo.getX(), hPointFreeWalkTo.getY()));
                        }
                        else{ // Horizontal, vertical or Triangle, i think
                            sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xFurniture, yFurniture));
                        }
                        break;
                    }
                }
            }
            else if(txtRadio.equals("Specific")){
                sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xSpecificPoint, ySpecificPoint));
            }
            else if(txtRadio.equals("Square")){
                if(squareSelected.contains(new HPoint(xFurniture, yFurniture).toString())){
                    sendToServer(new HPacket("MoveAvatar", HMessage.Direction.TOSERVER, xFurniture, yFurniture));
                }
            } else if(txtRadio.equals("SquareTo")) {
                if(squareSelected.contains(new HPoint(xFurniture, yFurniture).toString())){
                    sendToServer(new HPacket("MoveAvatar",
                            HMessage.Direction.TOSERVER, hPointSquareTo.getX(), hPointSquareTo.getY()));
                }
            }
        });
        t1.start(); // Thread started
    }

    public void handleButtonStart(){
        if("---OFF---".equals(buttonStart.getText())){
            buttonStart.setText("---ON---");    buttonStart.setTextFill(Color.GREEN);
        }
        else turnOffButton();
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
        Platform.runLater(() -> cbSpecificFurniture.setText("Specific Furnis (" + listSpecificFurniture.size() + ")"));
    }

    public void handleEraseEqualsCoords(ActionEvent actionEvent) {
        listViewEquals.getItems().clear();
    }

    public void onMinimize(ActionEvent event) {
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    public void onClose(ActionEvent event) {
        primaryStage.close();
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        Stage stage = (Stage) ((Node)mouseEvent.getSource()).getScene().getWindow();
        stage.setX(mouseEvent.getScreenX() - xFrame);
        stage.setY(mouseEvent.getScreenY() - yFrame);
    }

    public void onMousePressed(MouseEvent mouseEvent) {
        xFrame = mouseEvent.getSceneX();
        yFrame = mouseEvent.getSceneY();
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