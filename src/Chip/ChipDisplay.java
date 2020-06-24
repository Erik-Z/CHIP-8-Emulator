package Chip;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class ChipDisplay extends Application {
    int[] keyIdToKey;
    int[] keyBuffer;
    @Override
    public void start(Stage stage) throws Exception {
        keyIdToKey = new int[256];
        keyBuffer = new int[16];
        fillKeyIds();

        Chip8 chip = new Chip8();
        chip.init();
        //chip.loadProgram("./Programs/pong2.c8");

        VBox root = new VBox();
        Scene mainScene = new Scene(root);
        stage.setScene(mainScene);
        stage.setTitle("Chip-8 Emulator");

        root.getChildren().add(createMenuBar(stage, chip));

        Canvas canvas = new Canvas(960, 480);
        root.getChildren().add(canvas);

        keyHandlers(mainScene);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        Timeline loop = new Timeline();
        loop.setCycleCount( Timeline.INDEFINITE );
        KeyFrame kf = new KeyFrame(
            Duration.seconds(0.007),
            new EventHandler<ActionEvent>() {
                public void handle(ActionEvent ae)
                {
                    chip.setKeyBuffer(keyBuffer);
                    chip.run();
                    if(chip.needsRedraw()){
                        drawPixels(gc, chip);
                        chip.removeDrawFlag();
                    }
                }
            }
        );
        loop.getKeyFrames().add( kf );
        loop.play();
        stage.show();
    }

    private MenuBar createMenuBar(Stage stage, Chip8 chip){
        MenuBar menu_bar = new MenuBar();
        Menu file_menu = new Menu("File");
        MenuItem add_file = new MenuItem("Add File");

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("CHIP-8", "*.c8", "*.ch8");
        chooser.getExtensionFilters().add(filter);
        chooser.setInitialDirectory(new File("./Programs"));
        add_file.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                File file = chooser.showOpenDialog(stage);
                if (file != null) {
                    chip.init();
                    chip.loadProgram(file.toString());
                }
            }
        });

        Menu view_menu = new Menu("View");
        MenuItem view_memory = new Menu("View Memory");

        view_memory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String memory = "";
                for (int i = 0x000; i < 4096; i++){
                    if(i % 2 == 0){
                        memory = memory + String.format("%02X", (int) chip.getMemory()[i]);
                    } else {
                        memory = memory + String.format("%02X", (int) chip.getMemory()[i]) + " ";
                    }

                }
                TextArea textArea = new TextArea(memory);
                textArea.setWrapText(true);
                Scene memory_scene = new Scene(textArea,600, 400);

                Stage memory_window = new Stage();
                memory_window.setTitle("Memory");
                memory_window.setScene(memory_scene);
                memory_window.show();
            }
        });

        file_menu.getItems().add(add_file);
        view_menu.getItems().add(view_memory);
        menu_bar.getMenus().add(file_menu);
        menu_bar.getMenus().add(view_menu);
        return menu_bar;
    }

    private void drawPixels(GraphicsContext g, Chip8 chip){
        byte[] display = chip.getDisplay();
        for(int i = 0; i < display.length; i++) {
            if(display[i] == 0){
                g.setFill(Color.BLACK);
            } else {
                g.setFill(Color.WHITE);
            }
            int x = (i % 64);
            int y = (int)Math.floor(i / 64);
            g.fillRect(x * 15, y * 15, 15, 15);
        }
    }

    private void fillKeyIds() {
        for(int i = 0; i < keyIdToKey.length; i++) {
            keyIdToKey[i] = -1;
        }
        keyIdToKey['1'] = 1;
        keyIdToKey['2'] = 2;
        keyIdToKey['3'] = 3;
        keyIdToKey['q'] = 4;
        keyIdToKey['w'] = 5;
        keyIdToKey['e'] = 6;
        keyIdToKey['a'] = 7;
        keyIdToKey['s'] = 8;
        keyIdToKey['d'] = 9;
        keyIdToKey['z'] = 0xA;
        keyIdToKey['x'] = 0;
        keyIdToKey['c'] = 0xB;
        keyIdToKey['4'] = 0xC;
        keyIdToKey['r'] = 0xD;
        keyIdToKey['f'] = 0xE;
        keyIdToKey['v'] = 0xF;
    }

    private void keyHandlers(Scene mainScene){
        mainScene.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                char keyPressed = event.getText().charAt(0);
                if(keyIdToKey[keyPressed] != -1) {
                    keyBuffer[keyIdToKey[keyPressed]] = 1;
                }
            }
        });

        mainScene.setOnKeyReleased(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                char keyReleased = event.getText().charAt(0);
                if(keyIdToKey[keyReleased] != -1) {
                    keyBuffer[keyIdToKey[keyReleased]] = 0;
                }
            }
        });
    }
}
