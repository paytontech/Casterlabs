package co.casterlabs.caffeinated.ui;

import java.awt.BorderLayout;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefCommandLine;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefPrintHandler;
import org.jetbrains.annotations.Nullable;
import org.panda_lang.pandomium.Pandomium;
import org.panda_lang.pandomium.wrapper.PandomiumClient;

import co.casterlabs.caffeinated.cef.CefUtil;
import co.casterlabs.caffeinated.cef.bridge.JavascriptBridge;
import co.casterlabs.caffeinated.cef.scheme.SchemeHandler;
import co.casterlabs.caffeinated.cef.scheme.http.HttpRequest;
import co.casterlabs.caffeinated.cef.scheme.http.HttpResponse;
import co.casterlabs.caffeinated.cef.scheme.http.StandardHttpStatus;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class ApplicationUI {

    private static UILifeCycleListener lifeCycleListener = null;

    private static @Getter JavascriptBridge bridge;
    private static ApplicationWindow window;
    private static CefBrowser browser;
    private static CefClient client;
    private static @Getter String appAddress;

    private static FastLogger logger = new FastLogger();

    public static void initialize(@NonNull String addr, @NonNull UILifeCycleListener listener) {
        appAddress = addr;
        lifeCycleListener = listener;

        registerSchemes();

        Pandomium panda = CefUtil.createCefApp();
        PandomiumClient pandaClient = panda.createClient();

        window = new ApplicationWindow(listener);
        client = pandaClient.getCefClient();
        bridge = new JavascriptBridge(client);

        logger.debug("Loadstate 0");
        lifeCycleListener.onPreLoad();
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            // 0 = about:blank (preload)
            // 1 = app://index (load)
            // 2 = ... (completely loaded)
            private int loadState = 0;

            @Override
            public void onLoadEnd(CefBrowser _browser, CefFrame _frame, int httpStatusCode) {
                if (this.loadState == 0) {
                    logger.debug("Loadstate 1");
                    lifeCycleListener.onInitialLoad();
                    this.loadState = 2;
                    logger.debug("Loadstate 2");
                }

                bridge.injectBridgeScript(_frame);
            }

        });

        browser = pandaClient.loadURL(appAddress);
        window.getCefPanel().add(browser.getUIComponent(), BorderLayout.CENTER);
        window.getFrame().setVisible(true); // TODO figure out why onLoadEnd is not firing.

        setTitle(null);
    }

    public static void registerSchemes() {
        CefApp.addAppHandler(new CefAppHandler() {

            @Override
            public void stateHasChanged(CefAppState var1) {}

            @Override
            public void onScheduleMessagePumpWork(long var1) {}

            @Override
            public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
                if (!registrar.addCustomScheme(
                    "app", // Scheme
                    true,  // isStandard
                    false, // isLocal
                    false, // isDisplayIsolated
                    true,  // isSecure
                    false, // isCorsEnabled
                    false, // isCspBypassing
                    true   // isFetchEnabled
                )) {
                    FastLogger.logStatic(LogLevel.SEVERE, "Could not register scheme.");
                    System.exit(1);
                }
            }

            @Override
            public void onContextInitialized() {
                CefUtil.registerUrlScheme("app", new TestSchemeHandler());
            }

            @Override
            public boolean onBeforeTerminate() {
                return false;
            }

            @Override
            public void onBeforeCommandLineProcessing(String var1, CefCommandLine var2) {}

            @Override
            public CefPrintHandler getPrintHandler() {
                return null;
            }

        });
    }

    public static void setTitle(@Nullable String title) {
        if (title == null) {
            title = "Casterlabs Caffeinated";
        } else {
            title = "Casterlabs Caffeinated - " + title;
        }

        window.getFrame().setTitle(title);
        window.getTitleLabel().setText(title);
    }

    public static class TestSchemeHandler implements SchemeHandler {

        @Override
        public HttpResponse onRequest(HttpRequest request) {
            String content = "<!DOCTYPE html>"
                + "<html>"
                + "<meta charset=\"utf-8\">"
                + "<body>"
                + "Hello World!"
                + "<br />"
                + "<br />"
                + "<br />"
                + "to: world 🌎"
                + "<br />"
                + "from: Casterlabs-Caffeinated (JCEF/Pandomium)"
                + "</body>"
                + "</html>";

            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, content)
                .setMimeType("text/html");
        }

    }

}
