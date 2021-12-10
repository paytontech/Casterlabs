package co.casterlabs.caffeinated.localserver;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.caffeinated.app.CaffeinatedApp;
import co.casterlabs.caffeinated.localserver.handlers.RouteLocalServer;
import co.casterlabs.caffeinated.localserver.handlers.RoutePluginApi;
import co.casterlabs.caffeinated.localserver.handlers.RouteWidgetApi;
import co.casterlabs.caffeinated.localserver.websocket.RealtimeConnection;
import co.casterlabs.caffeinated.util.Pair;
import co.casterlabs.caffeinated.util.async.AsyncTask;
import co.casterlabs.rakurai.io.http.server.HttpServerImplementation;
import co.casterlabs.rakurai.io.http.websocket.Websocket;
import co.casterlabs.sora.Sora;
import co.casterlabs.sora.SoraFramework;
import co.casterlabs.sora.SoraLauncher;
import co.casterlabs.sora.api.SoraPlugin;
import co.casterlabs.sora.api.http.HttpProvider;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class LocalServer implements Closeable, HttpProvider {
    private static final long PING_INTERVAL = TimeUnit.SECONDS.toMillis(15);
    private SoraFramework framework;

    @SneakyThrows
    public LocalServer(int port) {
        this.framework = new SoraLauncher()
            .setPort(port)
            .setImplementation(HttpServerImplementation.UNDERTOW)
            .buildWithoutPluginLoader();

        this.framework
            .getSora()
            .register(new LocalServerPluginWrapper());
    }

    private class LocalServerPluginWrapper extends SoraPlugin {

        @Override
        public void onInit(Sora sora) {
            sora.addHttpProvider(this, new RouteLocalServer());
            sora.addHttpProvider(this, new RoutePluginApi());
            sora.addHttpProvider(this, new RouteWidgetApi());

            new AsyncTask(this::pingHandler);
        }

        private void pingHandler() {
            while (true) {
                try {
                    for (Websocket websocket : this.getWebsockets()) {
                        Pair<RealtimeConnection, Object> attachment = websocket.getAttachment();

                        attachment.a.checkExpiryAndPing();
                    }
                    Thread.sleep(PING_INTERVAL);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        public void onClose() {}

        @Override
        public @Nullable String getVersion() {
            return CaffeinatedApp.getInstance().getBuildInfo().getVersionString();
        }

        @Override
        public @Nullable String getAuthor() {
            return "Casterlabs";
        }

        @Override
        public @NonNull String getName() {
            return "Caffeinated Conductor (LocalServer)";
        }

        @Override
        public @NonNull String getId() {
            return "co.casterlabs.caffeinated.conductor";
        }

    }

    /* ---------------- */
    /* IO Related       */
    /* ---------------- */

    public void start() throws IOException {
        this.framework.getServer().start();
        FastLogger.logStatic("Started!");
    }

    public boolean isAlive() {
        return this.framework.getServer().isAlive();
    }

    @Override
    public void close() throws IOException {
        this.framework.getServer().stop();
        FastLogger.logStatic("Stopped!");
    }

}
