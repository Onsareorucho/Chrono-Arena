package client.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

public class MainMenuScreen extends JPanel {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static final Color BG_TOP = new Color(20, 20, 40);
    private static final Color BG_BOTTOM = new Color(40, 20, 60);
    private static final Color ACCENT = new Color(0, 200, 255);
    private static final Color ACCENT_HOVER = new Color(100, 220, 255);
    private static final Color BUTTON_BG = new Color(50, 50, 80);
    private static final Color BUTTON_HOVER = new Color(70, 70, 110);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(180, 180, 200);

    private JTextField nameField;

    private MenuButton playButton;
    private MenuButton quitButton;

    private float titleGlow = 0f;
    private Timer animationTimer;

    private PlayCallback onPlayClicked;

    private String statusMessage = "";
    private Color statusColor = Color.WHITE;

    public interface PlayCallback {
        void onPlay(String playerName);
    }

    public MainMenuScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setLayout(null);
        setFocusable(true);
 
        setupInputFields();
        setupButtons();
        startAnimation();
    }

    private void setupInputFields() {
        nameField = createStyledTextField("Enter your name...");
        nameField.setBounds(WIDTH / 2 - 150, 300, 300, 40);
        add(nameField);
 
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handlePlay();
                }
            }
        });
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 18));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(new Color(30, 30, 50));
        field.setCaretColor(ACCENT);
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(80, 80, 120), 2),
            javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        field.setHorizontalAlignment(JTextField.CENTER);
 
        // Placeholder text behavior
        field.setText(placeholder);
        field.setForeground(TEXT_SECONDARY);
 
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
 
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
            }
        });
 
        return field;
    }
    
    private void setupButtons() {
        playButton = new MenuButton("PLAY", WIDTH / 2 - 100, 420, 200, 50);
        playButton.setOnClick(this::handlePlay);
 
        quitButton = new MenuButton("QUIT", WIDTH / 2 - 80, 490, 160, 40);
        quitButton.setOnClick(() -> System.exit(0));
 
        // Mouse listener for button hover/click detection
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onButton = playButton.contains(e.getX(), e.getY()) 
                                || quitButton.contains(e.getX(), e.getY());
                setCursor(onButton ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                   : Cursor.getDefaultCursor());
                playButton.setHovered(playButton.contains(e.getX(), e.getY()));
                quitButton.setHovered(quitButton.contains(e.getX(), e.getY()));
                repaint();
            }
 
            @Override
            public void mousePressed(MouseEvent e) {
                if (playButton.contains(e.getX(), e.getY())) {
                    playButton.onClick();
                } else if (quitButton.contains(e.getX(), e.getY())) {
                    quitButton.onClick();
                }
            }
        };
 
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void startAnimation() {
        animationTimer = new Timer(50, e -> {
            titleGlow += 0.05f;
            if (titleGlow > Math.PI * 2) titleGlow = 0;
            repaint();
        });
        animationTimer.start();
    }

    private void handlePlay() {
        String name = nameField.getText().trim();
 
        // Validate name
        if (name.isEmpty() || name.equals("Enter your name...")) {
            setStatus("Please enter your name!", Color.ORANGE);
            nameField.requestFocus();
            return;
        }
 
        // Clear status and notify callback
        setStatus("Connecting...", ACCENT);
 
        if (onPlayClicked != null) {
            onPlayClicked.onPlay(name);
        }
    }

    public void setOnPlayClicked(PlayCallback callback) {
        this.onPlayClicked = callback;
    }

    public void setStatus(String message, Color color) {
        this.statusMessage = message;
        this.statusColor = color;
        repaint();
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 
        GradientPaint gradient = new GradientPaint(0, 0, BG_TOP, 0, HEIGHT, BG_BOTTOM);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
 
        drawBackgroundParticles(g2d);
 
        drawTitle(g2d);
 
        drawLabels(g2d);
 
        playButton.draw(g2d);
        quitButton.draw(g2d);
 
        if (!statusMessage.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(statusMessage)) / 2;
            g2d.setColor(statusColor);
            g2d.drawString(statusMessage, x, 400);
        }
 
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(TEXT_SECONDARY);
        String footer = "WASD to move • SPACE to freeze • E to use powerup";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(footer, (WIDTH - fm.stringWidth(footer)) / 2, HEIGHT - 30);
    }

    private void drawBackgroundParticles(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 20));
        long time = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            float x = (float) ((Math.sin(time * 0.001 + i * 0.5) + 1) * WIDTH / 2);
            float y = (float) ((Math.cos(time * 0.0008 + i * 0.7) + 1) * HEIGHT / 2);
            int size = 2 + (i % 3);
            g2d.fillOval((int) x, (int) y, size, size);
        }
    }

    private void drawTitle(Graphics2D g2d) {
        String title = "CHRONOARENA";
        Font titleFont = new Font("Arial", Font.BOLD, 72);
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
 
        int x = (WIDTH - fm.stringWidth(title)) / 2;
        int y = 150;
 
        // Glow effect
        float glowIntensity = (float) (Math.sin(titleGlow) + 1) / 2 * 0.5f + 0.5f;
        Color glowColor = new Color(
            (int) (ACCENT.getRed() * glowIntensity),
            (int) (ACCENT.getGreen() * glowIntensity),
            (int) (ACCENT.getBlue() * glowIntensity),
            100
        );
 
        // Draw glow layers
        g2d.setColor(glowColor);
        for (int i = 5; i > 0; i--) {
            g2d.drawString(title, x - i, y);
            g2d.drawString(title, x + i, y);
            g2d.drawString(title, x, y - i);
            g2d.drawString(title, x, y + i);
        }
 
        // Draw main title
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString(title, x, y);
 
        // Subtitle
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(TEXT_SECONDARY);
        String subtitle = "Control Time. Capture Zones. Dominate.";
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, y + 35);
    }

    private void drawLabels(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(TEXT_SECONDARY);
 
        g2d.drawString("PLAYER NAME", WIDTH / 2 - 150, 290);
    }


    private static class MenuButton {
        private String text;
        private int x, y, width, height;
        private boolean hovered = false;
        private Runnable onClick;
 
        public MenuButton(String text, int x, int y, int width, int height) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
 
        public void setOnClick(Runnable action) {
            this.onClick = action;
        }
 
        public void setHovered(boolean hovered) {
            this.hovered = hovered;
        }
 
        public boolean contains(int px, int py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
 
        public void onClick() {
            if (onClick != null) onClick.run();
        }
 
        public void draw(Graphics2D g2d) {
            // Button background
            Color bgColor = hovered ? BUTTON_HOVER : BUTTON_BG;
            Color borderColor = hovered ? ACCENT_HOVER : ACCENT;
 
            g2d.setColor(bgColor);
            g2d.fill(new RoundRectangle2D.Float(x, y, width, height, 10, 10));
 
            g2d.setColor(borderColor);
            g2d.setStroke(new java.awt.BasicStroke(2));
            g2d.draw(new RoundRectangle2D.Float(x, y, width, height, 10, 10));
 
            // Button text
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + (width - fm.stringWidth(text)) / 2;
            int textY = y + (height + fm.getAscent() - fm.getDescent()) / 2;
 
            g2d.setColor(hovered ? ACCENT_HOVER : TEXT_PRIMARY);
            g2d.drawString(text, textX, textY);
        }
    }

}