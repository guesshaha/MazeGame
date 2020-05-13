import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PingThread extends Thread
{

    boolean isTerminated = false;
    int targetKey;
    int KeySetIterateOffset;
    GameState myGameState;

    public PingThread(GameState c)
    {
        myGameState = c;

        Random random = new Random();
        List<Integer> keys = new ArrayList<Integer>(myGameState.playerIpAddresses.keySet());
        KeySetIterateOffset = random.nextInt(keys.size());
    }

    public void run()
    {
        System.out.println("PingThread start...");
        while (!isTerminated)
        {
            targetKey = getRandomTargetKey();
            if (!pingTest(targetKey))
            {
                FailAction(targetKey);
            }

            try
            {
                Thread.sleep(500);//sleep for 500 ms
            } catch (InterruptedException ex)
            {
                printWithSourceCodeDetailUtl.println(ex.toString());
            }
        }
    }

    public int getRandomTargetKey()
    {
        List<Integer> keys = new ArrayList<Integer>(myGameState.playerIpAddresses.keySet());
        if (++KeySetIterateOffset >= keys.size())
        {
            KeySetIterateOffset = 0;
        }
        int randomKey = keys.get(KeySetIterateOffset);
        return randomKey;
    }

    public boolean pingTest(int targetKey)
    {
        boolean retStatus = false;
        String ip = myGameState.playerIpAddresses.get(targetKey);
        int port = myGameState.playerPorts.get(targetKey);
        String name = myGameState.playerNames.get(targetKey);

        try
        {
            GameNodeInterface remoteGameNote = myGameState.playerStub.get(targetKey);
            retStatus = remoteGameNote.isAlive();
//            printWithSourceCodeDetailUtl.println("Ping test with " + name + "(" + targetKey + ") ok");
        } catch (RemoteException ex)
        {
            Logger.getLogger(PingThread.class.getName()).log(Level.SEVERE, null, ex);
            printWithSourceCodeDetailUtl.println("Ping test with " + name + "(" + targetKey + ") fail");
        }

        return retStatus;
    }

    public void FailAction(int targetKey)
    {
        boolean isSuccess = false;

        if (targetKey != myGameState.getPrimaryPlayerKey())
        {
            GameNodeInterface tempStub = myGameState.playerStub.get(myGameState.getPrimaryPlayerKey());
            GameState retGameState;
            try
            {
                retGameState = tempStub.CrashNodeReqHandler(targetKey);
                myGameState.updateWithNewGameState(retGameState);
                isSuccess = true;
            } catch (Exception ex)
            {
                printWithSourceCodeDetailUtl.println("Exception Error - " + ex);
            }

            if (isSuccess == false)
            {
                tempStub = myGameState.playerStub.get(myGameState.getBackupPlayerKey());
                try
                {
                    retGameState = tempStub.CrashNodeReqHandler(targetKey);
                    myGameState.updateWithNewGameState(retGameState);
                    isSuccess = true;
                } catch (Exception ex)
                {
                    printWithSourceCodeDetailUtl.println("Exception Error - " + ex);
                }
            }
        } else
        {
            GameNodeInterface tempStub = myGameState.playerStub.get(myGameState.getBackupPlayerKey());
            GameState retGameState;
            try
            {
                retGameState = tempStub.CrashNodeReqHandler(targetKey);
                myGameState.updateWithNewGameState(retGameState);
                isSuccess = true;
            } catch (Exception ex)
            {
                printWithSourceCodeDetailUtl.println("Exception Error - " + ex);
            }
        }

    }

}
