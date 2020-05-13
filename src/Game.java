import java.util.Map;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Game extends Application
{

    public static String trackerIp;
    public static int trackerPort;
    public static String playerId;

    private Stage primaryStage;
    private Grid grid;
    ObservableList<String> namesAndScores;

    public IntegerProperty intProperty;
    LogicLoop logic;

    public static void main(String[] args)
    {
        trackerIp = args[0];
        trackerPort = Integer.parseInt(args[1]);
        playerId = args[2];
        boolean validPlayerId = playerId.length() == 2;
        assert validPlayerId : "player-id " + playerId + " does not have 2 characters" + playerId.length();
        if (validPlayerId)
        {
            launch(args);
        }
    }

    @Override
    public void start(Stage primaryStage)
    {

        this.primaryStage = primaryStage;
        logic = new LogicLoop(this, trackerIp, trackerPort, playerId);
        
        intProperty = new SimpleIntegerProperty(this, "int", 0);
        intProperty.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(final ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(newValue.intValue()==1)
                        {
                          initView(logic.gridSize);
                        }
                        
                        updateView(logic.getGameState());
                    }
                });
            }

        });

        //to start a init GUI
        initializeGUI();

        // start thread to receive and process input from command line
        Thread logicThread = new Thread(logic);
        logicThread.start();

    }

    public void initView(int gridSize)
    {
        primaryStage.setTitle(playerId);

        double screenWidth = 800;
        double screenHeight = 400;

        final double TOP_OFFSET = 10;
        Group root = new Group();
        grid = new Grid(gridSize, gridSize, screenHeight, TOP_OFFSET, 5);
        root.getChildren().add(grid);
        grid.setTranslateX(screenWidth * 2 / 3);
        grid.setTranslateY(screenHeight / 2);

        ListView<String> leaderboard = new ListView<String>();
        final double LEADERBOARD_LEFT_OFFSET = TOP_OFFSET;
        leaderboard.setPrefWidth(screenWidth / 3 - 2 * LEADERBOARD_LEFT_OFFSET);
        leaderboard.setPrefHeight(screenHeight - 2 * TOP_OFFSET);
        root.getChildren().add(leaderboard);
        leaderboard.setTranslateX(LEADERBOARD_LEFT_OFFSET);
        leaderboard.setTranslateY(TOP_OFFSET);

        namesAndScores = FXCollections.observableArrayList();
        leaderboard.setItems(namesAndScores);

        Scene scene = new Scene(root, screenWidth, screenHeight);
        scene.setFill(Color.rgb(41, 34, 31));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void updateView(GameState gameState)
    {
        grid.update(gameState);

        namesAndScores.clear();
        for (Map.Entry<Integer, String> entry : gameState.playerNames.entrySet())
        {
            int playerKey = entry.getKey();
            int playerScore = gameState.playerScores.get(playerKey);
            String playerName = entry.getValue();
            if (playerKey == gameState.getPrimaryPlayerKey())
            {
                playerName += "(Primary Server)  ";
            } else if (playerKey == gameState.getBackupPlayerKey())
            {
                playerName += "(Backup Server)  ";
            } else
            {
                playerName += "     ";
            }
            namesAndScores.add(playerName + "score: "+playerScore);
        }
    }

    @Override
    public void stop()
    { // called when we quit the GUI client
        System.exit(0);
    }

    public void initializeGUI()
    {
        primaryStage.setTitle(playerId);

        double screenWidth = 800;
        double screenHeight = 400;

        final double TOP_OFFSET = 10;
        Group root = new Group();
        grid = new Grid(1, 1, screenHeight, TOP_OFFSET, 5);
        root.getChildren().add(grid);
        grid.setTranslateX(screenWidth * 2 / 3);
        grid.setTranslateY(screenHeight / 2);

        ListView<String> leaderboard = new ListView<String>();
        final double LEADERBOARD_LEFT_OFFSET = TOP_OFFSET;
        leaderboard.setPrefWidth(screenWidth / 3 - 2 * LEADERBOARD_LEFT_OFFSET);
        leaderboard.setPrefHeight(screenHeight - 2 * TOP_OFFSET);
        root.getChildren().add(leaderboard);
        leaderboard.setTranslateX(LEADERBOARD_LEFT_OFFSET);
        leaderboard.setTranslateY(TOP_OFFSET);

        namesAndScores = FXCollections.observableArrayList();
        leaderboard.setItems(namesAndScores);

        Scene scene = new Scene(root, screenWidth, screenHeight);
        scene.setFill(Color.rgb(41, 34, 31));
        primaryStage.setScene(scene);

        namesAndScores.add("Game Initializing. Please wait...");
        primaryStage.show();

    }
}
