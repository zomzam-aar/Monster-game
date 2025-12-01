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

/**
 * Missed Eye Catcher - renamed from Catch The Shape
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
    double scaleSpeed = 0.05;
    boolean scaleUp = true;

    int playerX = 160;
    int playerY = 360;
    int playerWidth = 160;
    int playerHeight = 75;
    int playerSpeed = 10;
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

    String playerName = "Player";

    boolean inWelcomeScreen = true;
    boolean gameRunning = false;
    boolean inGameOver = false;

    JTextField nameField;
    JComboBox<String> shapeSelector;
    JButton startButton;
    JButton btnNewGame;
    JButton btnExit;
    JButton btnMainMenu;

    int numStars = 50;
    int[][] stars = new int[numStars][2];
    Random starRandom = new Random();
    int spawnCount = 0;
    boolean shapeIsHeart = false;

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

        createWelcomeScreen();
        playStartMusicLoop();
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

        startButton = new JButton("Start Game");
        startButton.setBounds(130, 250, 130, 30);
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

        btnNewGame = new JButton("New Game");
        btnExit = new JButton("Exit");
        btnMainMenu = new JButton("Main Menu");
        int spacing = 8;
        int marginRight = 10;
        int wExit = 80, wNew = 120, wMain = 110;
        int xExit = WIDTH - marginRight - wExit;
        int xNew = xExit - spacing - wNew;
        int xMain = xNew - spacing - wMain;
        btnNewGame.setBounds(xNew, 10, wNew, 28);
        btnMainMenu.setBounds(xMain, 10, wMain, 28);
        btnExit.setBounds(xExit, 10, wExit, 28);
        btnNewGame.addActionListener(e -> restartGame());
        btnExit.addActionListener(e -> System.exit(0));
        btnMainMenu.addActionListener(e -> goToMainMenu());
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

    void createMenu(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem exit = new JMenuItem("Exit");

        newGame.addActionListener(e -> restartGame());
        exit.addActionListener(e -> System.exit(0));
        fileMenu.add(newGame);
        fileMenu.add(exit);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e ->
                JOptionPane.showMessageDialog(frame,
                        "Faculty of Graphics  MultiMedia\n Graphics & Animation Dept\nv1.0\nBy: Rafeek Yanni.",
                        "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(about);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);
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
        scaleUp = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;
        shapeY += shapeSpeed;
        rotation += 0;

        int dynamicSizeNow = (int)(shapeSize * scale);
        if (shapeY + dynamicSizeNow >= playerY &&
                shapeX + dynamicSizeNow >= playerX &&
                shapeX <= playerX + playerWidth) {
            if (shapeIsHeart) {
                lives++;
                catchText = "+1 Life";
                catchTextX = shapeX + dynamicSizeNow/2;
                catchTextY = playerY - 10;
                catchTextEndTime = System.currentTimeMillis() + 1000;
                playSound("./assets/eating1.wav");
                resetShape();
                bgColor = Color.GREEN.darker();
                effectEndTime = System.currentTimeMillis() + 200;
            } else {
                int gained = 1;
                if (dynamicSizeNow >= 50) gained = 2;
                score += gained;
                if (gained > 1) {
                    catchText = "2x";
                    catchTextX = shapeX + dynamicSizeNow/2;
                    catchTextY = playerY - 10;
                    catchTextEndTime = System.currentTimeMillis() + 800;
                }

                String[] eats = {"./assets/eating1.wav", "./assets/eating2.wav"};
                playSound(eats[random.nextInt(eats.length)]);
                resetShape();
                bgColor = Color.darkGray;
                effectEndTime = System.currentTimeMillis() + 200;

                if (score % 5 == 0) { level++; shapeSpeed++; }
                if (score > highScore) highScore = score;
            }
        }

        if (shapeY > HEIGHT) {
            File lf = new File("./assets/lose-heart.wav");
            if (lf.exists()) playSound("./assets/lose-heart.wav"); else playSound("./assets/miss.wav");
            lives--;
            if (lives <= 0) {
                gameOver();
                return;
            }
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

    void playSound(String fileName) {
        try {
            File f = new File(fileName);
            if (!f.exists()) return;
            AudioInputStream audio = AudioSystem.getAudioInputStream(f);
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
    }

    void playStartMusicLoop() {
        try {
            File f = new File("./assets/start.wav");
            if (!f.exists()) return;
            AudioInputStream audio = AudioSystem.getAudioInputStream(f);
            startMusicClip = AudioSystem.getClip();
            startMusicClip.open(audio);
            startMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            startMusicClip = null;
        }
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
        timer.stop();
        stopGameMusic();
        stopAllShortClips();
        playSound("./assets/gameover.wav");
        gameRunning = false;
        inGameOver = true;
        if (gameVoiceTimer != null) { gameVoiceTimer.stop(); gameVoiceTimer = null; }
        removeAll();
        setLayout(null);
        createGameOverScreen();
        revalidate();
        repaint();
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

        removeAll();
        setLayout(null);
        createWelcomeScreen();
        revalidate();
        repaint();

        playStartMusicLoop();
    }

    void playGameMusicLoop() {
        try {
            try { if (gameMusicClip != null && gameMusicClip.isRunning()) { gameMusicClip.stop(); gameMusicClip.close(); } } catch (Exception ignored) {}
            File f = new File("./assets/game.wav");
            if (!f.exists()) return;
            AudioInputStream audio = AudioSystem.getAudioInputStream(f);
            gameMusicClip = AudioSystem.getClip();
            gameMusicClip.open(audio);
            gameMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ignored) { gameMusicClip = null; }
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
        removeAll();
        setLayout(null);

        btnNewGame = new JButton("New Game");
        btnExit = new JButton("Exit");
        btnMainMenu = new JButton("Main Menu");
        int spacing = 8;
        int marginRight = 10;
        int wExit = 80, wNew = 120, wMain = 110;
        int xExit = WIDTH - marginRight - wExit;
        int xNew = xExit - spacing - wNew;
        int xMain = xNew - spacing - wMain;
        btnNewGame.setBounds(xNew, 10, wNew, 28);
        btnMainMenu.setBounds(xMain, 10, wMain, 28);
        btnExit.setBounds(xExit, 10, wExit, 28);
        btnNewGame.addActionListener(e -> restartGame());
        btnExit.addActionListener(e -> System.exit(0));
        btnMainMenu.addActionListener(e -> goToMainMenu());
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
