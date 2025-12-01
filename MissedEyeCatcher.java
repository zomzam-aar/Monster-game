import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;

/**
 * +++++++++Missed Eye Catcher - renamed from Catch The Shape
 */
public class MissedEyeCatcher extends JPanel implements ActionListener {

    static final int WIDTH = 720;
    static final int HEIGHT = 424;

    Timer timer;
    Random random = new Random();

    String shapeType = "Circle";
    int shapeX, shapeY;
    int shapeSize = 20;
    int shapeSpeed = 3;
    double rotation = 0;

    double scale = 1.0;

    int playerX = 160;
    int playerY = 360;
    int playerWidth = 160;
    int playerHeight = 75;
    double mouseTargetX = playerX;
    double mouseSmoothing = 0.15;

    int score = 0;
    int level = 1;
    int lives = 3;
    int highScore = 0;

    Color bgColor = Color.black;
    long effectEndTime = 0;
    BufferedImage backgroundImage = null;
    BufferedImage shapeImage = null;
    BufferedImage startBackground = null;
    BufferedImage heartImage = null;
    BufferedImage gameOverImage = null;
    String catchText = null;
    int catchTextX = 0, catchTextY = 0;
    long catchTextEndTime = 0;
    Image monsterImage = null;
    Clip startMusicClip = null;
    Clip gameMusicClip = null;
    Timer welcomeVoiceTimer = null;
    Timer gameVoiceTimer = null;
    String welcomeVoiceFile = "roar-echo.wav";
    final List<Clip> activeClips = new ArrayList<>();
    final Map<String, byte[]> preloadedSounds = new HashMap<>();

    String playerName = "Player";

    boolean inWelcomeScreen = true;
    boolean gameRunning = false;
    boolean inGameOver = false;

    JTextField nameField;
    
    JButton startButton;
    JButton btnNewGame;
    JButton btnExit;
    JButton btnMainMenu;

    int numStars = 50;
    int[][] stars = new int[numStars][2];
    Random starRandom = new Random();
    int spawnCount = 0;
    boolean shapeIsHeart = false;
    
    // Enhanced gameplay features
    int combo = 0;
    int maxCombo = 0;
    long lastCatchTime = 0;
    static final long COMBO_TIMEOUT = 3000; // 3 seconds to maintain combo
    
    // Particle system
    List<Particle> particles = new ArrayList<>();
    
    // Visual effects
    float shakeX = 0, shakeY = 0;
    long shakeEndTime = 0;
    
    // Power-up system
    boolean slowMotion = false;
    long slowMotionEndTime = 0;

    // Particle class for visual effects
    class Particle {
        float x, y, vx, vy;
        Color color;
        int life;
        int maxLife;
        
        Particle(float x, float y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (float)(random.nextFloat() * 4 - 2);
            this.vy = (float)(random.nextFloat() * 4 - 2);
            this.maxLife = 30 + random.nextInt(20);
            this.life = maxLife;
        }
        
        void update() {
            x += vx;
            y += vy;
            vy += 0.2f; // gravity
            life--;
        }
        
        void draw(Graphics2D g) {
            float alpha = (float)life / maxLife;
            g.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, alpha));
            int size = (int)(4 * alpha) + 2;
            g.fillOval((int)x, (int)y, size, size);
        }
        
        boolean isDead() { return life <= 0; }
    }
    
    public MissedEyeCatcher() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseTargetX = e.getX() - playerWidth / 2;
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseTargetX = e.getX() - playerWidth / 2;
            }
        });
        timer = new Timer(20, this);

        try {
            File f = new File("./assets/BackGround.png");
            if (f.exists()) backgroundImage = ImageIO.read(f);
            File sf = new File("./assets/fireball.png");
            if (sf.exists()) shapeImage = ImageIO.read(sf);
            File hf = new File("./assets/heart.png");
            if (hf.exists()) heartImage = ImageIO.read(hf);
            File go = new File("./assets/game_over.png");
            if (go.exists()) gameOverImage = ImageIO.read(go);
            File sb = new File("./assets/StartBackGround.png");
            if (sb.exists()) startBackground = ImageIO.read(sb);
            File mg = new File("./assets/monster.gif");
            if (mg.exists()) monsterImage = new ImageIcon(mg.getPath()).getImage();
        } catch (IOException ignored) {}

        // preload essential short sounds into memory to avoid first-play disk I/O delays
        preloadEssentialSounds();

        createWelcomeScreen();
        playStartMusicLoop();
    }

    void preloadEssentialSounds() {
        String[] essentials = new String[] {
            "./assets/gameover.wav",
            "./assets/miss.wav",
            "./assets/eating1.wav",
            "./assets/eating2.wav",
            "./assets/lose-heart.wav"
        };
        for (String p : essentials) {
            try {
                File f = new File(p);
                if (!f.exists()) continue;
                byte[] data = Files.readAllBytes(Paths.get(p));
                preloadedSounds.put(p, data);
            } catch (Exception ignored) {}
        }
    }

    void createWelcomeScreen() {
        setLayout(null);
        JLabel nameLabel = new JLabel("Enter your name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBounds(120, 100, 150, 25);
        add(nameLabel);

        nameField = new JTextField("Player");
        nameField.setBounds(120, 130, 150, 25);
        add(nameField);

        startButton = new JButton("▶️ Start Game");
        startButton.setBounds(110, 250, 170, 40);
        styleButton(startButton, new Color(46, 204, 113), Color.WHITE);
        startButton.addActionListener(e -> startGame());
        add(startButton);

        setBackground(Color.BLACK);

        File vf = new File("./assets/" + welcomeVoiceFile);
        if (vf.exists()) {
            playSound("./assets/" + welcomeVoiceFile);
            if (welcomeVoiceTimer != null) welcomeVoiceTimer.stop();
            welcomeVoiceTimer = new Timer(15000, ev -> playSound("./assets/" + welcomeVoiceFile));
            welcomeVoiceTimer.setInitialDelay(15000);
            welcomeVoiceTimer.setRepeats(true);
            welcomeVoiceTimer.start();
        }
    }

    void startGame() {
        playerName = nameField.getText();
        inWelcomeScreen = false;
        if (welcomeVoiceTimer != null) { welcomeVoiceTimer.stop(); welcomeVoiceTimer = null; }
        gameRunning = true;
        removeAll();
        setLayout(null);

        btnNewGame = new JButton("🔄 New Game");
        btnMainMenu = new JButton("🏠 Menu");
        btnExit = new JButton("❌ Exit");
        
        // Position buttons vertically on the right side with better spacing
        int btnWidth = 140;
        int btnHeight = 35;
        int marginRight = 15;
        int marginTop = 15;
        int spacing = 12;
        int xPos = WIDTH - marginRight - btnWidth;
        
        btnNewGame.setBounds(xPos, marginTop, btnWidth, btnHeight);
        btnMainMenu.setBounds(xPos, marginTop + btnHeight + spacing, btnWidth, btnHeight);
        btnExit.setBounds(xPos, marginTop + (btnHeight + spacing) * 2, btnWidth, btnHeight);
        
        // Style buttons
        styleButton(btnNewGame, new Color(46, 204, 113), Color.WHITE); // Green
        styleButton(btnMainMenu, new Color(52, 152, 219), Color.WHITE); // Blue
        styleButton(btnExit, new Color(231, 76, 60), Color.WHITE); // Red
        
        btnNewGame.addActionListener(e -> restartGame());
        btnMainMenu.addActionListener(e -> goToMainMenu());
        btnExit.addActionListener(e -> System.exit(0));
        
        add(btnNewGame);
        add(btnMainMenu);
        add(btnExit);

        revalidate();
        repaint();

        resetShape();

        for (int i = 0; i < numStars; i++) {
            stars[i][0] = starRandom.nextInt(WIDTH);
            stars[i][1] = starRandom.nextInt(HEIGHT);
        }

        timer.start();
        requestFocusInWindow();
        stopStartMusic();
        stopAllShortClips();
        playGameMusicLoop();
        File gv = new File("./assets/monster-growl.wav");
        if (gv.exists()) {
            playSound("./assets/monster-growl.wav");
            if (gameVoiceTimer != null) gameVoiceTimer.stop();
            gameVoiceTimer = new Timer(10000, ev -> playSound("./assets/monster-growl.wav"));
            gameVoiceTimer.setInitialDelay(10000);
            gameVoiceTimer.setRepeats(true);
            gameVoiceTimer.start();
        }
    }



    void resetShape() {
        spawnCount++;
        if (spawnCount % 15 == 0) {
            shapeIsHeart = true;
            shapeSize = 30;
        } else {
            shapeIsHeart = false;
            if (spawnCount % 10 == 0) {
                shapeSize = 50;
            } else {
                shapeSize = 35;
            }
        }
        shapeX = random.nextInt(Math.max(1, WIDTH - shapeSize));
        shapeY = 0;
        scale = 1.0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        
        // Apply slow motion effect if active
        int actualSpeed = slowMotion && System.currentTimeMillis() < slowMotionEndTime ? shapeSpeed / 2 : shapeSpeed;
        shapeY += actualSpeed;
        
        // Rotate shapes for visual appeal
        rotation += 0.05;
        
        // Update particles
        particles.removeIf(Particle::isDead);
        for (Particle p : particles) p.update();
        
        // Check combo timeout
        if (System.currentTimeMillis() - lastCatchTime > COMBO_TIMEOUT && combo > 0) {
            combo = 0;
        }
        
        // Update screen shake
        if (System.currentTimeMillis() < shakeEndTime) {
            shakeX = (float)(Math.random() * 6 - 3);
            shakeY = (float)(Math.random() * 6 - 3);
        } else {
            shakeX = shakeY = 0;
        }

        int dynamicSizeNow = (int)(shapeSize * scale);
        if (shapeY + dynamicSizeNow >= playerY &&
                shapeX + dynamicSizeNow >= playerX &&
                shapeX <= playerX + playerWidth) {
            if (shapeIsHeart) {
                lives++;
                combo++;
                lastCatchTime = System.currentTimeMillis();
                catchText = "+1 Life";
                catchTextX = shapeX + dynamicSizeNow/2;
                catchTextY = playerY - 10;
                catchTextEndTime = System.currentTimeMillis() + 1000;
                playSound("./assets/eating1.wav");
                spawnParticles(shapeX + dynamicSizeNow/2, shapeY + dynamicSizeNow/2, Color.PINK, 15);
                resetShape();
                bgColor = Color.GREEN.darker();
                effectEndTime = System.currentTimeMillis() + 200;
            } else {
                combo++;
                lastCatchTime = System.currentTimeMillis();
                if (combo > maxCombo) maxCombo = combo;
                
                int gained = 1;
                if (dynamicSizeNow >= 50) gained = 2;
                
                // Combo bonus: every 5 combo adds 1 extra point
                int comboBonus = combo / 5;
                gained += comboBonus;
                
                score += gained;
                
                if (gained > 1 || combo >= 5) {
                    String bonusText = gained > 2 ? "+" + gained : combo >= 5 ? "COMBO x" + combo : "2x";
                    catchText = bonusText;
                    catchTextX = shapeX + dynamicSizeNow/2;
                    catchTextY = playerY - 10;
                    catchTextEndTime = System.currentTimeMillis() + 800;
                }

                String[] eats = {"./assets/eating1.wav", "./assets/eating2.wav"};
                playSound(eats[random.nextInt(eats.length)]);
                
                // Spawn particles based on combo
                Color particleColor = combo >= 10 ? Color.YELLOW : combo >= 5 ? Color.ORANGE : Color.CYAN;
                spawnParticles(shapeX + dynamicSizeNow/2, shapeY + dynamicSizeNow/2, particleColor, 10 + combo);
                
                resetShape();
                bgColor = Color.darkGray;
                effectEndTime = System.currentTimeMillis() + 200;

                if (score % 5 == 0) { level++; shapeSpeed++; }
                if (score > highScore) highScore = score;
                
                // Activate slow motion every 20 points
                if (score > 0 && score % 20 == 0) {
                    slowMotion = true;
                    slowMotionEndTime = System.currentTimeMillis() + 3000;
                }
            }
        }

        if (shapeY > HEIGHT) {
            File lf = new File("./assets/lose-heart.wav");
            if (lf.exists()) playSound("./assets/lose-heart.wav"); else playSound("./assets/miss.wav");
            
            // Reset combo on miss
            combo = 0;
            
            lives--;
            if (lives <= 0) {
                gameOver();
                return;
            }
            
            // Screen shake on miss
            shakeEndTime = System.currentTimeMillis() + 200;
            spawnParticles(WIDTH/2, HEIGHT - 20, Color.RED, 20);
            
            resetShape();
            bgColor = Color.red;
            effectEndTime = System.currentTimeMillis() + 200;
        }

        if (System.currentTimeMillis() > effectEndTime)
            bgColor = Color.black;

        for (int i = 0; i < numStars; i++) {
            stars[i][1] += 1 + level * 0.1;
            if (stars[i][1] > HEIGHT) stars[i][1] = 0;
        }

        double dx = mouseTargetX - playerX;
        playerX += (int) Math.signum(dx) * Math.max(1, (int)(Math.abs(dx) * mouseSmoothing));
        if (playerX < 0) playerX = 0;
        if (playerX + playerWidth > WIDTH) playerX = WIDTH - playerWidth;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Apply screen shake
        g2d.translate(shakeX, shakeY);

        if (inWelcomeScreen) {
            if (startBackground != null) {
                g2d.drawImage(startBackground, 0, 0, WIDTH, HEIGHT, null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
            }
            return;
        }

        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);
            g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 60));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 30), 0, HEIGHT, Color.BLACK);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        g2d.setColor(Color.WHITE);
        for (int i = 0; i < numStars; i++)
            g2d.fillOval(stars[i][0], stars[i][1], 2, 2);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int dynamicSize = (int)(shapeSize * scale);
        if (shapeIsHeart) {
            if (heartImage != null) {
                g2d.drawImage(heartImage, shapeX, shapeY, dynamicSize, dynamicSize, null);
            } else {
                g2d.setColor(Color.RED);
                int cx = shapeX + dynamicSize/2;
                int cy = shapeY + dynamicSize/2;
                int[] xs = {cx, cx - dynamicSize/2, cx - dynamicSize/3, cx, cx + dynamicSize/3, cx + dynamicSize/2};
                int[] ys = {shapeY, shapeY + dynamicSize/3, shapeY + dynamicSize, cy + dynamicSize/3, shapeY + dynamicSize, shapeY + dynamicSize/3};
                g2d.fillPolygon(xs, ys, xs.length);
            }
        } else if (shapeImage != null) {
            AffineTransform old = g2d.getTransform();
            double cx = shapeX + dynamicSize/2.0;
            double cy = shapeY + dynamicSize/2.0;
            g2d.translate(cx, cy);
            g2d.rotate(rotation);
            g2d.drawImage(shapeImage, -dynamicSize/2, -dynamicSize/2, dynamicSize, dynamicSize, null);
            g2d.setTransform(old);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.translate(shapeX + shapeSize / 2, shapeY + shapeSize / 2);
            g2d.rotate(rotation);
            switch (shapeType) {
                case "Circle" -> g2d.fillOval(-dynamicSize/2, -dynamicSize/2, dynamicSize, dynamicSize);
                case "Square" -> g2d.fillRect(-dynamicSize/2, -dynamicSize/2, dynamicSize, dynamicSize);
                case "Star" -> drawStar(g2d, 0, 0, dynamicSize/2, dynamicSize/4, 5);
            }
            g2d.rotate(-rotation);
            g2d.translate(-(shapeX + shapeSize / 2), -(shapeY + shapeSize / 2));
        }

        if (monsterImage != null) {
            g2d.drawImage(monsterImage, playerX, playerY - (playerHeight), playerWidth, playerHeight*2, null);
        } else {
            g2d.setColor(Color.CYAN);
            g2d.fillRoundRect(playerX, playerY, playerWidth, playerHeight, 5, 5);
        }

        g2d.setColor(Color.WHITE);
        g2d.drawString("Player: " + playerName, 10, 20);
        g2d.drawString("Score: " + score, 10, 40);
        g2d.drawString("Level: " + level, 10, 60);
        g2d.drawString("High: " + highScore, 300, 20);
        
        // Draw combo counter
        if (combo > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            Color comboColor = combo >= 10 ? Color.YELLOW : combo >= 5 ? Color.ORANGE : Color.CYAN;
            g2d.setColor(comboColor);
            g2d.drawString("COMBO: " + combo, WIDTH - 200, 30);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        }
        
        // Draw max combo
        if (maxCombo > 0) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("Max Combo: " + maxCombo, WIDTH - 200, 50);
        }
        
        // Draw slow motion indicator
        if (slowMotion && System.currentTimeMillis() < slowMotionEndTime) {
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(Color.CYAN);
            g2d.drawString("⏱ SLOW MOTION", WIDTH/2 - 70, HEIGHT - 20);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        }

        if (heartImage != null) {
            int hw = 18;
            int hh = 18;
            for (int i = 0; i < lives; i++) {
                int x = 10 + i * (hw + 6);
                int y = 60;
                g2d.drawImage(heartImage, x, y, hw, hh, null);
            }
        } else {
            g2d.setColor(Color.PINK);
            for (int i = 0; i < lives; i++)
                g2d.fillOval(10 + i * 15, 70, 10, 10);
        }

        if (catchText != null && System.currentTimeMillis() < catchTextEndTime) {
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(Color.ORANGE);
            int tw = g2d.getFontMetrics().stringWidth(catchText);
            g2d.drawString(catchText, catchTextX - tw/2, catchTextY);
        } else {
            catchText = null;
        }
        
        // Draw particles
        for (Particle p : particles) {
            p.draw(g2d);
        }
        
        // Reset transform (undo shake)
        g2d.translate(-shakeX, -shakeY);
    }

    void drawStar(Graphics2D g, int x, int y, int radius1, int radius2, int points) {
        double angle = Math.PI / points;
        Polygon p = new Polygon();
        for (int i = 0; i < 2*points; i++) {
            double r = (i % 2 == 0) ? radius1 : radius2;
            double a = i * angle;
            p.addPoint((int)(x + Math.cos(a)*r), (int)(y + Math.sin(a)*r));
        }
        g.fillPolygon(p);
    }
    
    void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color));
        }
    }
    
    void styleButton(JButton btn, Color bgColor, Color fgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createLineBorder(bgColor.darker(), 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        btn.addMouseListener(new MouseAdapter() {
            Color originalBg = bgColor;
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(originalBg);
            }
        });
    }

    void playSound(String fileName) {
        // Load and play short sounds off the Event Dispatch Thread to avoid UI freezes
        new Thread(() -> {
            try {
                AudioInputStream audio;
                byte[] data = preloadedSounds.get(fileName);
                if (data != null) {
                    audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
                } else {
                    File f = new File(fileName);
                    if (!f.exists()) return;
                    audio = AudioSystem.getAudioInputStream(f);
                }
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                synchronized (activeClips) { activeClips.add(clip); }
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        try { clip.close(); } catch (Exception ignored) {}
                        synchronized (activeClips) { activeClips.remove(clip); }
                    }
                });
                clip.start();
            } catch (Exception ignored) {}
        }, "AudioPlayer").start();
    }

    void playStartMusicLoop() {
        // Load looping start music in background to avoid blocking the EDT
        new Thread(() -> {
            try {
                File f = new File("./assets/start.wav");
                if (!f.exists()) return;
                AudioInputStream audio = AudioSystem.getAudioInputStream(f);
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                // store reference so it can be stopped later
                startMusicClip = clip;
            } catch (Exception e) {
                startMusicClip = null;
            }
        }, "StartMusicLoader").start();
    }

    void stopStartMusic() {
        try {
            if (startMusicClip != null && startMusicClip.isRunning()) {
                startMusicClip.stop();
                startMusicClip.close();
            }
        } catch (Exception ignored) {}
        startMusicClip = null;
    }

    void stopAllShortClips() {
        synchronized (activeClips) {
            for (Clip c : new ArrayList<>(activeClips)) {
                try { if (c.isRunning()) c.stop(); c.close(); } catch (Exception ignored) {}
            }
            activeClips.clear();
        }
    }

    void gameOver() {
        // Stop the game loop first
        timer.stop();
        // set flags so UI knows state
        gameRunning = false;
        inGameOver = true;
        if (gameVoiceTimer != null) { gameVoiceTimer.stop(); gameVoiceTimer = null; }

        // Build and show the Game Over UI first so player sees it immediately
        removeAll();
        setLayout(null);
        createGameOverScreen();
        revalidate();
        repaint();
        // Force immediate paint of the panel to ensure the Game Over screen is visible
        try {
            this.paintImmediately(0, 0, WIDTH, HEIGHT);
        } catch (Exception ignored) {}

        // Now stop music and other clips (quick operations) and play the game-over sound asynchronously
        stopGameMusic();
        stopAllShortClips();
        playSound("./assets/gameover.wav");
    }

    void createGameOverScreen() {
        int imageBottomY = 0;
        if (gameOverImage != null) {
            final int drawW = 250;
            final int drawH = 250;
            final int imgX = (WIDTH - drawW) / 2;
            final int imgY = (HEIGHT - drawH) / 2;
            JComponent imgComp = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.drawImage(gameOverImage, imgX, imgY, drawW, drawH, null);
                }
            };
            imgComp.setBounds(0, 0, WIDTH, HEIGHT);
            add(imgComp);
            imageBottomY = imgY + drawW;
        } else {
            JLabel over = new JLabel("Game Over");
            over.setForeground(Color.WHITE);
            over.setFont(new Font("Arial", Font.BOLD, 36));
            over.setBounds(WIDTH/2 - 150, 80, 300, 50);
            over.setHorizontalAlignment(SwingConstants.CENTER);
            add(over);
            imageBottomY = 80 + 50;
        }

        JLabel stats = new JLabel("Score: " + score + "    High: " + highScore);
        stats.setForeground(Color.WHITE);
        stats.setFont(new Font("Arial", Font.PLAIN, 18));
        stats.setBounds(WIDTH/2 - 150, imageBottomY + 10, 300, 30);
        stats.setHorizontalAlignment(SwingConstants.CENTER);
        add(stats);

        JButton restart = new JButton("Restart");
        JButton main = new JButton("Main Menu");
        JButton exit = new JButton("Exit");
        int bw = 120;
        int spacing = 8;
        int totalW = bw * 3 + spacing * 2;
        int startX = (WIDTH - totalW) / 2;
        int btnY = imageBottomY + 45;
        restart.setBounds(startX, btnY, bw, 28);
        main.setBounds(startX + bw + spacing, btnY, bw, 28);
        exit.setBounds(startX + (bw + spacing) * 2, btnY, bw, 28);

        restart.addActionListener(e -> {
            removeAll();
            setLayout(null);
            inGameOver = false;
            restartGame();
        });
        main.addActionListener(e -> goToMainMenu());
        exit.addActionListener(e -> System.exit(0));

        add(restart);
        add(main);
        add(exit);

        setBackground(Color.BLACK);
    }

    void goToMainMenu() {
        timer.stop();
        stopGameMusic();
        stopAllShortClips();
        gameRunning = false;
        inWelcomeScreen = true;
        if (welcomeVoiceTimer != null) { welcomeVoiceTimer.stop(); welcomeVoiceTimer = null; }
        if (gameVoiceTimer != null) { gameVoiceTimer.stop(); gameVoiceTimer = null; }

        score = 0;
        level = 1;
        lives = 3;
        shapeSpeed = 3;
        combo = 0;
        maxCombo = 0;
        particles.clear();
        slowMotion = false;

        removeAll();
        setLayout(null);
        createWelcomeScreen();
        revalidate();
        repaint();

        playStartMusicLoop();
    }

    void playGameMusicLoop() {
        // Load and start looping game music on a background thread to avoid blocking
        new Thread(() -> {
            try {
                try { if (gameMusicClip != null && gameMusicClip.isRunning()) { gameMusicClip.stop(); gameMusicClip.close(); } } catch (Exception ignored) {}
                File f = new File("./assets/game.wav");
                if (!f.exists()) return;
                AudioInputStream audio = AudioSystem.getAudioInputStream(f);
                Clip clip = AudioSystem.getClip();
                clip.open(audio);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                gameMusicClip = clip;
            } catch (Exception ignored) { gameMusicClip = null; }
        }, "GameMusicLoader").start();
    }

    void stopGameMusic() {
        try {
            if (gameMusicClip != null && gameMusicClip.isRunning()) {
                gameMusicClip.stop();
                gameMusicClip.close();
            }
        } catch (Exception ignored) {}
        gameMusicClip = null;
    }

    void restartGame() {
        score = 0;
        level = 1;
        lives = 3;
        shapeSpeed = 3;
        combo = 0;
        maxCombo = 0;
        particles.clear();
        slowMotion = false;
        removeAll();
        setLayout(null);

        btnNewGame = new JButton("🔄 New Game");
        btnMainMenu = new JButton("🏠 Menu");
        btnExit = new JButton("❌ Exit");
        
        // Position buttons vertically on the right side with better spacing
        int btnWidth = 140;
        int btnHeight = 35;
        int marginRight = 15;
        int marginTop = 15;
        int spacing = 12;
        int xPos = WIDTH - marginRight - btnWidth;
        
        btnNewGame.setBounds(xPos, marginTop, btnWidth, btnHeight);
        btnMainMenu.setBounds(xPos, marginTop + btnHeight + spacing, btnWidth, btnHeight);
        btnExit.setBounds(xPos, marginTop + (btnHeight + spacing) * 2, btnWidth, btnHeight);
        
        // Style buttons
        styleButton(btnNewGame, new Color(46, 204, 113), Color.WHITE); // Green
        styleButton(btnMainMenu, new Color(52, 152, 219), Color.WHITE); // Blue
        styleButton(btnExit, new Color(231, 76, 60), Color.WHITE); // Red
        
        btnNewGame.addActionListener(e -> restartGame());
        btnMainMenu.addActionListener(e -> goToMainMenu());
        btnExit.addActionListener(e -> System.exit(0));
        
        add(btnNewGame);
        add(btnMainMenu);
        add(btnExit);

        resetShape();
        stopAllShortClips();
        if (gameMusicClip == null || !gameMusicClip.isRunning()) playGameMusicLoop();
        timer.start();
        gameRunning = true;
        inWelcomeScreen = false;
        inGameOver = false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Missed Eye Catcher");
            MissedEyeCatcher game = new MissedEyeCatcher();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
