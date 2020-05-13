import java.rmi.registry.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Tracker extends UnicastRemoteObject implements TrackerInterface
{

    private TrackerInfo tInfo; //Basic tracker Information
    static Integer playerKey = 10000; //static or public
    private String trackerHost = "127.0.0.1";

    public Tracker() throws RemoteException
    {
        super();
        this.tInfo = new TrackerInfo();

    }

    public void setTrackerInfo(int port, int N, int K)
    {
        this.tInfo.setPort(port);
        this.tInfo.setGridSize(N);
        this.tInfo.setNumTreasure(K);
    }

    //When player request to join the game, call addPlayer function and return tracker info
    //Need to call the specific function if need player info in HashMap
    public TrackerInfo PlayerJoinReqHandler(String id, String ip, String port, GameNodeInterface gameNode) throws RemoteException
    {
        boolean joinStatus;
        try
        {
            if (true)
            {
                joinStatus = addPlayer(id, ip, Integer.parseInt(port), gameNode);
                if(!joinStatus)
                    return null;
            } else
            {
                System.out.println("Print enter your ID, IP address, port correctly.");
            }
        } catch (RemoteException e)
        {
            e.printStackTrace();
        }
        //System.out.println("PlayerJoinReqHandler" + id + " " + ip + " " + port);
        return this.tInfo;
    }

    public boolean startTrackerService(String serviceName, UnicastRemoteObject obj)
    {
        try
        {
            Registry registry = LocateRegistry.createRegistry(this.tInfo.getPort());
            System.out.println("Port registered!");
            registry.bind(serviceName, obj);
            System.out.println("Tracker binded!");
        } catch (AccessException e)
        {
            e.printStackTrace();
        } catch (RemoteException e)
        {
            e.printStackTrace();
        } catch (AlreadyBoundException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean addPlayer(String playerID, String playerIP, Integer playerPort, GameNodeInterface gameNode) throws RemoteException
    {

        //Avoid adding duplicate playerID
        if (this.tInfo.playerIdMap.containsValue(playerID))
        {
            System.out.println("Duplicated ID noticed, please try again.");
            return false;
        } else
        {
            playerKey++;
            this.tInfo.playerIdMap.put(playerKey, playerID);
            this.tInfo.playerIPaddMap.put(playerKey, playerIP);
            this.tInfo.playerPortMap.put(playerKey, playerKey);
            this.tInfo.playerStubMap.put(playerKey, gameNode);

        }

        System.out.println(playerID + " added");
        System.out.println("current number of player is "+this.tInfo.playerIdMap.size());
        return true;

    }

    @Override
    public boolean removePlayer(int playerKey) throws RemoteException
    {
        //only remove the player if it exists in current list
        if (this.tInfo.playerIdMap.containsKey(playerKey))
        {
            System.out.println(this.tInfo.playerIdMap.get(playerKey) + " removed");
            this.tInfo.playerIdMap.remove(playerKey);
            this.tInfo.playerIPaddMap.remove(playerKey);
            this.tInfo.playerPortMap.remove(playerKey);
            this.tInfo.playerStubMap.remove(playerKey);
            System.out.println("Remaining number of player is "+this.tInfo.playerIdMap.size());
        } else
        {
            return false;
        }
        return true;
    }

    @Override
    public int getNumTreasure() throws RemoteException
    {
        return this.tInfo.getNumK();
    }

    @Override
    public int getGridSize() throws RemoteException
    {
        return this.tInfo.getSizeN();
    }

    public HashMap<Integer, String> getPlayerID() throws RemoteException
    {
        return this.tInfo.playerIdMap;
    }

    public HashMap<Integer, String> getPlayerIP() throws RemoteException
    {
        return this.tInfo.playerIPaddMap;
    }

    public HashMap<Integer, Integer> getPlayerPort() throws RemoteException
    {
        return this.tInfo.playerPortMap;
    }

    public static void main(String[] args)
    {
        Tracker tracker = null;
        try
        {
            tracker = new Tracker();
            if (args.length == 3)
            {
                tracker.setTrackerInfo(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } else
            {
                tracker.setTrackerInfo(1099, 15, 10);
            }
        } catch (RemoteException e)
        {
            e.printStackTrace();
        }
        tracker.startTrackerService("tracker", tracker);
    }

}
