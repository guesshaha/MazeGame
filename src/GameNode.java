import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.util.concurrent.atomic.AtomicInteger;

public class GameNode extends UnicastRemoteObject implements GameNodeInterface, Serializable
{

    public GameState gameState;

    public GameState getGameState()
    {
        System.out.println("Remote request GameState");
        return gameState;
    }

    private Integer myPlayerKey;

    public GameNode() throws RemoteException
    {
    }

    public void setGameState(GameState gameState)
    {
        this.gameState = gameState;
    }

    public void setPlayerKey(int key)
    {
        myPlayerKey = key;
    }

    public GameNode(GameState gameState, Integer myPlayerKey) throws RemoteException
    {
        this.gameState = gameState;
        this.myPlayerKey = myPlayerKey;
    }

    public GameNode(GameState gameState, Integer myPlayerKey, String myIpAddress, int myPort, String myPlayerId) throws RemoteException
    {
        this.gameState = gameState;
        this.myPlayerKey = myPlayerKey;

        System.out.println("Creating GameNode for " + myPlayerId + ", player key: " + myPlayerKey);

    }

    // Only if it's primary server!!!
    public GameState joinGame(Integer playerKey, String playerId, String ipAddress, int port, GameNodeInterface gameNode) throws RemoteException
    {
        if (isPrimaryServer())
        {
            // add the new player in.
            gameState.update(playerKey, playerId, ipAddress, port, gameNode);
            System.out.println("[PrimaryServer" + myPlayerKey + "] " + playerId + "(" + playerKey + ") has joined the game!");

            //if only 1 player, no need to send updateGameState
            //if only 2 player, PServer and BServer, no need to send updateGateState because this function will return new GameState to BServer
            if (gameState.getCurrentPlayerNum() > 2)
            {
                sendUpdateGameStateToBserver();
            }
        } else if (isBackupServer())
        {
            //means PServer dies, then change to PServer and assign BServer, perform join request and, udpate state with BServer, notify Tracker and return updated GameState
            System.out.println("[BackupServer " + myPlayerKey + "] receive joinGame Req, meaning PServer crashed already");
            int crashedNodeKey = gameState.getPrimaryPlayerKey();
            gameState.BSremoveCrashNode(crashedNodeKey, myPlayerKey);

            gameState.update(playerKey, playerId, ipAddress, port, gameNode);
            System.out.println("[PrimaryServer" + myPlayerKey + "] " + playerId + "(" + playerKey + ") has joined the game!");

            if (gameState.getCurrentPlayerNum() > 2)
            {
                sendUpdateGameStateToBserver();
            }

            sendCrashNodeToTracker(crashedNodeKey);
        } else
        {
            assert false : "Trying to call joinGame on a non-primary server!";
        }
        return gameState;
    }

    // Only if it's primary server!!!
    public GameState makeMove(Integer playerKey, String move) throws RemoteException
    {
        if (isPrimaryServer())
        {
            gameState.update(playerKey, move);
            //System.out.println("[PrimaryServer " + myPlayerKey + "] " + playerKey + " made move " + move);

            if (gameState.getCurrentPlayerNum() > 2)
            {
                sendUpdateGameStateToBserver();
            }
        } else if (isBackupServer())
        {
            //means PServer dies, then change to PServer and assign BServer, perform move request and, udpate state with BServer, notify Tracker and return updated GameState
            System.out.println("[BackupServer " + myPlayerKey + "] receive makeMove Req, meaning PServer crashed already");
            int crashedNodeKey = gameState.getPrimaryPlayerKey();
            gameState.BSremoveCrashNode(crashedNodeKey, myPlayerKey);

            gameState.update(playerKey, move);
            //System.out.println("[PrimaryServer " + myPlayerKey + "] " + playerKey + " made move " + move);

            if (gameState.getCurrentPlayerNum() > 2)
            {
                sendUpdateGameStateToBserver();
            }

            sendCrashNodeToTracker(crashedNodeKey);
        } else
        {
            assert false : "Trying to call makeMove on a non-primary server!";
        }
        return gameState;
    }

    public boolean isAlive() throws RemoteException
    {
        //System.out.println("Get ping request from others, return alive");
        return true;
    }

    public GameState CrashNodeReqHandler(int crashedNodeKey) throws RemoteException
    {
        if (!gameState.playerNames.containsKey(crashedNodeKey))
        {
            return gameState;
        }

        if (isPrimaryServer())
        {
            System.out.println("[PrimaryServer" + myPlayerKey + "] playerKey " + crashedNodeKey + " crashed ");
            gameState.PSremoveCrashNode(crashedNodeKey);
        } else if (isBackupServer())
        {
            System.out.println("[BackupServer" + myPlayerKey + "] playerKey " + crashedNodeKey + " crashed ");
            gameState.BSremoveCrashNode(crashedNodeKey, myPlayerKey);
        }
        if (gameState.getCurrentPlayerNum() > 1)
        {
            sendUpdateGameStateToBserver();
        }

        sendCrashNodeToTracker(crashedNodeKey);
        return gameState;
    }

    public GameState syncGameState(GameState clientGameState) throws RemoteException
    {
        if (!isPrimaryServer())
        {
            gameState.updateWithNewGameState(clientGameState);
        }

        return gameState;
    }

    private boolean isPrimaryServer()
    {
        return gameState.getPrimaryPlayerKey() == myPlayerKey;
    }

    private boolean isBackupServer()
    {
        return gameState.getBackupPlayerKey() == myPlayerKey;
    }

    private boolean sendUpdateGameStateToBserver()
    {
        boolean retStatus = false;
        String mip = gameState.playerIpAddresses.get(gameState.getBackupPlayerKey());
        int mport = gameState.playerPorts.get(gameState.getBackupPlayerKey());
        String mname = gameState.playerNames.get(gameState.getBackupPlayerKey());

        GameNodeInterface tempStub = gameState.playerStub.get(gameState.getBackupPlayerKey());
        GameState returnGS;
        try
        {
            returnGS = tempStub.syncGameState(gameState);
            if (returnGS.getSeqId() != gameState.getSeqId())
            {
                System.out.println("[PlayerKey " + myPlayerKey + "] " + "send syncGameState to BServer fail");
            } else
            {
                System.out.println("[PlayerKey " + myPlayerKey + "] " + "send syncGameState to BServer success");
            }
        } catch (RemoteException ex)
        {
            printWithSourceCodeDetailUtl.println(ex.toString());
        }
        return retStatus;
    }

    public boolean sendCrashNodeToTracker(int crashedNodeKey)
    {
        boolean retStatus = false;
        String mip = Game.trackerIp; //currently hardcode
        int mport = Game.trackerPort;  //currently hardcode
        String mname = "tracker";   //currently hardcode

        try
        {   // look for tracker
            String TRACKER_REGISTRATION = "rmi://" + mip + ":" + mport + "/tracker";
            // Lookup the service in the registry, and obtain a remote service
            Remote remoteService = Naming.lookup(TRACKER_REGISTRATION);
            TrackerInterface myTrackerInterface = (TrackerInterface) remoteService;

            myTrackerInterface.removePlayer(crashedNodeKey);

            System.out.println("[PlayerKey " + myPlayerKey + "] " + "send crashNodeKey: " + crashedNodeKey + " to Tracker ");
        }catch (NotBoundException nbe)
        {
            printWithSourceCodeDetailUtl.println("No service available in registry!");
        } catch (RemoteException re)
        {
            printWithSourceCodeDetailUtl.println("RMI Error - " + re);
        } catch (Exception e)
        {
            printWithSourceCodeDetailUtl.println("Error - " + e);
        }
        return retStatus;
    }
}
