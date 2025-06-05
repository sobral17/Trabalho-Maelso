import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;

public final class SpaceInvaders extends JPanel implements ActionListener, KeyListener {
    // tela
    int tileSize = 32;
    int rows = 16;
    int columns = 16;

    int boardWidth = tileSize * columns; // 32 * 16
    int boardHeight = tileSize * rows;   // 32 * 16

    Image shipImg;
    Image alienImg;
    Image alienCyanImg;
    Image alienMagentaImg;
    Image alienYellowImg;
    ArrayList<Image> alienImgArray;

    static class Block {
        int x;
        int y;
        int width;
        int height;
        Image img;
        boolean alive = true; // usado para aliens
        boolean used = false; // usado para balas

        Block(int x, int y, int width, int height, Image img) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.img = img;
        }
    }

    // nave
    int shipWidth = tileSize*2;
    int shipHeight = tileSize;
    int shipX = tileSize * columns/2 - tileSize;
    int shipY = tileSize * rows - tileSize*2;
    int shipVelocityX = tileSize; // velocidade de movimento da nave
    Block ship;

    // aliens
    ArrayList<Block> alienArray;
    int alienWidth = tileSize*2;
    int alienHeight = tileSize;
    int alienX = tileSize;
    int alienY = tileSize;

    int alienRows = 2;
    int alienColumns = 3;
    int alienCount = 0; // número de aliens a derrotar
    int alienVelocityX = 1; // velocidade de movimento dos aliens

    // balas
    ArrayList<Block> bulletArray;
    int bulletWidth = tileSize/8;
    int bulletHeight = tileSize/2;
    int bulletVelocityY = -10; // velocidade de movimento das balas

    Timer gameLoop;
    boolean gameOver = false;
    int score = 0;

    SpaceInvaders() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        // carregar imagens
        shipImg = new ImageIcon(Objects.requireNonNull(getClass().getResource("./ship.png"))).getImage();
        alienImg = new ImageIcon(Objects.requireNonNull(getClass().getResource("./alien.png"))).getImage();
        alienCyanImg = new ImageIcon(Objects.requireNonNull(getClass().getResource("./alien-cyan.png"))).getImage();
        alienMagentaImg = new ImageIcon(Objects.requireNonNull(getClass().getResource("./alien-magenta.png"))).getImage();
        alienYellowImg = new ImageIcon(Objects.requireNonNull(getClass().getResource("./alien-yellow.png"))).getImage();

        alienImgArray = new ArrayList<>();
        alienImgArray.add(alienImg);
        alienImgArray.add(alienCyanImg);
        alienImgArray.add(alienMagentaImg);
        alienImgArray.add(alienYellowImg);

        ship = new Block(shipX, shipY, shipWidth, shipHeight, shipImg);
        alienArray = new ArrayList<>();
        bulletArray = new ArrayList<>();

        // temporizador do jogo
        gameLoop = new Timer(1000/60, this); // 1000/60 = 16.6ms
        createAliens();
        gameLoop.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // desenhar nave
        g.drawImage(ship.img, ship.x, ship.y, ship.width, ship.height, null);

        // desenhar aliens
        for (Block alien : alienArray) {
            if (alien.alive) {
                g.drawImage(alien.img, alien.x, alien.y, alien.width, alien.height, null);
            }
        }

        // desenhar balas
        g.setColor(Color.white);
        for (Block bullet : bulletArray) {
            if (!bullet.used) {
                g.drawRect(bullet.x, bullet.y, bullet.width, bullet.height);
                // g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
            }
        }

        // desenhar pontuação
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Fim de jogo: " + score, 10, 35);
        } else {
            g.drawString(String.valueOf(score), 10, 35);
        }
    }

    public void move() {
        // mover aliens
        for (int i = 0; i < alienArray.size(); i++) {
            Block alien = alienArray.get(i);
            if (alien.alive) {
                alien.x += alienVelocityX;

                // se o aliens encostar nas bordas
                if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                    alienVelocityX *= -1;
                    alien.x += alienVelocityX*2;

                    // mover todos os aliens uma linha para baixo
                    for (Block block : alienArray) {
                        block.y += alienHeight;
                    }
                }

                if (alien.y >= ship.y) {
                    gameOver = true;
                }
            }
        }

        // mover balas
        for (Block bullet : bulletArray) {
            bullet.y += bulletVelocityY;

            // colisão das balas com os aliens
            for (Block alien : alienArray) {
                if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                    bullet.used = true;
                    alien.alive = false;
                    alienCount--;
                    score += 100;
                }
            }
        }

        // remover balas fora da tela ou já usadas
        while (!bulletArray.isEmpty() && (bulletArray.getFirst().used || bulletArray.getFirst().y < 0)) {
            bulletArray.removeFirst(); // remove o primeiro elemento da lista
        }

        // próxima fase
        if (alienCount == 0) {
            score += alienColumns * alienRows * 100; // pontos bônus :)
            alienColumns = Math.min(alienColumns + 1, columns/2 - 2); // limite em 6
            alienRows = Math.min(alienRows + 1, rows - 6);            // limite em 10
            alienArray.clear();
            bulletArray.clear();
            createAliens();
        }
    }

    public void createAliens() {
        Random random = new Random();
        for (int c = 0; c < alienColumns; c++) {
            for (int r = 0; r < alienRows; r++) {
                int randomImgIndex = random.nextInt(alienImgArray.size());
                Block alien = new Block(
                        alienX + c * alienWidth,
                        alienY + r * alienHeight,
                        alienWidth,
                        alienHeight,
                        alienImgArray.get(randomImgIndex)
                );
                alienArray.add(alien);
            }
        }
        alienCount = alienArray.size();
    }

    public boolean detectCollision(Block a, Block b) {
        return  a.x < b.x + b.width &&      // canto superior esquerdo de A não ultrapassa o canto superior direito de B
                a.x + a.width > b.x &&      // canto superior direito de A ultrapassa o canto superior esquerdo de B
                a.y < b.y + b.height &&     // canto superior esquerdo de A não ultrapassa o canto inferior esquerdo de B
                a.y + a.height > b.y;       // canto inferior esquerdo de A ultrapassa o canto superior esquerdo de B
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) { // qualquer tecla para reiniciar
            ship.x = shipX;
            bulletArray.clear();
            alienArray.clear();
            gameOver = false;
            score = 0;
            alienColumns = 3;
            alienRows = 2;
            alienVelocityX = 1;
            createAliens();
            gameLoop.start();
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT  && ship.x - shipVelocityX >= 0) {
            ship.x -= shipVelocityX; // mover para a esquerda
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT  && ship.x + shipVelocityX + ship.width <= boardWidth) {
            ship.x += shipVelocityX; // mover para a direita
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // atirar
            Block bullet = new Block(ship.x + shipWidth * 15 / 32, ship.y, bulletWidth, bulletHeight, null);
            bulletArray.add(bullet);
        }
    }
}

