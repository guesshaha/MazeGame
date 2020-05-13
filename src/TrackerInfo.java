import java.io.Serializable;
import java.util.HashMap;

public class TrackerInfo implements Serializable
{

    private int port;
    private int gridSize;
    private int NumTreasure;
    //hashmaps of playerKey, playerID, player IP address, player port
    public HashMap<Integer, String> playerIdMap = new HashMap<Integer, String>();
    public HashMap<Integer, String> playerIPaddMap = new HashMap<Integer, String>();
    public HashMap<Integer, Integer> playerPortMap = new HashMap<Integer, Integer>();
    public HashMap<Integer, GameNodeInterface> playerStubMap = new HashMap<Integer, GameNodeInterface>();

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setGridSize(int N)
    {
        this.gridSize = N;
    }

    public void setNumTreasure(int K)
    {
        this.NumTreasure = K;
    }

    public int getPort()
    {
        return port;
    }

    public int getSizeN()
    {
        return gridSize;
    }

    public int getNumK()
    {
        return NumTreasure;
    }

}
