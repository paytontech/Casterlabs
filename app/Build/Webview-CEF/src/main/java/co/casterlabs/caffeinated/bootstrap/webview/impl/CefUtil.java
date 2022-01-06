package co.casterlabs.caffeinated.bootstrap.webview.impl;

import java.io.File;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

import co.casterlabs.caffeinated.app.CaffeinatedApp;
import co.casterlabs.caffeinated.bootstrap.webview.AppWebview;
import co.casterlabs.caffeinated.bootstrap.webview.scheme.SchemeHandler;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;

public class CefUtil {
    public static final boolean enableOSR = System.getProperty("caffeinated.cef.offscreenrendering.enable", "").equals("true"); // Defaults to false
    public static final boolean enableTransparency = System.getProperty("caffeinated.cef.transparency.enable", "").equals("true"); // Defaults to false

    public static final File bundleDirectory = new File(CaffeinatedApp.appDataDir, "dependencies/cef_bundle");

    public static void create(@NonNull String appScheme, @NonNull SchemeHandler schemeHandler) {
        try {
            CefAppBuilder builder = new CefAppBuilder();

            builder.addJcefArgs("--disable-http-cache", "--disable-web-security");
            builder.setInstallDir(bundleDirectory);

            builder.getCefSettings().windowless_rendering_enabled = enableOSR;
            builder.getCefSettings().log_severity = LogSeverity.LOGSEVERITY_DISABLE;
            builder.getCefSettings().user_agent_product = String.format("Chromium; Just A CasterlabsCaffeinated (%s)", AppWebview.STATE_PASSWORD);

            builder.setAppHandler(new MavenCefAppHandlerAdapter() {

                @Override
                public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
                    registrar.addCustomScheme(
                        appScheme,
                        true,  // isStandard
                        false, // isLocal
                        false, // isDisplayIsolated
                        true,  // isSecure
                        true,  // isCorsEnabled
                        true,  // isCspBypassing
                        true   // isFetchEnabled
                    );
                }

                @Override
                public void onContextInitialized() {
                    CefApp
                        .getInstance()
                        .registerSchemeHandlerFactory(appScheme, "", new CefSchemeHandlerFactory() {
                            @Override
                            public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
                                if (schemeName.equals(appScheme)) {
                                    return new CefResponseResourceHandler(schemeHandler);
                                }

                                return null;
                            }
                        });
                }

            });

            builder.setProgressHandler(new CefDownloadProgressDialog());

            builder.build();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SneakyThrows
    public static CefClient createCefClient() {
        return CefApp.getInstance().createClient();
    }

}
