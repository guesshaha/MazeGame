import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface TrackerInterface extends Remote
{

    //Ask tracker to add new player
    boolean addPlayer(String playerID, String playerIP, Integer playerPort, GameNodeInterface gameNode) throws RemoteException;

    boolean removePlayer(int playerKey) throws RemoteException;

    int getNumTreasure() throws RemoteException;

    int getGridSize() throws RemoteException;

    TrackerInfo PlayerJoinReqHandler(String id, String ip, String port, GameNodeInterface gameNode) throws RemoteException;

    HashMap<Integer, String> getPlayerID() throws RemoteException;

    HashMap<Integer, String> getPlayerIP() throws RemoteException;

    HashMap<Integer, Integer> getPlayerPort() throws RemoteException;
    //To add more
}
