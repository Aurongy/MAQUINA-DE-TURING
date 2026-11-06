import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SIMULADOR DE MAQUINA DE TURING
 *
 * Lee la definición formal de una MT desde un archivo de texto, permite
 * ingresar una cadena en la cinta (validada contra los alfabetos) y simula
 * paso a paso la ejecución, terminando en ACEPTACION o NO ACEPTACION.
 *
 * Formato del archivo (los saltos de línea y espacios son libres):
 *   Estados( q0, q1, ... )
 *   Inicial( q0 )
 *   Ha( qa )                 estado(s) de aceptación (halt accept)
 *   He( qr )                 estado(s) de rechazo   (halt error/reject)
 *   Alfabeto( a, b, ... )    alfabeto de entrada
 *   Cinta( X, Y, _ )         (opcional) símbolos adicionales de la cinta
 *   MT( q0, a ) = ( q1, X, D )     D = derecha, I = izquierda, N = no mueve
 *
 * El símbolo "_" es el blanco de la cinta y siempre pertenece a la cinta.
 * Si no existe transición para (estado, símbolo), la máquina se detiene
 * en NO ACEPTACION.
 */
public class TuringSimulator {

    /** Símbolo blanco de la cinta. */
    static final String BLANCO = "_";
    /** Límite de pasos para evitar ciclos infinitos. */
    static final int MAX_PASOS = 100000;

    // ------------------------------------------------------------------
    // Modelo de la máquina
    // ------------------------------------------------------------------

    /** Resultado de una transición: δ(q, s) = (nuevoEstado, escribir, mov). */
    static class Transicion {
        final String estado, escribir;
        final char mov;
        Transicion(String estado, String escribir, char mov) {
            this.estado = estado; this.escribir = escribir; this.mov = mov;
        }
    }

    /** Definición formal de la MT. */
    static class Maquina {
        Set<String> estados = new LinkedHashSet<>();
        String inicial;
        Set<String> aceptacion = new LinkedHashSet<>();
        Set<String> rechazo = new LinkedHashSet<>();
        Set<String> alfabeto = new LinkedHashSet<>();
        Set<String> simbolosCinta = new LinkedHashSet<>();
        Map<String, Transicion> delta = new HashMap<>();

        static String clave(String q, String s) { return q + "\u0000" + s; }
    }

    // ------------------------------------------------------------------
    // Lectura y validación del archivo de definición
    // ------------------------------------------------------------------

    /** Separa una lista "a, b, c" en elementos sin espacios. */
    static List<String> lista(String texto) {
        List<String> r = new ArrayList<>();
        for (String s : texto.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) r.add(s);
        }
        return r;
    }

    /** Carga y valida la definición de una MT desde un archivo. */
    static Maquina cargar(String ruta) throws IOException {
        String texto = new String(Files.readAllBytes(Paths.get(ruta)), StandardCharsets.UTF_8);
        Maquina m = new Maquina();

        // 1) Transiciones: MT(q, s) = (p, w, M)
        Pattern pt = Pattern.compile(
            "MT\\s*\\(\\s*([^,()]+?)\\s*,\\s*([^,()]+?)\\s*\\)\\s*=\\s*\\(\\s*([^,()]+?)\\s*,\\s*([^,()]+?)\\s*,\\s*([DINdin])\\s*\\)",
            Pattern.CASE_INSENSITIVE);
        Matcher mt = pt.matcher(texto);
        List<String[]> trans = new ArrayList<>();
        while (mt.find()) {
            trans.add(new String[]{mt.group(1), mt.group(2), mt.group(3), mt.group(4),
                                   mt.group(5).toUpperCase()});
        }
        // Se eliminan del texto para analizar el resto de las secciones
        String resto = pt.matcher(texto).replaceAll(" ");

        // 2) Secciones: Nombre( lista )
        Matcher ms = Pattern.compile("([A-Za-z]+)\\s*\\(([^()]*)\\)").matcher(resto);
        while (ms.find()) {
            String nombre = ms.group(1).toLowerCase();
            List<String> items = lista(ms.group(2));
            switch (nombre) {
                case "estados":  m.estados.addAll(items); break;
                case "inicial":
                    if (items.size() != 1)
                        throw new IllegalArgumentException("Inicial() debe tener exactamente un estado.");
                    m.inicial = items.get(0); break;
                case "ha":       m.aceptacion.addAll(items); break;
                case "he":       m.rechazo.addAll(items); break;
                case "alfabeto": m.alfabeto.addAll(items); break;
                case "cinta":    m.simbolosCinta.addAll(items); break;
                default:
                    throw new IllegalArgumentException("Sección desconocida: " + ms.group(1));
            }
        }

        // 3) Validaciones de consistencia
        if (m.estados.isEmpty()) throw new IllegalArgumentException("Falta la sección Estados().");
        if (m.inicial == null)   throw new IllegalArgumentException("Falta la sección Inicial().");
        if (!m.estados.contains(m.inicial))
            throw new IllegalArgumentException("El estado inicial '" + m.inicial + "' no está en Estados().");
        for (String q : m.aceptacion)
            if (!m.estados.contains(q)) throw new IllegalArgumentException("Estado de aceptación inválido: " + q);
        for (String q : m.rechazo)
            if (!m.estados.contains(q)) throw new IllegalArgumentException("Estado de rechazo inválido: " + q);
        if (m.alfabeto.isEmpty()) throw new IllegalArgumentException("Falta la sección Alfabeto().");

        // Símbolos de la cinta = alfabeto + blanco + adicionales + escritos en transiciones
        m.simbolosCinta.addAll(m.alfabeto);
        m.simbolosCinta.add(BLANCO);
        for (String[] t : trans) m.simbolosCinta.add(t[3]);

        for (String[] t : trans) {
            if (!m.estados.contains(t[0]) || !m.estados.contains(t[2]))
                throw new IllegalArgumentException("Transición con estado no definido: MT(" + t[0] + "," + t[1] + ")");
            if (!m.simbolosCinta.contains(t[1]))
                throw new IllegalArgumentException("Símbolo leído no definido en la cinta: " + t[1]);
            String k = Maquina.clave(t[0], t[1]);
            if (m.delta.containsKey(k))
                throw new IllegalArgumentException("Transición duplicada (no determinista): MT(" + t[0] + "," + t[1] + ")");
            m.delta.put(k, new Transicion(t[2], t[3], t[4].charAt(0)));
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Simulación
    // ------------------------------------------------------------------

    /** Muestra la configuración actual: estado y cinta con la cabeza entre [ ]. */
    static void imprimir(int paso, String estado, List<String> cinta, int cabeza) {
        int fin = cinta.size() - 1;
        while (fin > cabeza && cinta.get(fin).equals(BLANCO)) fin--;   // recorta blancos a la derecha
        int ini = 0;
        while (ini < cabeza && cinta.get(ini).equals(BLANCO)) ini++;   // recorta blancos a la izquierda
        StringBuilder sb = new StringBuilder();
        for (int i = ini; i <= fin; i++) {
            sb.append(i == cabeza ? "[" + cinta.get(i) + "]" : " " + cinta.get(i) + " ");
        }
        System.out.printf("Paso %-4d | Estado: %-6s | ...%s...%n", paso, estado, sb);
    }

    /** Ejecuta la MT sobre la cadena. Devuelve true si ACEPTA. */
    static boolean simular(Maquina m, String entrada) {
        // Cinta "infinita": lista que crece hacia ambos lados cuando se necesita
        List<String> cinta = new ArrayList<>();
        for (char c : entrada.toCharArray()) cinta.add(String.valueOf(c));
        if (cinta.isEmpty()) cinta.add(BLANCO);

        int cabeza = 0;
        String estado = m.inicial;
        int paso = 0;

        System.out.println("\n--- INICIO DE LA SIMULACION ---");
        imprimir(paso, estado, cinta, cabeza);

        while (true) {
            if (m.aceptacion.contains(estado)) return true;
            if (m.rechazo.contains(estado)) return false;
            if (paso >= MAX_PASOS) {
                System.out.println("Se alcanzó el límite de " + MAX_PASOS + " pasos (posible ciclo infinito).");
                return false;
            }

            Transicion t = m.delta.get(Maquina.clave(estado, cinta.get(cabeza)));
            if (t == null) {
                System.out.println("No existe transición para (" + estado + ", " + cinta.get(cabeza) + ").");
                return false;
            }

            cinta.set(cabeza, t.escribir);
            estado = t.estado;
            if (t.mov == 'D') cabeza++;
            else if (t.mov == 'I') cabeza--;

            // Extender la cinta si la cabeza sale de los límites
            if (cabeza < 0) { cinta.add(0, BLANCO); cabeza = 0; }
            if (cabeza >= cinta.size()) cinta.add(BLANCO);

            paso++;
            imprimir(paso, estado, cinta, cabeza);
        }
    }

    // ------------------------------------------------------------------
    // Interacción con el usuario
    // ------------------------------------------------------------------

    /** Pide la ruta del archivo hasta cargar una definición válida. */
    static Maquina pedirMaquina(Scanner sc) {
        while (true) {
            System.out.print("\nUbicación del archivo con la definición de la MT: ");
            String ruta = sc.nextLine().trim();
            try {
                Maquina m = cargar(ruta);
                System.out.println("Definición cargada correctamente.");
                System.out.println("  Estados  : " + m.estados);
                System.out.println("  Inicial  : " + m.inicial);
                System.out.println("  Ha / He  : " + m.aceptacion + " / " + m.rechazo);
                System.out.println("  Alfabeto : " + m.alfabeto);
                System.out.println("  Cinta    : " + m.simbolosCinta);
                return m;
            } catch (IOException e) {
                System.out.println("Error: no se pudo leer el archivo (" + e.getMessage() + ").");
            } catch (IllegalArgumentException e) {
                System.out.println("Error en la definición: " + e.getMessage());
            }
        }
    }

    /**
     * Pide la cadena de entrada. El ingreso termina con ENTER.
     * Valida que cada carácter pertenezca al alfabeto y a los símbolos de la cinta.
     * Una línea vacía equivale a una cinta en blanco (cadena vacía).
     */
    static String pedirCadena(Scanner sc, Maquina m) {
        while (true) {
            System.out.print("\nIngrese la cadena para la cinta (ENTER para terminar): ");
            String s = sc.nextLine();
            String invalido = null;
            for (char c : s.toCharArray()) {
                String x = String.valueOf(c);
                if (!m.alfabeto.contains(x) || !m.simbolosCinta.contains(x)) { invalido = x; break; }
            }
            if (invalido == null) return s;
            System.out.println("Carácter '" + invalido + "' no definido en el alfabeto/cinta de la máquina. Intente de nuevo.");
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SIMULADOR DE MAQUINA DE TURING ===");
        Maquina m = pedirMaquina(sc);

        while (true) {
            String cadena = pedirCadena(sc, m);
            boolean ok = simular(m, cadena);
            System.out.println("--- FIN DE LA SIMULACION ---");
            System.out.println(ok ? "RESULTADO: ACEPTACION" : "RESULTADO: NO ACEPTACION");

            System.out.println("\n1) Nueva cinta (misma máquina)");
            System.out.println("2) Nueva definición de máquina");
            System.out.println("3) Salir");
            System.out.print("Opción: ");
            String op = sc.nextLine().trim();
            if (op.equals("2")) m = pedirMaquina(sc);
            else if (op.equals("3")) break;
            // cualquier otra opción (incluida 1) -> nueva cinta
        }
        System.out.println("Fin del programa.");
        sc.close();
    }
}
