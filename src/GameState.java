import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState implements Serializable
{

    private AtomicInteger primaryServerPlayerKey;
    private AtomicInteger backupServerPlayerKey; // Could be null if not yet unassigned.
    private AtomicInteger[][] grid;
    public HashMap<Integer, String> playerNames;
    public HashMap<Integer, String> playerIpAddresses;
    public HashMap<Integer, Integer> playerPorts;
    public HashMap<Integer, Integer> playerScores;
    public HashMap<Integer, GameNodeInterface> playerStub;
    private AtomicInteger seqId;

    final public static int EMPTY_ID = Integer.MAX_VALUE;
    final public static int TREASURE_ID = Integer.MAX_VALUE - 1;

    private static Random rand = new Random(1000);

    public GameState(AtomicInteger primaryServerPlayerKey,
            AtomicInteger backupServerPlayerKey,
            AtomicInteger[][] grid,
            HashMap<Integer, String> playerNames,
            HashMap<Integer, String> playerIpAddresses,
            HashMap<Integer, Integer> playerPorts,
            HashMap<Integer, Integer> playerScores,
            HashMap<Integer, GameNodeInterface> playerStub,
            AtomicInteger seqId)
    {
        assert primaryServerPlayerKey != null : "Cannot create game state without primary server key.";
        this.primaryServerPlayerKey = primaryServerPlayerKey;
        this.backupServerPlayerKey = backupServerPlayerKey;
        this.grid = grid;
        this.playerNames = playerNames;
        this.playerIpAddresses = playerIpAddresses;
        this.playerPorts = playerPorts;
        this.playerScores = playerScores;
        this.playerStub = playerStub;
        this.seqId = seqId;
    }

    public int getPrimaryPlayerKey()
    {
        return primaryServerPlayerKey.get();
    }

    public int getBackupPlayerKey()
    {
        return backupServerPlayerKey.get();
    }

    public int getSeqId()
    {
        return seqId.get();
    }

    public int gridSize()
    {
        return grid[0].length;
    }

    public static class V2
    {

        public int x;
        public int y;

        public V2(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    private static V2 GetPlayerPos(int playerKey, AtomicInteger[][] grid, int gridSize)
    {
        V2 result = null;
        // loop through grid to find player position
        for (int row = 0; row < gridSize; row++)
        {
            for (int col = 0; col < gridSize; col++)
            {
                if (getOccupant(grid, row, col) == playerKey)
                {
                    result = new V2(col, row);
                    break;
                }
            }
        }
        assert result != null : "Could not find player " + playerKey;
        return result;
    }

    // new player joined
    public GameState update(Integer playerKey, String playerId, String ipAddress, int port, GameNodeInterface gameNode)
    {
        synchronized (playerNames)
        {
            playerNames.put(playerKey, playerId);
        }
        synchronized (playerIpAddresses)
        {
            playerIpAddresses.put(playerKey, ipAddress);
        }
        synchronized (playerPorts)
        {
            playerPorts.put(playerKey, port);
        }
        synchronized (playerScores)
        {
            playerScores.put(playerKey, 0);
        }
        synchronized (playerStub)
        {
            playerStub.put(playerKey, gameNode);
        }

        V2 playerPos = GameState.getRandomUnoccupied(grid, gridSize());
        grid[playerPos.y][playerPos.x].set(playerKey);
        // if there's no backup server, make this new player the backup server.

        if (backupServerPlayerKey == null)
        {
            backupServerPlayerKey = new AtomicInteger(playerKey);
        } else
        {
//            String temp = playerIpAddresses.get(backupServerPlayerKey);
            String temp = playerIpAddresses.get(backupServerPlayerKey.get());
            if (temp == null)
            {
                backupServerPlayerKey = new AtomicInteger(playerKey);
            }
        }

        seqId.getAndIncrement();
        return this;
    }

    public GameState update(int playerKey, String move)
    {
        if (move.equals("0"))
        { // refresh
        } else if (move.equals("9"))
        { // quit
            V2 playerPos = GetPlayerPos(playerKey, grid, gridSize());
            grid[playerPos.y][playerPos.x].set(EMPTY_ID);
            synchronized (playerNames)
            {
                playerNames.remove(playerKey);
            }
            synchronized (playerPorts)
            {
                playerPorts.remove(playerKey);
            }
            synchronized (playerIpAddresses)
            {
                playerIpAddresses.remove(playerKey);
            }
            synchronized (playerScores)
            {
                playerScores.remove(playerKey);
            }
            synchronized (playerStub)
            {
                playerStub.remove(playerKey);
            }

            seqId.getAndIncrement();
        } else
        {
            V2 moveV2 = null;
            switch (move)
            {
                case "1":  //West
                    moveV2 = new V2(-1, 0);
                    break;
                case "2":  //South
                    moveV2 = new V2(0, 1);
                    break;
                case "3":  //East
                    moveV2 = new V2(1, 0);
                    break;
                case "4":  //North
                    moveV2 = new V2(0, -1);
                    break;
            }
            if (moveV2 != null)
            {
                V2 playerPos = GetPlayerPos(playerKey, grid, gridSize());
                int targetRow = playerPos.y + moveV2.y;
                int targetCol = playerPos.x + moveV2.x;
                if (targetRow >= 0 && targetRow < gridSize()
                        && targetCol >= 0 && targetCol < gridSize())
                {
                    int occupant = getOccupant(grid, targetRow, targetCol);
                    if (occupant == EMPTY_ID)
                    {
                        grid[playerPos.y][playerPos.x].set(EMPTY_ID);
                        grid[targetRow][targetCol].set(playerKey);

                        seqId.getAndIncrement();
                    } else if (occupant == TREASURE_ID)
                    {
                        grid[playerPos.y][playerPos.x].set(EMPTY_ID);
                        grid[targetRow][targetCol].set(playerKey);
                        synchronized (playerScores)
                        {
                            playerScores.put(playerKey, playerScores.get(playerKey) + 1);
                        }
                        V2 newTreasurePos = getRandomUnoccupied(grid, gridSize());
                        grid[newTreasurePos.y][newTreasurePos.x].set(TREASURE_ID);

                        seqId.getAndIncrement();
                    }
                }
            }
        }

        return this;
    }

    public static V2 getRandomUnoccupied(AtomicInteger[][] grid, int gridSize)
    {
        V2 result = null;
        do
        {
            result = new V2(rand.nextInt(gridSize - 1),
                    rand.nextInt(gridSize - 1));
        } while (getOccupant(grid, result.y, result.x) != EMPTY_ID);
        return result;
    }

    private static int getOccupant(AtomicInteger[][] grid, int row, int col)
    {
        assert row >= 0 && row < grid.length : "Out of range row " + row;
        assert col >= 0 && col < grid[0].length : "Out of range col " + col;
        AtomicInteger id = grid[row][col];
        return id.get();
    }

    public String getOccupantAsString(int row, int col)
    {
        String result;
        int id = getOccupant(grid, row, col);
        if (id == EMPTY_ID)
        {
            result = "";
        } else if (id == TREASURE_ID)
        {
            result = "*";
        } else
        {
            result = playerNames.get(id);
        }
        return result;
    }

    public void PSremoveCrashNode(int crashedNodeKey)
    {
        synchronized (playerNames)
        {
            playerNames.remove(crashedNodeKey);
        }
        synchronized (playerPorts)
        {
            playerPorts.remove(crashedNodeKey);
        }
        synchronized (playerIpAddresses)
        {
            playerIpAddresses.remove(crashedNodeKey);
        }
        synchronized (playerScores)
        {
            playerScores.remove(crashedNodeKey);
        }
        synchronized (playerStub)
        {
            playerStub.remove(crashedNodeKey);
        }

        V2 playerPos = GameState.GetPlayerPos(crashedNodeKey, grid, gridSize());
        grid[playerPos.y][playerPos.x].set(EMPTY_ID);

        // if there's no backup server, assign new backup server.
        if (backupServerPlayerKey.get() == crashedNodeKey )
        {
            backupServerPlayerKey.set(0);
            if(getCurrentPlayerNum() > 1)
                assignBServer();
        }

        seqId.getAndIncrement();

    }

    public void BSremoveCrashNode(int crashedNodeKey, int myPlayerKey)
    {
        if (crashedNodeKey == primaryServerPlayerKey.get())
        {
            //assign PServer, assign BServer
            primaryServerPlayerKey.set(myPlayerKey);

        } else
        {
            System.out.println("CrashNodeReqHandler error, crash node not PServer but BServer receive crash node request");
        }

        synchronized (playerNames)
        {
            playerNames.remove(crashedNodeKey);
        }
        synchronized (playerPorts)
        {
            playerPorts.remove(crashedNodeKey);
        }
        synchronized (playerIpAddresses)
        {
            playerIpAddresses.remove(crashedNodeKey);
        }
        synchronized (playerScores)
        {
            playerScores.remove(crashedNodeKey);
        }
        synchronized (playerStub)
        {
            playerStub.remove(crashedNodeKey);
        }

        V2 playerPos = GameState.GetPlayerPos(crashedNodeKey, grid, gridSize());
        grid[playerPos.y][playerPos.x].set(EMPTY_ID);

//        if (getCurrentPlayerNum() > 1)
//        {
//            assignBServer();
//        }
        
        backupServerPlayerKey.set(0);
        if(getCurrentPlayerNum() > 1)
            assignBServer();
        
        seqId.getAndIncrement();

    }

    private void assignBServer()
    {
        //update local state with new seqCounter
        int key;
        int i;

        List<Integer> keys = new ArrayList<Integer>(playerIpAddresses.keySet());
        int nodeTotalNum = keys.size();

        for (i = 0; i < nodeTotalNum; i++)
        {
            key = keys.get(i);
            if (key != primaryServerPlayerKey.get())
            {
                backupServerPlayerKey.set(key);
                break;
            }
        }
    }

    public GameState updateWithNewGameState(GameState clientGameState)
    {
        if (seqId.get() >= clientGameState.getSeqId())
        {
            return this;
        }

        int i, j, size;
        size = clientGameState.gridSize();

        seqId = clientGameState.seqId;
        primaryServerPlayerKey = clientGameState.primaryServerPlayerKey;
        backupServerPlayerKey = clientGameState.backupServerPlayerKey;

        for (i = 0; i < size; i++)
        {
            for (j = 0; j < size; j++)
            {
                grid[i][j] = clientGameState.grid[i][j];
            }
        }

        List<Integer> keys = new ArrayList<Integer>(clientGameState.playerIpAddresses.keySet());
        int nodeTotalNum = keys.size();
        synchronized (playerNames)
        {
            playerNames.clear();
        }
        synchronized (playerPorts)
        {
            playerPorts.clear();
        }
        synchronized (playerIpAddresses)
        {
            playerIpAddresses.clear();
        }
        synchronized (playerScores)
        {
            playerScores.clear();
        }
        synchronized (playerStub)
        {
            playerStub.clear();
        }

        for (i = 0; i < nodeTotalNum; i++)
        {
            int key = keys.get(i);
            synchronized (playerNames)
            {
                playerNames.put(key, clientGameState.playerNames.get(key));
            }
            synchronized (playerPorts)
            {
                playerPorts.put(key, clientGameState.playerPorts.get(key));
            }
            synchronized (playerIpAddresses)
            {
                playerIpAddresses.put(key, clientGameState.playerIpAddresses.get(key));
            }
            synchronized (playerScores)
            {
                playerScores.put(key, clientGameState.playerScores.get(key));
            }
            synchronized (playerStub)
            {
                playerStub.put(key, clientGameState.playerStub.get(key));
            }
        }

        return this;
    }

    public int getCurrentPlayerNum()
    {
        List<Integer> keys = new ArrayList<Integer>(playerIpAddresses.keySet());
        return keys.size();
    }
}
