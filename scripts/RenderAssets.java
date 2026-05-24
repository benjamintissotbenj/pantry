import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * One-off Java renderer for the Pantry Play Store assets.
 * - icon-512.png: 512x512 PNG of the basket-of-fruits logo on forest green.
 * - feature-graphic.png: 1024x500 PNG banner with the logo + word-mark.
 *
 * Sourced from logo_pantry.xml. Hand-translated paths because none of
 * magick / rsvg-convert / inkscape were available on the target machine.
 *
 * Compile + run:
 *   javac scripts/RenderAssets.java -d scripts/
 *   java -cp scripts RenderAssets
 */
public class RenderAssets {

    // Source palette (matches logo_pantry.xml exactly)
    static final Color FOREST_GREEN = new Color(0x2E7D32);
    static final Color CREAM         = new Color(0xFFF8E1);
    static final Color WEAVE_TAN     = new Color(0xE8DCB8);
    static final Color APPLE_RED     = new Color(0xC62828);
    static final Color PEAR_YELLOW   = new Color(0xF9A825);
    static final Color GRAPE_PURPLE  = new Color(0x6A1B9A);
    static final Color LEAF_GREEN    = new Color(0x386A20);

    public static void main(String[] args) throws Exception {
        renderIcon();
        renderFeatureGraphic();
        System.out.println("Wrote play-store/icon-512.png + play-store/feature-graphic.png");
    }

    /** 512x512 logo, source viewport 108. Scale factor 512/108. */
    static void renderIcon() throws Exception {
        int W = 512;
        BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Scale the 108-viewport coordinates up to 512.
        double s = (double) W / 108.0;
        g.scale(s, s);

        paintLogoComposition(g);

        g.dispose();
        ImageIO.write(img, "png", new File("play-store/icon-512.png"));
    }

    /** 1024x500 banner: logo on the left (~300x300), word-mark + tagline on the right. */
    static void renderFeatureGraphic() throws Exception {
        int W = 1024, H = 500;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Solid green fill
        g.setColor(FOREST_GREEN);
        g.fillRect(0, 0, W, H);

        // Logo: rendered at 350x350 starting at (75, 75). Scale 350/108.
        Graphics2D logoG = (Graphics2D) g.create();
        logoG.translate(75, 75);
        logoG.scale(350.0 / 108.0, 350.0 / 108.0);
        // Repaint the basket+fruits, but skip the background rectangle (the
        // banner already filled green).
        paintLogoComposition(logoG, /*paintBackground=*/false);
        logoG.dispose();

        // Word-mark "Pantry"
        g.setColor(CREAM);
        g.setFont(new Font("SansSerif", Font.BOLD, 110));
        FontMetrics fmTitle = g.getFontMetrics();
        int titleX = 480;
        int titleY = 250;
        g.drawString("Pantry", titleX, titleY);

        // Tagline
        g.setColor(WEAVE_TAN);
        g.setFont(new Font("SansSerif", Font.PLAIN, 36));
        g.drawString("Your household's shared kitchen", titleX, titleY + 60);

        g.dispose();
        ImageIO.write(img, "png", new File("play-store/feature-graphic.png"));
    }

    /** Paints the logo composition into the given Graphics2D, assumed to be
     *  pre-scaled to a 108-unit coordinate system. */
    static void paintLogoComposition(Graphics2D g) {
        paintLogoComposition(g, true);
    }

    static void paintLogoComposition(Graphics2D g, boolean paintBackground) {
        if (paintBackground) {
            g.setColor(FOREST_GREEN);
            g.fillRect(0, 0, 108, 108);
        }

        // Basket trapezoid: M28,58 Q54,52 80,58 L76,84 L32,84 Z
        Path2D basket = new Path2D.Double();
        basket.moveTo(28, 58);
        basket.quadTo(54, 52, 80, 58);
        basket.lineTo(76, 84);
        basket.lineTo(32, 84);
        basket.closePath();
        g.setColor(CREAM);
        g.fill(basket);

        // Weave band 1: M30,64 Q54,60 78,64 L78,68 Q54,64 30,68 Z
        Path2D band1 = new Path2D.Double();
        band1.moveTo(30, 64);
        band1.quadTo(54, 60, 78, 64);
        band1.lineTo(78, 68);
        band1.quadTo(54, 64, 30, 68);
        band1.closePath();

        // Weave band 2: M31,73 Q54,69 77,73 L77,77 Q54,73 31,77 Z
        Path2D band2 = new Path2D.Double();
        band2.moveTo(31, 73);
        band2.quadTo(54, 69, 77, 73);
        band2.lineTo(77, 77);
        band2.quadTo(54, 73, 31, 77);
        band2.closePath();

        g.setColor(WEAVE_TAN);
        g.fill(band1);
        g.fill(band2);

        // Three fruits (circles). Each as: center (cx, cy), radius r.
        // fillOval takes top-left + width/height.
        // Left apple: cx=36, cy=46, r=10
        g.setColor(APPLE_RED);
        g.fillOval(36 - 10, 46 - 10, 20, 20);

        // Centre pear: cx=54, cy=42, r=12
        g.setColor(PEAR_YELLOW);
        g.fillOval(54 - 12, 42 - 12, 24, 24);

        // Right grape: cx=72, cy=46, r=10
        g.setColor(GRAPE_PURPLE);
        g.fillOval(72 - 10, 46 - 10, 20, 20);

        // Leaf: M54,30 Q60,18 66,28 Q60,32 54,30 Z
        Path2D leaf = new Path2D.Double();
        leaf.moveTo(54, 30);
        leaf.quadTo(60, 18, 66, 28);
        leaf.quadTo(60, 32, 54, 30);
        leaf.closePath();
        g.setColor(LEAF_GREEN);
        g.fill(leaf);
    }
}
