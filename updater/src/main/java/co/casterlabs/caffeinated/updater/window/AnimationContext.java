package co.casterlabs.caffeinated.updater.window;

import java.awt.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class AnimationContext {
    private static final int FRAME_RATE = 30;

    private static final double FRAME_INTERVAL = FRAME_RATE / 1000f;
    private static final long FRAME_INTERVAL_FLOOR = (long) Math.floor(FRAME_RATE);

    private static @Setter Component repaintable;

    private static @Getter @Setter boolean isAnimationFrame = false;
    private static @Setter long lastAnimation = 0;

    static {
        // Java will kill it if it's time to shutdown.
        Thread t = new Thread(() -> {
            while (true) {
                AnimationContext.waitToAnimate();
            }
        });

        t.setName("Animation Thread");
        t.setDaemon(true);
        t.start();
    }

    public static double getDelta() {
        // Get the time between now and lastAnimation, make it a float between 0 and 1.
        long now = System.currentTimeMillis();
        long diff = now - lastAnimation;

        if (diff <= 0) {
            return 1;
        } else {
            return diff / FRAME_INTERVAL;
        }
    }

    @SneakyThrows
    private static void waitToAnimate() {
        Thread.sleep(FRAME_INTERVAL_FLOOR);

        if (repaintable != null) {
            while (getDelta() < 1) {
                // Waste cpu.
            }

            // Animate.
            isAnimationFrame = true;

            repaintable.repaint(0);
        }
    }

    // This gets called by UpdaterDialog.
    public static void reset() {
        if (isAnimationFrame) {
            lastAnimation = System.currentTimeMillis();
            isAnimationFrame = false;
        }
    }

}
