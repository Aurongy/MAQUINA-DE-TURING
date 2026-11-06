import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * INTERFAZ GRAFICA DEL SIMULADOR DE MAQUINA DE TURING
 *
 * Muestra de forma visual todo el proceso:
 *  - La cinta con animación de desplazamiento y el cabezal con su estado actual.
 *  - El diagrama de estados, resaltando el arco (transición) que se acaba de usar.
 *  - La función de transición, resaltando la regla aplicada.
 *  - Un registro paso a paso (estado, símbolo leído/escrito, movimiento).
 *  - El resultado final: ACEPTACION o NO ACEPTACION.
 *
 * Reutiliza el lector/validador de definiciones de {@link TuringSimulator}.
 * Compilar con: javac -encoding UTF-8 -d out src/*.java
 */
public class TuringGUI extends JFrame {

    // ------------------------------------------------------------------
    // Paleta y fuentes (tema oscuro)
    // ------------------------------------------------------------------
    static final Color BG     = new Color(0x0F172A);
    static final Color PANEL  = new Color(0x1E293B);
    static final Color PANEL2 = new Color(0x162033);
    static final Color BORDE  = new Color(0x334155);
    static final Color TEXTO  = new Color(0xE2E8F0);
    static final Color MUTED  = new Color(0x94A3B8);
    static final Color ACCENT = new Color(0x38BDF8);
    static final Color OK     = new Color(0x22C55E);
    static final Color BAD    = new Color(0xEF4444);
    static final Color WARN   = new Color(0xF59E0B);

    static Font sans(int estilo, int tam) { return new Font("SansSerif", estilo, tam); }
    static Font mono(int estilo, int tam) { return new Font(Font.MONOSPACED, estilo, tam); }

    static void calidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Dibuja texto centrado en (cx, cy). */
    static void centrar(Graphics2D g, String s, double cx, double cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0),
                (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    static Color alfa(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    // ------------------------------------------------------------------
    // Motor de simulación paso a paso (cinta infinita con posiciones enteras)
    // ------------------------------------------------------------------

    /** Datos de un paso ejecutado, para el registro y las animaciones. */
    static class Registro {
        int paso, pos;
        String desde, lee, escribe, hasta, clave;
        char mov;
    }

    static class Sim {
        final TuringSimulator.Maquina m;
        final Map<Integer, String> cinta = new HashMap<>();
        int cabeza = 0, paso = 0;
        String estado;
        boolean terminada, acepta;
        String motivo = "";

        Sim(TuringSimulator.Maquina m, String entrada) {
            this.m = m;
            this.estado = m.inicial;
            for (int i = 0; i < entrada.length(); i++) cinta.put(i, String.valueOf(entrada.charAt(i)));
            revisar();
        }

        /** Comprueba si el estado actual es de aceptación o rechazo. */
        void revisar() {
            if (m.aceptacion.contains(estado)) {
                terminada = true; acepta = true;
                motivo = "Se alcanzó el estado de aceptación " + estado;
            } else if (m.rechazo.contains(estado)) {
                terminada = true; acepta = false;
                motivo = "Se alcanzó el estado de rechazo " + estado;
            }
        }

        String leer() { return cinta.getOrDefault(cabeza, TuringSimulator.BLANCO); }

        /** Ejecuta un paso. Devuelve null si la máquina se detuvo sin aplicar transición. */
        Registro avanzar() {
            if (terminada) return null;
            if (paso >= TuringSimulator.MAX_PASOS) {
                terminada = true; acepta = false;
                motivo = "Límite de pasos alcanzado (posible ciclo infinito)";
                return null;
            }
            String s = leer();
            String clave = TuringSimulator.Maquina.clave(estado, s);
            TuringSimulator.Transicion t = m.delta.get(clave);
            if (t == null) {
                terminada = true; acepta = false;
                motivo = "No existe transición para (" + estado + ", " + s + ")";
                return null;
            }
            Registro r = new Registro();
            r.paso = ++paso; r.desde = estado; r.lee = s; r.escribe = t.escribir;
            r.mov = t.mov; r.hasta = t.estado; r.pos = cabeza; r.clave = clave;

            cinta.put(cabeza, t.escribir);
            estado = t.estado;
            if (t.mov == 'D') cabeza++;
            else if (t.mov == 'I') cabeza--;
            revisar();
            return r;
        }
    }

    // ------------------------------------------------------------------
    // Estado de la aplicación
    // ------------------------------------------------------------------
    TuringSimulator.Maquina maquina;
    Sim sim;
    String cadenaActual = "";
    boolean ejecutando = false;
    javax.swing.Timer autoTimer;

    TapePanel tapePanel;
    DiagramPanel diagramPanel;
    JTextField campo;
    JLabel msgLabel, infoLabel;
    Boton btnPlay, btnStep, btnReset, btnRun;
    JSlider slider;
    DefaultListModel<String> listModel = new DefaultListModel<>();
    List<String> listKeys = new ArrayList<>();
    JList<String> lista;
    int listaActiva = -1;
    DefaultTableModel logModel;
    JTable logTable;

    // ------------------------------------------------------------------
    // Componentes personalizados
    // ------------------------------------------------------------------

    /** Botón redondeado con efecto hover. */
    static class Boton extends JButton {
        final Color base;
        Boton(String texto, Color base) {
            super(texto);
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(sans(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
        }
        @Override public void setEnabled(boolean b) {
            super.setEnabled(b);
            setForeground(b ? Color.WHITE : MUTED);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            Color c = !isEnabled() ? new Color(0x334155)
                    : getModel().isPressed() ? base.darker()
                    : getModel().isRollover() ? base.brighter() : base;
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    static JLabel etiqueta(String t, int tam, Color c, boolean negrita) {
        JLabel l = new JLabel(t);
        l.setFont(sans(negrita ? Font.BOLD : Font.PLAIN, tam));
        l.setForeground(c);
        return l;
    }

    // ------------------------------------------------------------------
    // Panel de la cinta (animado)
    // ------------------------------------------------------------------
    class TapePanel extends JPanel {
        double view = 0;           // posición (fraccionaria) de la ventana de la cinta
        int flashPos = Integer.MIN_VALUE;
        float flash = 0f;          // intensidad del destello de escritura
        boolean running = false;

        TapePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 195));
            new javax.swing.Timer(16, e -> {
                if (sim != null) {
                    double objetivo = sim.cabeza;
                    view += (objetivo - view) * 0.22;
                    if (Math.abs(objetivo - view) < 0.002) view = objetivo;
                }
                if (flash > 0) flash = Math.max(0f, flash - 0.025f);
                repaint();
            }).start();
        }

        void reiniciar() { view = 0; flash = 0; flashPos = Integer.MIN_VALUE; }
        void destello(int pos) { flashPos = pos; flash = 1f; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            int w = getWidth(), h = getHeight();
            g.setColor(PANEL);
            g.fillRoundRect(0, 0, w, h, 20, 20);

            g.setFont(sans(Font.BOLD, 12));
            g.setColor(MUTED);
            g.drawString("CINTA", 18, 24);

            // Insignia de estado de la simulación
            String txt; Color c;
            if (sim == null) { txt = "SIN CINTA"; c = MUTED; }
            else if (sim.terminada) { txt = sim.acepta ? "ACEPTACIÓN" : "NO ACEPTACIÓN"; c = sim.acepta ? OK : BAD; }
            else if (running) { txt = "EN EJECUCIÓN"; c = ACCENT; }
            else { txt = "EN PAUSA"; c = WARN; }
            g.setFont(sans(Font.BOLD, 13));
            int bw = g.getFontMetrics().stringWidth(txt) + 30;
            g.setColor(alfa(c, 40));
            g.fillRoundRect(w - bw - 16, 10, bw, 28, 28, 28);
            g.setColor(c);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(w - bw - 16, 10, bw, 28, 28, 28);
            centrar(g, txt, w - bw / 2.0 - 16, 24);

            if (sim == null) {
                g.setColor(MUTED);
                g.setFont(sans(Font.PLAIN, 15));
                centrar(g, maquina == null
                        ? "Cargue la definición de la máquina (archivo .txt) para comenzar"
                        : "Escriba una cadena arriba y presione ENTER para ejecutar",
                        w / 2.0, h / 2.0 + 10);
                g.dispose();
                return;
            }

            final int cell = 58;
            double cy = h / 2.0 + 20;
            double top = cy - 26;
            int primero = (int) Math.floor(view - (w / 2.0) / cell) - 1;
            int ultimo = (int) Math.ceil(view + (w / 2.0) / cell) + 1;

            for (int i = primero; i <= ultimo; i++) {
                double x = w / 2.0 + (i - view) * cell - cell / 2.0 + 3;
                RoundRectangle2D r = new RoundRectangle2D.Double(x, top, cell - 6, cell - 6, 12, 12);
                boolean enCabezal = (i == sim.cabeza);
                g.setColor(enCabezal ? new Color(0x0C4A6E) : PANEL2);
                g.fill(r);
                if (i == flashPos && flash > 0) {
                    g.setColor(alfa(WARN, (int) (flash * 170)));
                    g.fill(r);
                }
                g.setColor(enCabezal ? ACCENT : BORDE);
                g.setStroke(new BasicStroke(enCabezal ? 2.5f : 1.2f));
                g.draw(r);

                String sim_ = sim.cinta.getOrDefault(i, TuringSimulator.BLANCO);
                boolean blanco = sim_.equals(TuringSimulator.BLANCO);
                g.setFont(mono(Font.BOLD, 24));
                g.setColor(blanco ? new Color(0x475569) : TEXTO);
                centrar(g, sim_, x + (cell - 6) / 2.0, top + (cell - 6) / 2.0);

                g.setFont(sans(Font.PLAIN, 11));
                g.setColor(new Color(0x64748B));
                centrar(g, String.valueOf(i), x + (cell - 6) / 2.0, top + cell + 8);
            }

            // Desvanecido en los bordes (efecto de cinta infinita)
            g.setPaint(new GradientPaint(0, 0, PANEL, 110, 0, alfa(PANEL, 0)));
            g.fillRect(0, 44, 110, h - 44);
            g.setPaint(new GradientPaint(w - 110, 0, alfa(PANEL, 0), w, 0, PANEL));
            g.fillRect(w - 110, 44, 110, h - 44);

            // Cabezal: flecha + insignia con el estado actual
            double cx = w / 2.0;
            Color ce = sim.terminada ? (sim.acepta ? OK : BAD) : ACCENT;
            Path2D flecha = new Path2D.Double();
            flecha.moveTo(cx - 11, top - 22);
            flecha.lineTo(cx + 11, top - 22);
            flecha.lineTo(cx, top - 6);
            flecha.closePath();
            g.setColor(ce);
            g.fill(flecha);
            g.setFont(sans(Font.BOLD, 14));
            int sw = Math.max(74, g.getFontMetrics().stringWidth(sim.estado) + 28);
            g.fillRoundRect((int) (cx - sw / 2.0), (int) (top - 50), sw, 28, 10, 10);
            g.setColor(new Color(0x0B1220));
            centrar(g, sim.estado, cx, top - 36);

            // Información inferior
            g.setFont(sans(Font.PLAIN, 12));
            g.setColor(MUTED);
            g.drawString("Paso " + sim.paso + "     Posición del cabezal: " + sim.cabeza
                    + "     Símbolo leído: " + sim.leer(), 18, h - 12);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Panel del diagrama de estados
    // ------------------------------------------------------------------
    class DiagramPanel extends JPanel {
        static final int R = 25;
        final Map<String, List<String>> edges = new LinkedHashMap<>();
        String estadoActual, aFrom, aTo, aLabel;
        Boolean resultado;

        DiagramPanel() {
            setOpaque(false);
            new javax.swing.Timer(40, e -> repaint()).start();
        }

        void setMachine() {
            edges.clear();
            reiniciar(null);
            if (maquina == null) return;
            TreeMap<String, TuringSimulator.Transicion> ord = new TreeMap<>(maquina.delta);
            for (Map.Entry<String, TuringSimulator.Transicion> e : ord.entrySet()) {
                String[] k = e.getKey().split("\u0000", 2);
                TuringSimulator.Transicion t = e.getValue();
                edges.computeIfAbsent(k[0] + "\u0000" + t.estado, x -> new ArrayList<>())
                        .add(k[1] + "/" + t.escribir + "," + t.mov);
            }
        }

        void reiniciar(String estado) {
            estadoActual = estado; aFrom = aTo = aLabel = null; resultado = null;
        }

        void activar(String desde, String hasta, String label) {
            aFrom = desde; aTo = hasta; aLabel = label; estadoActual = hasta;
        }

        void flecha(Graphics2D g, double tx, double ty, double fx, double fy) {
            double a = Math.atan2(ty - fy, tx - fx);
            Path2D p = new Path2D.Double();
            p.moveTo(tx, ty);
            p.lineTo(tx - 11 * Math.cos(a - 0.4), ty - 11 * Math.sin(a - 0.4));
            p.lineTo(tx - 11 * Math.cos(a + 0.4), ty - 11 * Math.sin(a + 0.4));
            p.closePath();
            g.fill(p);
        }

        void etiquetas(Graphics2D g, List<String> labs, double x, double y, boolean activa) {
            g.setFont(mono(Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int lh = 16;
            double y0 = y - (labs.size() * lh) / 2.0 + lh / 2.0;
            for (int i = 0; i < labs.size(); i++) {
                String s = labs.get(i);
                int sw = fm.stringWidth(s) + 10;
                double yy = y0 + i * lh;
                boolean esta = activa && s.equals(aLabel);
                g.setColor(alfa(PANEL, 230));
                g.fillRoundRect((int) (x - sw / 2.0), (int) (yy - 8), sw, 16, 8, 8);
                g.setColor(esta ? WARN : new Color(0xCBD5E1));
                centrar(g, s, x, yy);
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            int w = getWidth(), h = getHeight();
            g.setColor(PANEL);
            g.fillRoundRect(0, 0, w, h, 20, 20);
            g.setFont(sans(Font.BOLD, 12));
            g.setColor(MUTED);
            g.drawString("DIAGRAMA DE ESTADOS", 18, 24);

            if (maquina == null) {
                g.setFont(sans(Font.PLAIN, 15));
                centrar(g, "El diagrama aparecerá al cargar una definición", w / 2.0, h / 2.0);
                g.dispose();
                return;
            }

            List<String> st = new ArrayList<>(maquina.estados);
            int n = st.size();
            double cx = w / 2.0, cy = h / 2.0 + 18;
            double rx = Math.max(30, w / 2.0 - 110), ry = Math.max(30, h / 2.0 - 95);
            Map<String, Point2D.Double> pos = new HashMap<>();
            for (int i = 0; i < n; i++) {
                if (n == 1) { pos.put(st.get(i), new Point2D.Double(cx, cy)); continue; }
                double a = -Math.PI / 2 + 2 * Math.PI * i / n;
                pos.put(st.get(i), new Point2D.Double(cx + rx * Math.cos(a), cy + ry * Math.sin(a)));
            }

            // Arcos
            for (Map.Entry<String, List<String>> e : edges.entrySet()) {
                String[] k = e.getKey().split("\u0000");
                String from = k[0], to = k[1];
                Point2D.Double a = pos.get(from), b = pos.get(to);
                if (a == null || b == null) continue;
                boolean activo = from.equals(aFrom) && to.equals(aTo);
                Color col = activo ? WARN : new Color(0x64748B);
                g.setColor(col);

                if (from.equals(to)) {
                    double ang = (n == 1) ? -Math.PI / 2 : Math.atan2(a.y - cy, a.x - cx);
                    Point2D.Double p1 = new Point2D.Double(a.x + R * Math.cos(ang - 0.5), a.y + R * Math.sin(ang - 0.5));
                    Point2D.Double p2 = new Point2D.Double(a.x + (R + 2) * Math.cos(ang + 0.5), a.y + (R + 2) * Math.sin(ang + 0.5));
                    Point2D.Double c1 = new Point2D.Double(a.x + 2.7 * R * Math.cos(ang - 0.45), a.y + 2.7 * R * Math.sin(ang - 0.45));
                    Point2D.Double c2 = new Point2D.Double(a.x + 2.7 * R * Math.cos(ang + 0.45), a.y + 2.7 * R * Math.sin(ang + 0.45));
                    g.setStroke(new BasicStroke(activo ? 3f : 1.7f));
                    g.draw(new CubicCurve2D.Double(p1.x, p1.y, c1.x, c1.y, c2.x, c2.y, p2.x, p2.y));
                    flecha(g, p2.x, p2.y, c2.x, c2.y);
                    etiquetas(g, e.getValue(), a.x + (R + 50) * Math.cos(ang), a.y + (R + 50) * Math.sin(ang), activo);
                } else {
                    double dx = b.x - a.x, dy = b.y - a.y;
                    double len = Math.hypot(dx, dy);
                    if (len < 1) continue;
                    boolean curvo = edges.containsKey(to + "\u0000" + from);
                    double off = curvo ? 38 : 0;
                    double qx = (a.x + b.x) / 2 + (-dy / len) * off * 2;
                    double qy = (a.y + b.y) / 2 + (dx / len) * off * 2;
                    double da = Math.hypot(qx - a.x, qy - a.y), db = Math.hypot(qx - b.x, qy - b.y);
                    double sx = a.x + (qx - a.x) / da * R, sy = a.y + (qy - a.y) / da * R;
                    double ex = b.x + (qx - b.x) / db * (R + 2), ey = b.y + (qy - b.y) / db * (R + 2);
                    g.setStroke(new BasicStroke(activo ? 3f : 1.7f));
                    g.draw(new QuadCurve2D.Double(sx, sy, qx, qy, ex, ey));
                    flecha(g, ex, ey, qx, qy);
                    etiquetas(g, e.getValue(), 0.25 * sx + 0.5 * qx + 0.25 * ex,
                            0.25 * sy + 0.5 * qy + 0.25 * ey, activo);
                }
            }

            // Nodos
            for (String s : st) {
                Point2D.Double p = pos.get(s);
                boolean actual = s.equals(estadoActual);
                boolean acepta = maquina.aceptacion.contains(s);
                boolean rechaza = maquina.rechazo.contains(s);
                Color base = resultado == null ? ACCENT : (resultado ? OK : BAD);

                if (actual) {
                    for (int i = 3; i >= 1; i--) {
                        g.setColor(alfa(base, 28));
                        g.fill(new Ellipse2D.Double(p.x - R - 6 * i, p.y - R - 6 * i, 2 * (R + 6 * i), 2 * (R + 6 * i)));
                    }
                }
                Ellipse2D circ = new Ellipse2D.Double(p.x - R, p.y - R, 2 * R, 2 * R);
                g.setColor(actual ? base : (rechaza ? alfa(BAD, 50) : PANEL2));
                g.fill(circ);
                g.setStroke(new BasicStroke(2.2f));
                g.setColor(actual ? base.brighter() : (rechaza ? BAD : (acepta ? OK : new Color(0x94A3B8))));
                g.draw(circ);
                if (acepta) {
                    g.setColor(actual ? new Color(0x0B1220) : OK);
                    g.setStroke(new BasicStroke(1.6f));
                    g.draw(new Ellipse2D.Double(p.x - R + 5, p.y - R + 5, 2 * R - 10, 2 * R - 10));
                }
                g.setFont(sans(Font.BOLD, 13));
                g.setColor(actual ? new Color(0x0B1220) : TEXTO);
                centrar(g, s, p.x, p.y);

                if (s.equals(maquina.inicial)) {
                    g.setColor(MUTED);
                    g.setStroke(new BasicStroke(1.8f));
                    g.draw(new Line2D.Double(p.x - R - 34, p.y, p.x - R - 3, p.y));
                    flecha(g, p.x - R - 2, p.y, p.x - R - 34, p.y);
                }
            }

            // Leyenda
            g.setFont(sans(Font.PLAIN, 11));
            g.setColor(MUTED);
            g.drawString("Arcos:  leído / escrito , movimiento", 18, h - 12);
            String[] nombres = {"actual", "aceptación", "rechazo"};
            Color[] cols = {ACCENT, OK, BAD};
            int x = w - 270;
            for (int i = 0; i < 3; i++) {
                g.setColor(cols[i]);
                g.fillOval(x, h - 21, 10, 10);
                g.setColor(MUTED);
                g.drawString(nombres[i], x + 15, h - 12);
                x += 15 + g.getFontMetrics().stringWidth(nombres[i]) + 18;
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Construcción de la ventana
    // ------------------------------------------------------------------
    public TuringGUI() {
        super("Simulador de Máquina de Turing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
        setLayout(new BorderLayout(12, 12));

        autoTimer = new javax.swing.Timer(500, e -> ejecutarPaso());
        autoTimer.setInitialDelay(450);

        tapePanel = new TapePanel();
        diagramPanel = new DiagramPanel();

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearPanelTransiciones(), BorderLayout.WEST);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(tapePanel, BorderLayout.NORTH);
        centro.add(diagramPanel, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        add(crearPanelInferior(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1000, 700));
        setSize(1320, 860);
        setLocationRelativeTo(null);
        actualizarBotones();

        File ej = new File("ejemplo_anbn.txt");
        if (ej.exists()) cargarDefinicion(ej, false);
    }

    JPanel crearEncabezado() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(PANEL);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(14, 18, 12, 18)));

        JPanel fila1 = new JPanel(new BorderLayout());
        fila1.setOpaque(false);
        fila1.add(etiqueta("Simulador de Máquina de Turing", 22, TEXTO, true), BorderLayout.WEST);
        infoLabel = etiqueta("Sin definición cargada", 12, MUTED, false);
        fila1.add(infoLabel, BorderLayout.EAST);
        p.add(fila1, BorderLayout.NORTH);

        JPanel fila2 = new JPanel(new BorderLayout(12, 0));
        fila2.setOpaque(false);
        Boton cargar = new Boton("Cargar definición…", new Color(0x475569));
        cargar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            fc.setDialogTitle("Seleccione el archivo con la definición de la MT");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                cargarDefinicion(fc.getSelectedFile(), true);
        });
        fila2.add(cargar, BorderLayout.WEST);

        campo = new JTextField();
        campo.setFont(mono(Font.BOLD, 18));
        campo.setBackground(new Color(0x0B1220));
        campo.setForeground(TEXTO);
        campo.setCaretColor(ACCENT);
        bordeCampo(ACCENT);
        campo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validarEnVivo(); }
            public void removeUpdate(DocumentEvent e) { validarEnVivo(); }
            public void changedUpdate(DocumentEvent e) { validarEnVivo(); }
        });
        campo.addActionListener(e -> ejecutarCadena());   // ENTER termina el ingreso
        fila2.add(campo, BorderLayout.CENTER);

        btnRun = new Boton("Cargar cinta y ejecutar", new Color(0x0284C7));
        btnRun.addActionListener(e -> ejecutarCadena());
        fila2.add(btnRun, BorderLayout.EAST);
        p.add(fila2, BorderLayout.CENTER);

        msgLabel = etiqueta("Cargue una definición y escriba la cadena de entrada (ENTER para ejecutar).", 12, MUTED, false);
        p.add(msgLabel, BorderLayout.SOUTH);
        return p;
    }

    void bordeCampo(Color c) {
        campo.setBorder(new CompoundBorder(new LineBorder(c, 2, true), new EmptyBorder(7, 12, 7, 12)));
    }

    JPanel crearPanelTransiciones() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(PANEL);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(12, 8, 8, 8)));
        p.setPreferredSize(new Dimension(280, 100));
        p.add(etiqueta("  FUNCIÓN DE TRANSICIÓN δ", 12, MUTED, true), BorderLayout.NORTH);

        lista = new JList<>(listModel);
        lista.setBackground(PANEL);
        lista.setSelectionModel(new DefaultListSelectionModel() {
            @Override public void setSelectionInterval(int a, int b) { /* solo lectura */ }
        });
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, false, false);
                setBorder(new EmptyBorder(5, 8, 5, 8));
                setFont(mono(Font.PLAIN, 12));
                if (i == listaActiva) { setBackground(new Color(0x78350F)); setForeground(new Color(0xFCD34D)); }
                else { setBackground(PANEL); setForeground(TEXTO); }
                return this;
            }
        });
        JScrollPane sp = new JScrollPane(lista);
        sp.setBorder(null);
        sp.getViewport().setBackground(PANEL);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    JPanel crearPanelInferior() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        // Controles
        JPanel ctr = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ctr.setOpaque(false);
        btnPlay = new Boton("Ejecutar", new Color(0x16A34A));
        btnStep = new Boton("Paso a paso", new Color(0x0284C7));
        btnReset = new Boton("Reiniciar", new Color(0x475569));
        btnPlay.addActionListener(e -> { if (ejecutando) detener(); else play(); actualizarBotones(); });
        btnStep.addActionListener(e -> { detener(); ejecutarPaso(); });
        btnReset.addActionListener(e -> { if (maquina != null && sim != null) cargarCadena(cadenaActual, false); });
        ctr.add(btnPlay); ctr.add(btnStep); ctr.add(btnReset);
        ctr.add(Box.createHorizontalStrut(20));
        ctr.add(etiqueta("Velocidad", 12, MUTED, false));
        slider = new JSlider(1, 10, 6);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(180, 28));
        slider.addChangeListener(e -> autoTimer.setDelay(1100 - slider.getValue() * 100));
        autoTimer.setDelay(1100 - slider.getValue() * 100);
        ctr.add(slider);
        p.add(ctr, BorderLayout.NORTH);

        // Registro de pasos
        logModel = new DefaultTableModel(
                new String[]{"Paso", "Estado", "Lee", "Escribe", "Mueve", "Nuevo estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        logTable = new JTable(logModel);
        logTable.setRowHeight(25);
        logTable.setShowGrid(false);
        logTable.setIntercellSpacing(new Dimension(0, 0));
        logTable.setFillsViewportHeight(true);
        logTable.setBackground(PANEL);
        logTable.setFont(mono(Font.PLAIN, 13));
        logTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer celdas = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, false, false, row, col);
                setHorizontalAlignment(CENTER);
                setBorder(null);
                boolean ultima = row == t.getRowCount() - 1;
                setBackground(ultima ? new Color(0x1E3A5F) : (row % 2 == 0 ? PANEL : PANEL2));
                setForeground(TEXTO);
                setFont(mono(Font.PLAIN, 13));
                if ("FIN".equals(String.valueOf(t.getValueAt(row, 0)))) {
                    boolean ok = String.valueOf(t.getValueAt(row, 5)).startsWith("ACEPT");
                    setForeground(ok ? OK : BAD);
                    setFont(mono(Font.BOLD, 13));
                    setBackground(ok ? alfa(OK, 40) : alfa(BAD, 40));
                }
                return this;
            }
        };
        logTable.setDefaultRenderer(Object.class, celdas);

        DefaultTableCellRenderer cab = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, false, false, row, col);
                setHorizontalAlignment(CENTER);
                setOpaque(true);
                setBackground(new Color(0x0B1220));
                setForeground(MUTED);
                setFont(sans(Font.BOLD, 12));
                setBorder(new EmptyBorder(6, 4, 6, 4));
                return this;
            }
        };
        logTable.getTableHeader().setDefaultRenderer(cab);

        JScrollPane sp = new JScrollPane(logTable);
        sp.setBorder(new LineBorder(BORDE, 1, true));
        sp.getViewport().setBackground(PANEL);
        sp.setPreferredSize(new Dimension(100, 190));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------
    // Lógica de la aplicación
    // ------------------------------------------------------------------

    void cargarDefinicion(File f, boolean mostrarError) {
        try {
            TuringSimulator.Maquina m = TuringSimulator.cargar(f.getPath());
            detener();
            maquina = m;
            sim = null;
            cadenaActual = "";
            logModel.setRowCount(0);
            listModel.clear();
            listKeys.clear();
            listaActiva = -1;
            TreeMap<String, TuringSimulator.Transicion> ord = new TreeMap<>(m.delta);
            for (Map.Entry<String, TuringSimulator.Transicion> e : ord.entrySet()) {
                String[] k = e.getKey().split("\u0000", 2);
                TuringSimulator.Transicion t = e.getValue();
                listKeys.add(e.getKey());
                listModel.addElement("δ(" + k[0] + ", " + k[1] + ") = (" + t.estado + ", " + t.escribir + ", " + t.mov + ")");
            }
            diagramPanel.setMachine();
            tapePanel.reiniciar();
            infoLabel.setText(f.getName() + "   ·   " + m.estados.size() + " estados   ·   inicial: " + m.inicial
                    + "   ·   Σ = " + m.alfabeto);
            setTitle("Simulador de Máquina de Turing — " + f.getName());
            msgLabel.setForeground(OK);
            msgLabel.setText("Definición cargada correctamente. Escriba la cadena y presione ENTER.");
            campo.requestFocusInWindow();
            actualizarBotones();
        } catch (Exception ex) {
            if (mostrarError) {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Error al cargar la definición", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Devuelve null si la cadena es válida, o el mensaje de error. */
    String validar(String s) {
        if (maquina == null) return "Primero cargue la definición de la máquina.";
        for (char c : s.toCharArray()) {
            String x = String.valueOf(c);
            if (!maquina.alfabeto.contains(x) || !maquina.simbolosCinta.contains(x))
                return "El carácter '" + x + "' no está definido en el alfabeto de la máquina.";
        }
        return null;
    }

    void validarEnVivo() {
        String err = validar(campo.getText());
        if (err != null) {
            msgLabel.setForeground(BAD);
            msgLabel.setText(err);
            bordeCampo(BAD);
        } else {
            msgLabel.setForeground(MUTED);
            msgLabel.setText("Cadena válida. Presione ENTER para cargarla en la cinta y ejecutar.");
            bordeCampo(ACCENT);
        }
    }

    void ejecutarCadena() {
        String err = validar(campo.getText());
        if (err != null) {
            msgLabel.setForeground(BAD);
            msgLabel.setText(err);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        cargarCadena(campo.getText(), true);
    }

    void cargarCadena(String s, boolean auto) {
        detener();
        cadenaActual = s;
        sim = new Sim(maquina, s);
        logModel.setRowCount(0);
        logModel.addRow(new Object[]{0, sim.estado, "-", "-", "-", "Configuración inicial"});
        tapePanel.reiniciar();
        diagramPanel.reiniciar(sim.estado);
        listaActiva = -1;
        lista.repaint();
        msgLabel.setForeground(MUTED);
        msgLabel.setText("Cinta cargada: " + (s.isEmpty() ? "(cadena vacía)" : s));
        if (sim.terminada) finalizar();
        else if (auto) play();
        actualizarBotones();
    }

    static String flecha(char m) {
        return m == 'D' ? "D →" : (m == 'I' ? "I ←" : "N ·");
    }

    void ejecutarPaso() {
        if (sim == null || sim.terminada) { detener(); actualizarBotones(); return; }
        Registro r = sim.avanzar();
        if (r != null) {
            logModel.addRow(new Object[]{r.paso, r.desde, r.lee, r.escribe, flecha(r.mov), r.hasta});
            tapePanel.destello(r.pos);
            diagramPanel.activar(r.desde, r.hasta, r.lee + "/" + r.escribe + "," + r.mov);
            listaActiva = listKeys.indexOf(r.clave);
            if (listaActiva >= 0) lista.ensureIndexIsVisible(listaActiva);
            lista.repaint();
        }
        if (sim.terminada) finalizar();
        int ultima = logModel.getRowCount() - 1;
        SwingUtilities.invokeLater(() -> logTable.scrollRectToVisible(logTable.getCellRect(ultima, 0, true)));
        actualizarBotones();
    }

    void finalizar() {
        detener();
        diagramPanel.estadoActual = sim.estado;
        diagramPanel.resultado = sim.acepta;
        String res = sim.acepta ? "ACEPTACIÓN" : "NO ACEPTACIÓN";
        logModel.addRow(new Object[]{"FIN", sim.estado, "", "", "", res});
        msgLabel.setForeground(sim.acepta ? OK : BAD);
        msgLabel.setText("Resultado: " + res + "  —  " + sim.motivo);
        int ultima = logModel.getRowCount() - 1;
        SwingUtilities.invokeLater(() -> logTable.scrollRectToVisible(logTable.getCellRect(ultima, 0, true)));
    }

    void play() {
        if (sim == null || sim.terminada) return;
        ejecutando = true;
        tapePanel.running = true;
        btnPlay.setText("Pausar");
        autoTimer.start();
    }

    void detener() {
        if (autoTimer.isRunning()) autoTimer.stop();
        ejecutando = false;
        tapePanel.running = false;
        btnPlay.setText("Ejecutar");
        actualizarBotones();
    }

    void actualizarBotones() {
        boolean listo = sim != null && !sim.terminada;
        btnPlay.setEnabled(listo);
        btnStep.setEnabled(listo);
        btnReset.setEnabled(sim != null);
        btnRun.setEnabled(maquina != null);
        campo.setEnabled(maquina != null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignorada) { }
            new TuringGUI().setVisible(true);
        });
    }
}
