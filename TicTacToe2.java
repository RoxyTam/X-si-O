package roxi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TicTacToe2 implements Runnable {

    private String ip = "127.0.0.1";
    private int port = 65432;
    private Scanner scanner = new Scanner(System.in);
    private JFrame frame;
    private final int WIDTH = 506;
    private final int HEIGHT = 527;
    private Thread thread;

    private Painter painter;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private ServerSocket serverSocket;

    private BufferedImage placa;
    private BufferedImage xRosu;
    private BufferedImage xAlbastru;
    private BufferedImage zeroRosu;
    private BufferedImage zeroAlbastru;

    private String[] spaces = new String[9];

    private boolean yourTurn = false;
    private boolean cerc = true;
    private boolean accepted = false;
    private boolean conexiuneIndisponibila = false;
    private boolean castigator = false;
    private boolean enemycastigator = false;
    private boolean egalitate = false;

    private int lengthOfSpace = 160;
    private int errors = 0;
    private int firstSpot = -1;
    private int secondSpot = -1;

    private Font font = new Font("Verdana", Font.BOLD, 32);
    private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
    private Font largerFont = new Font("Verdana", Font.BOLD, 50);

    private String waitingString = "In asteptarea oponentului";
    private String conexiuneIndisponibilaString = "Nu se poate comunica cu oponentul.";
    private String castigatorString = "Esti castigator!";
    private String enemycastigatorString = "Oponent castigator!";
    private String egalitateString = "Egalitate.";

    private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

    /**
     * <pre>
     * 0, 1, 2
     * 3, 4, 5
     * 6, 7, 8
     * </pre>
     */

    public TicTacToe2() {
        System.out.println("Intodu IP Server: ");
        ip = scanner.nextLine();
        System.out.println("Introdu portul: ");
        port = scanner.nextInt();
        while (port < 1 || port > 65535) {
            System.out.println("Portul este invalid, incercati din nou: ");
            port = scanner.nextInt();
        }

        loadImages();

        painter = new Painter();
        painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        if (!connect()) initializeServer();

        frame = new JFrame();
        frame.setTitle("X si O");
        frame.setContentPane(painter);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        thread = new Thread(this, "X si 0");
        thread.start();
    }

    public void run() {
        while (true) {
            tick();
            painter.repaint();

            if (!cerc && !accepted) {
                listenForServerRequest();
            }

        }
    }

    private void render(Graphics g) {
        g.drawImage(placa, 0, 0, null);
        if (conexiuneIndisponibila) {
            g.setColor(Color.RED);
            g.setFont(smallerFont);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(conexiuneIndisponibilaString);
            g.drawString(conexiuneIndisponibilaString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            return;
        }

        if (accepted) {
            for (int i = 0; i < spaces.length; i++) {
                if (spaces[i] != null) {
                    if (spaces[i].equals("X")) {
                        if (cerc) {
                            g.drawImage(xRosu, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(xAlbastru, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    } else if (spaces[i].equals("O")) {
                        if (cerc) {
                            g.drawImage(zeroAlbastru, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        } else {
                            g.drawImage(zeroRosu, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
                        }
                    }
                }
            }
            if (castigator || enemycastigator) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(10));
                g.setColor(Color.BLACK);
                g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);

                g.setColor(Color.RED);
                g.setFont(largerFont);
                if (castigator) {
                    int stringWidth = g2.getFontMetrics().stringWidth(castigatorString);
                    g.drawString(castigatorString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                } else if (enemycastigator) {
                    int stringWidth = g2.getFontMetrics().stringWidth(enemycastigatorString);
                    g.drawString(enemycastigatorString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
                }
            }
            if (egalitate) {
                Graphics2D g2 = (Graphics2D) g;
                g.setColor(Color.BLACK);
                g.setFont(largerFont);
                int stringWidth = g2.getFontMetrics().stringWidth(egalitateString);
                g.drawString(egalitateString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
            }
        } else {
            g.setColor(Color.RED);
            g.setFont(font);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
            g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
        }

    }

    private void tick() {
        if (errors >= 10) conexiuneIndisponibila = true;

        if (!yourTurn && !conexiuneIndisponibila) {
            try {
                int space = dis.readInt();
                if (cerc) spaces[space] = "X";
                else spaces[space] = "O";
                checkForEnemyWin();
                checkForegalitate();
                yourTurn = true;
            } catch (IOException e) {
                e.printStackTrace();
                errors++;
            }
        }
    }

    private void checkForWin() {
        for (int i = 0; i < wins.length; i++) {
            if (cerc) {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    castigator = true;
                }
            } else {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    castigator = true;
                }
            }
        }
    }

    private void checkForEnemyWin() {
        for (int i = 0; i < wins.length; i++) {
            if (cerc) {
                if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemycastigator = true;
                }
            } else {
                if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
                    firstSpot = wins[i][0];
                    secondSpot = wins[i][2];
                    enemycastigator = true;
                }
            }
        }
    }

    private void checkForegalitate() {
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i] == null) {
                return;
            }
        }
        egalitate = true;
    }

    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
            System.out.println("Request de la client acceptat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean connect() {
        try {
            socket = new Socket(ip, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            accepted = true;
        } catch (IOException e) {
            System.out.println("Nu se poate conecta la: " + ip + ":" + port + " | Pornire server");
            return false;
        }
        System.out.println("Conectare cu succes.");
        return true;
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
        } catch (Exception e) {
            e.printStackTrace();
        }
        yourTurn = true;
        cerc = false;
    }

    private void loadImages() {
        try {
            placa = ImageIO.read(getClass().getResourceAsStream("/roxi/placa.png"));
            xRosu = ImageIO.read(getClass().getResourceAsStream("/roxi/xRosu.png"));
            zeroRosu = ImageIO.read(getClass().getResourceAsStream("/roxi/zeroRosu.png"));
            xAlbastru = ImageIO.read(getClass().getResourceAsStream("/roxi/xAlbastru.png"));
            zeroAlbastru = ImageIO.read(getClass().getResourceAsStream("/roxi/zeroAlbastru.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        TicTacToe2 ticTacToe = new TicTacToe2();
    }

    private class Painter extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        public Painter() {
            setFocusable(true);
            requestFocus();
            setBackground(Color.WHITE);
            addMouseListener(this);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            render(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (accepted) {
                if (yourTurn && !conexiuneIndisponibila && !castigator && !enemycastigator) {
                    int x = e.getX() / lengthOfSpace;
                    int y = e.getY() / lengthOfSpace;
                    y *= 3;
                    int position = x + y;

                    if (spaces[position] == null) {
                        if (!cerc) spaces[position] = "X";
                        else spaces[position] = "O";
                        yourTurn = false;
                        repaint();
                        Toolkit.getDefaultToolkit().sync();

                        try {
                            dos.writeInt(position);
                            dos.flush();
                        } catch (IOException e1) {
                            errors++;
                            e1.printStackTrace();
                        }

                        System.out.println("Pachet de date trimis");
                        checkForWin();
                        checkForegalitate();

                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

    }

}
