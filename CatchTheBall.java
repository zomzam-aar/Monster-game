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
 * 🎮 Catch The Shape - النسخة المتقدمة التعليمية
 *
 * أهداف النسخة:
 * - تعلم Swing + AWT للرسم والأنيميشن
 * - تجربة Rotation وScale (حجم ديناميكي)
 * - مؤثرات صوتية وتفاعل مع اللاعب
 * - خلفية متحركة + تدرج لوني + نجوم
 */
public class CatchTheBall extends JPanel implements ActionListener {

    // =========================
    // 🖥️ إعدادات النافذة واللعبة
    // =========================
    static final int WIDTH = 720;
    static final int HEIGHT = 424;

    Timer timer;                // مؤقت لتحريك اللعبة (Animation Loop)
    Random random = new Random();

    // -------------------------
    // خصائص الشكل (دائرة، مربع، نجمة)
    // -------------------------
    String shapeType = "Circle"; // نوع الشكل
    int shapeX, shapeY;          // موقع الشكل
    int shapeSize = 20;          // حجم الشكل الأساسي
    int shapeSpeed = 3;          // سرعة سقوط الشكل
    double rotation = 0;         // زاوية الدوران

    // -------------------------
    // خصائص الحجم الديناميكي (Scale Animation)
    // -------------------------
    double scale = 1.0;          // مقياس الشكل الحالي
    double scaleSpeed = 0.05;    // سرعة تغيير الحجم
    boolean scaleUp = true;      // اتجاه التغيير (تكبير أو تصغير)

    // -------------------------
    // خصائص اللاعب
    // -------------------------
    int playerX = 160;
    int playerY = 360;
    int playerWidth = 160;
    int playerHeight = 75;
    int playerSpeed = 10;
    // Mouse control smoothing (0..1) where lower is slower/delayed follow
    double mouseTargetX = playerX;
    double mouseSmoothing = 0.15; // tweak this for more/less delay

    // -------------------------
    // نظام اللعبة (نقاط، مستوى، حياة)
    // -------------------------
    int score = 0;
    int level = 1;
    int lives = 3;
    int highScore = 0;

    // -------------------------
    // خلفية وتأثيرات
    // -------------------------
    Color bgColor = Color.black;
    long effectEndTime = 0;
    BufferedImage backgroundImage = null;
    BufferedImage shapeImage = null;
    BufferedImage startBackground = null;
    BufferedImage heartImage = null;
    BufferedImage gameOverImage = null;
    // Catch text overlay
    String catchText = null;
    int catchTextX = 0, catchTextY = 0;
    long catchTextEndTime = 0;
    Image monsterImage = null;
    // start screen music clip (loop)
    Clip startMusicClip = null;
    Clip gameMusicClip = null;
    Timer welcomeVoiceTimer = null;
    Timer gameVoiceTimer = null;
    String welcomeVoiceFile = "roar-echo.wav";
    // track active short clips (eating/miss/etc.) so we can stop them when switching screens
    final List<Clip> activeClips = new ArrayList<>();

    // -------------------------
    // اسم اللاعب
    // -------------------------
    String playerName = "Player";

    // -------------------------
    // حالات اللعبة
    // -------------------------
    boolean inWelcomeScreen = true; // شاشة البداية
    boolean gameRunning = false;    // اللعبة قيد التشغيل
    boolean inGameOver = false;     // عرض شاشة نهاية اللعبة

    // -------------------------
    // عناصر شاشة الترحيب
    // -------------------------
    JTextField nameField;
    JComboBox<String> shapeSelector;
    JButton startButton;
    // In-game buttons
    JButton btnNewGame;
    JButton btnExit;
    JButton btnMainMenu;

    // -------------------------
    // الخلفية المتحركة: نجوم
    // -------------------------
    int numStars = 50;
    int[][] stars = new int[numStars][2]; // stars[i][0]=x, stars[i][1]=y
    Random starRandom = new Random();
    // spawn counter for special sizes
    int spawnCount = 0;
    boolean shapeIsHeart = false;

    // =========================
    // 🔧 المُنشئ (Constructor)
    // =========================
    public CatchTheBall() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        // Mouse movement control: follow mouse X with smoothing
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
        timer = new Timer(20, this); // تحديث كل 20 مللي ثانية

        // حاول تحميل صور الخلفية والشكل
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

        createWelcomeScreen();       // إنشاء شاشة الترحيب
        // start welcome music
        playStartMusicLoop();
    }

    // =========================
    // 🎬 شاشة الترحيب
    // =========================
    void createWelcomeScreen() {
        setLayout(null); // Layout يدوي لتحديد موقع العناصر

        // title removed from start screen

        // إدخال اسم اللاعب
        JLabel nameLabel = new JLabel("Enter your name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBounds(120, 100, 150, 25);
        add(nameLabel);

        nameField = new JTextField("Player");
        nameField.setBounds(120, 130, 150, 25);
        add(nameField);

        // removed shape selector (game always uses selected shape setting)

        // زر بدء اللعبة
        startButton = new JButton("Start Game");
        startButton.setBounds(130, 250, 130, 30);
        startButton.addActionListener(e -> startGame());
        add(startButton);

        setBackground(Color.BLACK);

        // start music now triggered from constructor or when returning to main menu
        // play a welcome voice immediately and then every 20 seconds
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

    // =========================
    // ▶️ بدء اللعبة بعد اختيار الاسم والشكل
    // =========================
    void startGame() {
        playerName = nameField.getText();               // حفظ اسم اللاعب
        // shape selector removed from welcome screen; keep existing shapeType
        inWelcomeScreen = false;
        // stop welcome voice timer when entering the game
        if (welcomeVoiceTimer != null) { welcomeVoiceTimer.stop(); welcomeVoiceTimer = null; }
        gameRunning = true;
        removeAll();                                   // إزالة عناصر البداية
        setLayout(null);

        // Create in-game buttons at top-right with consistent spacing
        btnNewGame = new JButton("New Game");
        btnExit = new JButton("Exit");
        btnMainMenu = new JButton("Main Menu");
        int spacing = 8; // px between buttons
        int marginRight = 10; // px from right edge
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

        // تهيئة أول شكل
        resetShape();

        // تهيئة النجوم في الخلفية المتحركة
        for (int i = 0; i < numStars; i++) {
            stars[i][0] = starRandom.nextInt(WIDTH);
            stars[i][1] = starRandom.nextInt(HEIGHT);
        }

        timer.start();                                 // بدء المؤقت
        requestFocusInWindow();                        // تفعيل لوحة المفاتيح
        // stop start screen music and any active short clips
        stopStartMusic();
        stopAllShortClips();
        // start gameplay music loop
        playGameMusicLoop();
        // start in-game repeating voice if file exists: play immediately then every 10s
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

    // =========================
    // 📋 إنشاء Menu اللعبة
    // =========================
    void createMenu(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem exit = new JMenuItem("Exit");

        newGame.addActionListener(e -> restartGame()); // إعادة تشغيل اللعبة
        exit.addActionListener(e -> System.exit(0));  // خروج من اللعبة
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

    // =========================
    // 🔄 إعادة تهيئة موقع الشكل
    // =========================
    void resetShape() {
        // increment spawn counter and set special size every 10th spawn
        spawnCount++;
        // every 15th spawn create a falling life-heart instead of a normal shape
        if (spawnCount % 15 == 0) {
            shapeIsHeart = true;
            shapeSize = 30; // heart size
        } else {
            shapeIsHeart = false;
            if (spawnCount % 10 == 0) {
                shapeSize = 50; // special larger size
            } else {
                shapeSize = 35; // standard size
            }
        }
        shapeX = random.nextInt(Math.max(1, WIDTH - shapeSize));
        shapeY = 0;
        // make size fixed for this spawned shape
        scale = 1.0;
        scaleUp = false;
    }

    // =========================
    // ⏱️ تحديث اللعبة (Animation Loop)
    // =========================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameRunning) return;

        // -------------------------
        // تحريك الشكل
        // -------------------------
        shapeY += shapeSpeed;
        rotation += 0; // دوران الشكل

        // size is fixed per spawn (no dynamic scaling while falling)

        // -------------------------
        // التحقق من التقاط الشكل
        // -------------------------
        int dynamicSizeNow = (int)(shapeSize * scale);
        if (shapeY + dynamicSizeNow >= playerY &&
                shapeX + dynamicSizeNow >= playerX &&
                shapeX <= playerX + playerWidth) {
            if (shapeIsHeart) {
                // caught a life heart: grant one extra life, no score
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
                // score: double if dynamic size > 50
                int gained = 1;
                if (dynamicSizeNow >= 50) gained = 2;
                score += gained;
                if (gained > 1) {
                    // show overlay at catch x position
                    catchText = "2x";
                    catchTextX = shapeX + dynamicSizeNow/2;
                    catchTextY = playerY - 10;
                    catchTextEndTime = System.currentTimeMillis() + 800;
                }

                // play one of two random eating sounds when caught
                String[] eats = {"./assets/eating1.wav", "./assets/eating2.wav"};
                playSound(eats[random.nextInt(eats.length)]);
                resetShape();
                bgColor = Color.darkGray;
                effectEndTime = System.currentTimeMillis() + 200;

                if (score % 5 == 0) { level++; shapeSpeed++; }
                if (score > highScore) highScore = score;
            }
        }

        // -------------------------
        // شكل سقط خارج النافذة
        // -------------------------
        if (shapeY > HEIGHT) {
            // Play a dedicated lose-life sound if available, otherwise fallback to miss.wav
            File lf = new File("./assets/lose-heart.wav");
            if (lf.exists()) playSound("./assets/lose-heart.wav"); else playSound("./assets/miss.wav");
            // decrement life first and if player has no lives left, immediately trigger game over
            lives--;
            if (lives <= 0) {
                gameOver();
                return; // ensure no further processing this tick
            }
            // otherwise reset for next shape and show hit effect
            resetShape();
            bgColor = Color.red;
            effectEndTime = System.currentTimeMillis() + 200;
        }

        // إنهاء التأثير اللوني المؤقت
        if (System.currentTimeMillis() > effectEndTime)
            bgColor = Color.black;

        // -------------------------
        // تحريك النجوم في الخلفية
        // -------------------------
        for (int i = 0; i < numStars; i++) {
            stars[i][1] += 1 + level * 0.1; // سرعة النجوم تزيد مع المستوى
            if (stars[i][1] > HEIGHT) stars[i][1] = 0;
        }

        // Smoothly move player towards mouse target X
        double dx = mouseTargetX - playerX;
        playerX += (int) Math.signum(dx) * Math.max(1, (int)(Math.abs(dx) * mouseSmoothing));
        // keep player within bounds
        if (playerX < 0) playerX = 0;
        if (playerX + playerWidth > WIDTH) playerX = WIDTH - playerWidth;

        repaint(); // إعادة الرسم
    }

    // =========================
    // 🖌️ رسم اللعبة
    // =========================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // إذا كنا في شاشة البداية
        if (inWelcomeScreen) {
            // draw start background if available
            if (startBackground != null) {
                g2d.drawImage(startBackground, 0, 0, WIDTH, HEIGHT, null);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, WIDTH, HEIGHT);
            }
            return;
        }

        // -------------------------
        // -------------------------
        // رسم الخلفية: صورة إذا وجدت وإلا تدرج لوني
        // -------------------------
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT, null);
            // ضع طبقة遮罩 بسيطة حسب bgColor
            g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 60));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 30), 0, HEIGHT, Color.BLACK);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }

        // -------------------------
        // رسم النجوم
        // -------------------------
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < numStars; i++)
            g2d.fillOval(stars[i][0], stars[i][1], 2, 2);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // -------------------------
        // رسم الشكل مع دوران وحجم ديناميكي
        // -------------------------
        int dynamicSize = (int)(shapeSize * scale);
        if (shapeIsHeart) {
            // draw falling life heart
            if (heartImage != null) {
                g2d.drawImage(heartImage, shapeX, shapeY, dynamicSize, dynamicSize, null);
            } else {
                // simple red heart approximation using filled polygon
                g2d.setColor(Color.RED);
                int cx = shapeX + dynamicSize/2;
                int cy = shapeY + dynamicSize/2;
                int[] xs = {cx, cx - dynamicSize/2, cx - dynamicSize/3, cx, cx + dynamicSize/3, cx + dynamicSize/2};
                int[] ys = {shapeY, shapeY + dynamicSize/3, shapeY + dynamicSize, cy + dynamicSize/3, shapeY + dynamicSize, shapeY + dynamicSize/3};
                g2d.fillPolygon(xs, ys, xs.length);
            }
        } else if (shapeImage != null) {
            AffineTransform old = g2d.getTransform();
            // draw rotated+scaled image centered at shape center
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

        // -------------------------
        // رسم اللاعب (paddle) - use monster GIF if available
        // -------------------------
        if (monsterImage != null) {
            g2d.drawImage(monsterImage, playerX, playerY - (playerHeight), playerWidth, playerHeight*2, null);
        } else {
            g2d.setColor(Color.CYAN);
            g2d.fillRoundRect(playerX, playerY, playerWidth, playerHeight, 5, 5);
        }

        // -------------------------
        // معلومات اللعبة
        // -------------------------
        g2d.setColor(Color.WHITE);
        g2d.drawString("Player: " + playerName, 10, 20);
        g2d.drawString("Score: " + score, 10, 40);
        g2d.drawString("Level: " + level, 10, 60);
        g2d.drawString("High: " + highScore, 300, 20);

        // draw lives as heart images if available, otherwise fallback to small pink ovals
        if (heartImage != null) {
            int hw = 18; // heart width
            int hh = 18; // heart height
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

        // draw catch overlay text if active
        if (catchText != null && System.currentTimeMillis() < catchTextEndTime) {
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(Color.ORANGE);
            int tw = g2d.getFontMetrics().stringWidth(catchText);
            g2d.drawString(catchText, catchTextX - tw/2, catchTextY);
        } else {
            catchText = null;
        }
    }

    // =========================
    // ⭐ رسم النجمة
    // =========================
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

    // =========================
    // 🖱️ تحكم بالفأرة (Smooth follow)
    // =========================
    // Mouse events handled by MouseMotionListener added in constructor

    // =========================
    // 🔊 مؤثرات صوتية
    // =========================
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

    // Play a looping WAV on the start screen. Use file './StartBackGround.wav' or './start.wav'.
    void playStartMusicLoop() {
        try {
            File f = new File("./assets/start.wav");
            if (!f.exists()) return;
            AudioInputStream audio = AudioSystem.getAudioInputStream(f);
            startMusicClip = AudioSystem.getClip();
            startMusicClip.open(audio);
            startMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            // can't play; ignore
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

    // stop and clear any active short clips
    void stopAllShortClips() {
        synchronized (activeClips) {
            for (Clip c : new ArrayList<>(activeClips)) {
                try { if (c.isRunning()) c.stop(); c.close(); } catch (Exception ignored) {}
            }
            activeClips.clear();
        }
    }

    // =========================
    // 💀 شاشة النهاية
    // =========================
    void gameOver() {
        // Stop game and show the Game Over screen
        timer.stop();
        stopGameMusic();
        stopAllShortClips();
        // play game over sound once (in assets folder)
        playSound("./assets/gameover.wav");
        gameRunning = false;
        inGameOver = true;
        if (gameVoiceTimer != null) { gameVoiceTimer.stop(); gameVoiceTimer = null; }
        // show game over UI
        removeAll();
        setLayout(null);
        createGameOverScreen();
        revalidate();
        repaint();
    }

    void createGameOverScreen() {
        int imageBottomY = 0;
        if (gameOverImage != null) {
            // center the image (250x250) in the middle of the screen (smaller so buttons fit below)
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
            imageBottomY = imgY + drawH;
        } else {
            // Large title (fallback)
            JLabel over = new JLabel("Game Over");
            over.setForeground(Color.WHITE);
            over.setFont(new Font("Arial", Font.BOLD, 36));
            over.setBounds(WIDTH/2 - 150, 80, 300, 50);
            over.setHorizontalAlignment(SwingConstants.CENTER);
            add(over);
            imageBottomY = 80 + 50;
        }


        // Stats (placed below the image)
        JLabel stats = new JLabel("Score: " + score + "    High: " + highScore);
        stats.setForeground(Color.WHITE);
        stats.setFont(new Font("Arial", Font.PLAIN, 18));
        stats.setBounds(WIDTH/2 - 150, imageBottomY + 10, 300, 30);
        stats.setHorizontalAlignment(SwingConstants.CENTER);
        add(stats);

        // Buttons: Restart, Main Menu, Exit (centered under stats)
        JButton restart = new JButton("Restart");
        JButton main = new JButton("Main Menu");
        JButton exit = new JButton("Exit");
        int bw = 120;
        int spacing = 8;
        int totalW = bw * 3 + spacing * 2;
        int startX = (WIDTH - totalW) / 2;
        int btnY = imageBottomY + 45; // below stats
        restart.setBounds(startX, btnY, bw, 28);
        main.setBounds(startX + bw + spacing, btnY, bw, 28);
        exit.setBounds(startX + (bw + spacing) * 2, btnY, bw, 28);

        restart.addActionListener(e -> {
            // remove game-over UI and restart
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
        // Stop gameplay and music, reset game state and show welcome UI
        timer.stop();
        stopGameMusic();
        stopAllShortClips();
        gameRunning = false;
        inWelcomeScreen = true;
        // ensure welcome voice restarts when main menu created (createWelcomeScreen will start it)
        if (welcomeVoiceTimer != null) { welcomeVoiceTimer.stop(); welcomeVoiceTimer = null; }
        if (gameVoiceTimer != null) { gameVoiceTimer.stop(); gameVoiceTimer = null; }

        // reset core game state
        score = 0;
        level = 1;
        lives = 3;
        shapeSpeed = 3;

        removeAll();
        setLayout(null);
        createWelcomeScreen();
        revalidate();
        repaint();

        // start start-screen music if present
        playStartMusicLoop();
    }

    void playGameMusicLoop() {
        try {
            // stop existing game music if any
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

    // =========================
    // 🔁 إعادة تشغيل اللعبة
    // =========================
    void restartGame() {
        // Reset core gameplay state and rebuild in-game UI (buttons)
        score = 0;
        level = 1;
        lives = 3;
        shapeSpeed = 3;
        removeAll();
        setLayout(null);

        // Create in-game buttons at top-right (reuse same layout as startGame)
        btnNewGame = new JButton("New Game");
        btnExit = new JButton("Exit");
        btnMainMenu = new JButton("Main Menu");
        int spacing = 8; // px between buttons
        int marginRight = 10; // px from right edge
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
        // restart gameplay music if not playing
        if (gameMusicClip == null || !gameMusicClip.isRunning()) playGameMusicLoop();
        timer.start();
        gameRunning = true;
        inWelcomeScreen = false;
        inGameOver = false;
    }

    // =========================
    // 🚀 البرنامج الرئيسي
    // =========================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🎮 Catch The Shape");
            CatchTheBall game = new CatchTheBall();
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
