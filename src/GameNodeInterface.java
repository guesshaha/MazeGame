import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameNodeInterface extends Remote
{

    GameState joinGame(Integer playerKey, String playerId, String ipAddress, int port, GameNodeInterface gameNode) throws RemoteException; // Only if it's primary server!!!

    GameState makeMove(Integer playerKey, String move) throws RemoteException; // Only if it's primary server!!!

    GameState getGameState() throws RemoteException;

    boolean isAlive() throws RemoteException;

    public GameState CrashNodeReqHandler(int crashedNodeKey) throws RemoteException;

    GameState syncGameState(GameState clientGameState) throws RemoteException;
}
