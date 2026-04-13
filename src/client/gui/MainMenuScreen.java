package client.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

public class MainMenuScreen extends JPanel {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static final Color ACCENT = new Color(201, 180, 117);
    private static final Color ACCENT_HOVER = new Color(255, 242, 201);
    private static final Color BUTTON_BG = new Color(169, 145, 71);
    private static final Color BUTTON_HOVER = new Color(204, 185, 129);
    private static final Color TEXT_PRIMARY = new Color(255, 210, 87);
    private static final Color TEXT_SECONDARY = new Color(255, 242, 201);

    private JTextField nameField;
    private JTextField ipField;

    private MenuButton playButton;
    private MenuButton quitButton;

    private Timer animationTimer;

    private PlayCallback onPlayClicked;

    private String statusMessage = "";
    private Color statusColor = Color.WHITE;

    private SpriteManager spriteManager;

    public interface PlayCallback {
        void onPlayClicked(String playerName, String serverIp);
    }

    public MainMenuScreen() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setLayout(null);
        setFocusable(true);

        spriteManager = new SpriteManager();
        spriteManager.loadSprite("menu_background", "menu_background.png");
        
        setupInputFields();
        setupButtons();
        startAnimation();
    }

    private void setupInputFields() {
        nameField = createStyledTextField("Enter your name...");
        nameField.setBounds(WIDTH / 2 - 150, 210, 300, 40);
        add(nameField);

        ipField = createStyledTextField("Server IP (e.g. 192.168.1.10)");
        ipField.setBounds(WIDTH / 2 - 150, 290, 300, 40);
        add(ipField);

        KeyAdapter enterHandler = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handlePlay();
            }
        };
        nameField.addKeyListener(enterHandler);
        ipField.addKeyListener(enterHandler);
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
        playButton = new MenuButton("PLAY", WIDTH / 2 - 100, 370, 200, 50);
        playButton.setOnClick(this::handlePlay);

        quitButton = new MenuButton("QUIT", WIDTH / 2 - 80, 455, 160, 40);
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
            repaint();
        });
        animationTimer.start();
    }

    private void handlePlay() {
        String name = nameField.getText().trim();
        String ip   = ipField.getText().trim();

        if (name.isEmpty() || name.equals("Enter your name...")) {
            setStatus("Please enter your name!", Color.ORANGE);
            nameField.requestFocus();
            return;
        }

        if (ip.isEmpty() || ip.equals("Server IP (e.g. 192.168.1.10)")) {
            setStatus("Please enter the server IP!", Color.ORANGE);
            ipField.requestFocus();
            return;
        }

        setStatus("Connecting...", ACCENT);

        if (onPlayClicked != null) {
            onPlayClicked.onPlayClicked(name, ip);
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

        // Draw background image
        if (spriteManager.hasSprite("menu_background")) {
            BufferedImage bg = spriteManager.getSprite("menu_background");
            g2d.drawImage(bg, 0, 0, WIDTH, HEIGHT, null);
        } else {
            // Fallback solid color if image not loaded
            g2d.setColor(new Color(30, 30, 50));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        drawLabels(g2d);
 
        playButton.draw(g2d);
        quitButton.draw(g2d);
 
        if (!statusMessage.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(statusMessage)) / 2;
            g2d.setColor(statusColor);
            g2d.drawString(statusMessage, x, 300);
        }
    }

    private void drawLabels(Graphics2D g2d) {
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(TEXT_SECONDARY);
        g2d.drawString("PLAYER NAME", WIDTH / 2 - 150, 200);
        g2d.drawString("SERVER IP", WIDTH / 2 - 150, 280);
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