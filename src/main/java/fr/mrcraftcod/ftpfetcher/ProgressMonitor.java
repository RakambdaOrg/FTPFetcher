package fr.mrcraftcod.ftpfetcher;

import com.jcraft.jsch.SftpProgressMonitor;

public class ProgressMonitor implements SftpProgressMonitor
{
    private static final int MAX_CHAR = 140;
    private long max                = 0;
    private long count              = 0;
    private double percent            = 0.0;
    
    public ProgressMonitor() {}

    public void init(int op, java.lang.String src, java.lang.String dest, long max)
    {
        this.max = max;
         System.out.format("Starting\r");
    }

    private String getProgressBar(double percent, int max, char c)
    {
        String s = "";
        for(int i =0; i <= percent * max; i++)
            s += c;
        return s;
    }

    public boolean count(long bytes)
    {
        this.count += bytes;
        double percentNow = (double) this.count/max;
        if(percentNow > this.percent)
        {
            this.percent = percentNow;
            System.out.format(" %6.2f%% [%-" + MAX_CHAR + "s]\r", percentNow * 100, getProgressBar(percentNow, MAX_CHAR, '#'));
        }

        return(true);
    }

    public void end()
    {
        System.out.format("\33[2K\r");
    }
}
