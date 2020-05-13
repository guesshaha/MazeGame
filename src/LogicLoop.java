import javafx.application.Platform;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogicLoop implements Runnable
{

    private Game game;
    private String myPlayerId; // Two characters (e.g. "DY")
    private Integer myPlayerKey = null;

    private GameNode gameNode;
    private PingThread myPingThread;

    private int myPort = 0;
    public int gridSize = 0;

    private String trackerIp;
    private int trackerPort;

    public GameState getGameState()
    {
        return gameNode.gameState;
    }

    public LogicLoop(Game game, String trackerIp, int trackerPort, String myPlayerId)
    {
        this.game = game;
        this.myPlayerId = myPlayerId;
        this.trackerIp = trackerIp;
        this.trackerPort = trackerPort;
    }

    public void init()
    {
        GameState gameState = null;
        boolean assignedPlayerKey = false;
        TrackerInfo tInfo = null;
        int numPlayers = 0;
        GameNodeInterface firstContact = null;
        int count;

        try
        {
            //init gameNode first
            gameNode = new GameNode();
        } catch (RemoteException ex)
        {
            printWithSourceCodeDetailUtl.println("RemoteException:" + ex.toString());
        }

        //get info from tracker
        //should be always successful
        try
        { // look for tracker
            String TRACKER_REGISTRATION = "rmi://" + trackerIp + ":" + trackerPort + "/tracker";
            // Lookup the service in the registry, and obtain a remote service
            Remote remoteService = Naming.lookup(TRACKER_REGISTRATION);

            TrackerInterface tracker = (TrackerInterface) remoteService;
            tInfo = tracker.PlayerJoinReqHandler(myPlayerId, trackerIp, String.valueOf(myPort), gameNode);
            //End the program if entering same playerID
            if(tInfo == null) {
                System.out.println("Duplicate ID, exiting.");
                game.stop();
            }
            HashMap<Integer, String> playerIdMap = tInfo.playerIdMap;
            numPlayers = 0;
            for (Integer playerKey : playerIdMap.keySet())
            {
                numPlayers++;
                String playerId = playerIdMap.get(playerKey);
                if (playerId.equals(myPlayerId))
                {
                    myPlayerKey = playerKey;
                }
            }
            myPort = tInfo.playerPortMap.get(myPlayerKey);
            gameNode.setPlayerKey(myPlayerKey);

            assignedPlayerKey = myPlayerKey != null;
            assert assignedPlayerKey : "Was not assigned a player key";
        } catch (NotBoundException nbe)
        {
            printWithSourceCodeDetailUtl.println("No service available in registry!");
        } catch (RemoteException re)
        {
            printWithSourceCodeDetailUtl.println("RMI Error - " + re);
        } catch (Exception e)
        {
            printWithSourceCodeDetailUtl.println("Error - " + e);
        }

        if (assignedPlayerKey && tInfo != null && numPlayers > 0)
        {
            gridSize = tInfo.getSizeN();
            System.out.println("Success! Welcome, " + myPlayerId + ". Your player key is " + myPlayerKey + ".");
            // get the first player's ip address. May or may not be primary server!!!

            for (Map.Entry<Integer, String> entry : tInfo.playerIdMap.entrySet())
            {
                if (entry.getKey() == myPlayerKey)
                {// Primary server
                    gridSize = tInfo.getSizeN();
                    System.out.println("Success! Welcome, " + myPlayerId + ". Your player key is " + myPlayerKey + ".");
                    System.out.println("You're the primary server.");
                    gameState = createInitialGameState(gridSize, tInfo.getNumK(), myPlayerKey, myPlayerId, trackerIp, myPort, tInfo.playerStubMap.get(entry.getKey()));
                    break;
                }

                for (count = 0; count < 3; count++)
                {
                    GameNodeInterface firstStub = tInfo.playerStubMap.get(entry.getKey());
                    if (firstStub != null)
                    {
                        try
                        {
                            gameState = firstStub.getGameState();
                            break;
                        } catch (Exception e)
                        {
                            printWithSourceCodeDetailUtl.println("Error - " + e);
                        }
//                        break;
                    }
                    //delay 1s
                    try
                    {
                        Thread.sleep(1);
                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(LogicLoop.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(gameState!=null)
                    break;
                else
                {//inform tracker to remove the node
                    gameNode.sendCrashNodeToTracker(entry.getKey());
                }
            }
        }

        //if not PServer, contack PServer to join game
        //may not be successful, as PServer may just start and have not start his RMI server, retry 3 times
        //may not be successful, PServer may just crash, need to ask BServer
        if (assignedPlayerKey && tInfo != null && gameState.getPrimaryPlayerKey() != myPlayerKey && gameState != null)
        {
            boolean joinGameOK = false;
            for (count = 0; count < 3; count++)
            {
                try
                {
                    GameNodeInterface tempStub = gameState.playerStub.get(gameState.getPrimaryPlayerKey());
                    gameState = tempStub.joinGame(myPlayerKey, myPlayerId, trackerIp, myPort, tInfo.playerStubMap.get(myPlayerKey));
                    joinGameOK = true;
                    break;
                } catch (RemoteException re)
                {
                    printWithSourceCodeDetailUtl.println("RMI Error - " + re);
                } catch (Exception e)
                {
                    printWithSourceCodeDetailUtl.println("Error - " + e);
                }
                //delay 1s
                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException ex)
                {
                    Logger.getLogger(LogicLoop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            //PServer may just crash, need to ask BServer
            if (joinGameOK == false)
            {
                for (count = 0; count < 3; count++)
                {
                    try
                    {
                        GameNodeInterface tempStub = gameState.playerStub.get(gameState.getBackupPlayerKey());
                        gameState = tempStub.joinGame(myPlayerKey, myPlayerId, trackerIp, myPort, tInfo.playerStubMap.get(myPlayerKey));
                        joinGameOK = true;
                        break;
                    } catch (RemoteException re)
                    {
                        printWithSourceCodeDetailUtl.println("RMI Error - " + re);
                    } catch (Exception e)
                    {
                        printWithSourceCodeDetailUtl.println("Error - " + e);
                    }
                    //delay 1s
                    try
                    {
                        Thread.sleep(1);
                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(LogicLoop.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }

        gameNode.setGameState(gameState);

//        game.initView(gridSize);
//        updateGameView(game, gameNode.gameState);
        game.intProperty.set(1);

        //start Ping thread
        myPingThread = new PingThread(gameNode.gameState);
        myPingThread.start();
    }

    // Should only be called by primary server!!!
    private static GameState createInitialGameState(int gridSize, int numTreasures, Integer playerKey, String playerId, String playerIpAddress, Integer playerPort, GameNodeInterface gameNode)
    {
        AtomicInteger[][] grid = new AtomicInteger[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++)
        {
            for (int j = 0; j < gridSize; j++)
            {
                //grid[i][j].set(GameState.EMPTY_ID);
                grid[i][j] = new AtomicInteger(GameState.EMPTY_ID);
            }
        }
        GameState.V2 playerPos = GameState.getRandomUnoccupied(grid, gridSize);
        grid[playerPos.y][playerPos.x].set(playerKey);

        for (int i = 0; i < numTreasures; i++)
        {
            GameState.V2 treasurePos = GameState.getRandomUnoccupied(grid, gridSize);
            grid[treasurePos.y][treasurePos.x].set(GameState.TREASURE_ID);
        }

        HashMap<Integer, String> playerNames = new HashMap<Integer, String>();
        playerNames.put(playerKey, playerId);

        HashMap<Integer, String> playerIpAddresses = new HashMap<Integer, String>();
        playerIpAddresses.put(playerKey, playerIpAddress);

        HashMap<Integer, Integer> playerPorts = new HashMap<Integer, Integer>();
        playerPorts.put(playerKey, playerPort);

        HashMap<Integer, Integer> playerScores = new HashMap<Integer, Integer>();
        playerScores.put(playerKey, 0);

        HashMap<Integer, GameNodeInterface> playerStubs = new HashMap<Integer, GameNodeInterface>();
        playerStubs.put(playerKey, gameNode);

        GameState gameState = new GameState(new AtomicInteger(playerKey), null, grid, playerNames, playerIpAddresses, playerPorts, playerScores, playerStubs, new AtomicInteger(0));
        return gameState;
    }

    private static String getPlayerRegistration(TrackerInfo tInfo, Integer playerKey)
    {
        String registration = null;
        String playerId = tInfo.playerIdMap.get(playerKey);
        String playerIpAdd = tInfo.playerIPaddMap.get(playerKey);
        Integer playerPort = tInfo.playerPortMap.get(playerKey);
        if (playerId != null && playerIpAdd != null && playerPort != null)
        {
            registration = "rmi://" + playerIpAdd + ":" + playerPort + "/" + playerId;
        } else
        {
            assert false : "Could not lookup registration for player key " + playerKey;
        }
        return registration;
    }

    private static String getPlayerRegistration(GameState gameState, Integer playerKey)
    {
        String registration = null;
        String playerId = gameState.playerNames.get(playerKey);
        String playerIpAdd = gameState.playerIpAddresses.get(playerKey);
        Integer playerPort = gameState.playerPorts.get(playerKey);
        if (playerId != null && playerIpAdd != null && playerPort != null)
        {
            registration = "rmi://" + playerIpAdd + ":" + playerPort + "/" + playerId;
        } else
        {
            assert false : "Could not lookup registration via game state for player key " + playerKey;
        }
        return registration;
    }

    public void run()
    {
        Scanner scanner = new Scanner(System.in);
        boolean actionReplyOK = false;
        init();
        while (scanner.hasNextLine())
        {
            actionReplyOK = false;
            String input = scanner.nextLine();

            if (game.intProperty.get() == 0)
            {
                continue;
            }

            //first try with PServer
            GameNodeInterface tempStub = gameNode.gameState.playerStub.get(gameNode.gameState.getPrimaryPlayerKey());
            GameState retGameState;

            try
            {
                retGameState = tempStub.makeMove(myPlayerKey, input);
                gameNode.gameState = retGameState;
                if(input.equals("9"))
                {
                    game.stop();
                    break;
                }
                updateGameView(game, retGameState);
                myPingThread.myGameState = retGameState;
                actionReplyOK = true;

            } catch (RemoteException ex)
            {
                Logger.getLogger(LogicLoop.class.getName()).log(Level.SEVERE, null, ex);
            }

            //if Pserver crash, then try with BServer
            if (actionReplyOK == false)
            {
                tempStub = gameNode.gameState.playerStub.get(gameNode.gameState.getBackupPlayerKey());
                try
                {
                    retGameState = tempStub.makeMove(myPlayerKey, input);
                    gameNode.gameState = retGameState;
                    updateGameView(game, retGameState);
                    myPingThread.myGameState = retGameState;
                    actionReplyOK = true;
                } catch (RemoteException ex)
                {
                    Logger.getLogger(LogicLoop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static void updateGameView(Game game, GameState newGameState)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
//                game.updateView(newGameState);
                game.intProperty.set(game.intProperty.get() + 1);
            }
        });
    }
}
