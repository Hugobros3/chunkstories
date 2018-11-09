//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.net.announcer;

import io.xol.chunkstories.api.util.configuration.Configuration;
import io.xol.chunkstories.net.http.HttpRequests;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.util.VersionInfo;

import java.net.Inet4Address;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO Use proper way to http stuff instead of this ugly ass hack

/**
 * Small background thread that is tasked with putting and keeping up to date the server's entry in the global list
 * <br/>
 * Note: This keeps running so it can be hot-enabled/disabled without too much trouble
 */
public class ServerAnnouncerThread extends Thread {
    private final DedicatedServer server;
    private final AtomicBoolean run = new AtomicBoolean(true);

    /**
     * Unique key used to authentificate on the servers list.
     */
    private int lolcode; //TODO moar bits

    public ServerAnnouncerThread(DedicatedServer server) {
        this.server = server;

        lolcode = server.getServerConfig().getIntValue("server.announcer.lolcode");
        if (lolcode == 0L) {
            Random rnd = new Random();
            lolcode = rnd.nextInt(Integer.MAX_VALUE);
            Configuration.OptionInt option = server.getServerConfig().get("server.announcer.lolcode");
            option.trySetting(lolcode);
        }

        setName("Server list announcer thread");
    }

    public void stopAnnouncer() {
        run.set(false);
    }

    @Override
    public void run() {
        try {
            String internalIp = Inet4Address.getLocalHost().getHostAddress();
            String externalIp = HttpRequests.sendPost("https://chunkstories.xyz/api/sayMyName.php?ip=1", "");

            while (run.get()) {
                if (server.getServerConfig().getBooleanValue("server.announcer.enable")) {

                    String serverName = server.getServerConfig().getValue("server.name");
                    String serverDescription = server.getServerConfig().getValue("server.description");

                    HttpRequests.sendPost("https://chunkstories.xyz/api/serverAnnounce.php",
                            "srvname=" + serverName + "&desc=" + serverDescription + "&ip=" + externalIp + "&iip=" + internalIp
                                    + "&mu=" + server.getHandler().getMaxClients() + "&u="
                                    + server.getHandler().getPlayersNumber() + "&n=0&w=default&p=1&v="
                                    + VersionInfo.version + "&lolcode=" + lolcode);

                    sleep(server.getServerConfig().getIntValue("server.announcer.dutyCycle"));
                } else
                    sleep(6000);
            }
        } catch (Exception e) {
            server.logger().error("An unexpected error happened during multiverse stuff. More info below.");
            e.printStackTrace();
        }
    }
}
