# MANUAL DE COMPILACIÓN Y EJECUCIÓN
## Simulador de Máquina de Turing

### 4.1. Requisitos del sistema

Para compilar y ejecutar correctamente el programa se necesitan los siguientes elementos:

- Sistema operativo Windows 10/11, Linux o macOS.
- Java Development Kit (JDK) instalado.
- Editor de código o entorno de desarrollo, como IntelliJ IDEA o Visual Studio Code.
- Archivos fuente del proyecto.
- Un archivo de texto con la definición de la Máquina de Turing.

Se recomienda utilizar JDK 17 o una versión posterior.

### 4.2. Archivos del proyecto

El proyecto está compuesto principalmente por dos archivos Java:

| Archivo | Descripción |
|---|---|
| `TuringSimulator.java` | Contiene el motor de simulación, el modelo de la máquina, la lectura de archivos, las validaciones y la ejecución de las transiciones. |
| `TuringGUI.java` | Contiene la interfaz gráfica, la cinta animada, el diagrama de estados, los controles y el registro de pasos. |

También se necesita un archivo `.txt` que contenga la definición de la máquina que se desea simular.

### 4.3. Preparación del entorno

**Paso 1. Instalar Java JDK**

Descargar e instalar el JDK desde:

https://www.oracle.com/java/technologies/downloads/

**Paso 2. Verificar la instalación**

Abrir CMD o PowerShell y ejecutar:

```bash
java -version
```

Después ejecutar:

```bash
javac -version
```

Ambos comandos deben mostrar la versión de Java instalada.

**Paso 3. Organizar los archivos**

Crear una carpeta para el proyecto y colocar los archivos fuente dentro de una carpeta llamada `src`.

La estructura recomendada es:

```text
SimuladorMaquinaTuring/
│
├── src/
│   ├── TuringSimulator.java
│   └── TuringGUI.java
│
├── out/
│
└── ejemplo_anbn.txt
```

### 4.4. Compilación del programa

Abrir CMD o PowerShell en la carpeta principal del proyecto.

Ejecutar el siguiente comando:

```bash
javac -encoding UTF-8 -d out src\TuringSimulator.java src\TuringGUI.java
```

**Explicación del comando:**

- `javac`: compilador de Java.
- `-encoding UTF-8`: permite reconocer correctamente los caracteres especiales.
- `-d out`: indica que los archivos compilados se almacenarán en la carpeta `out`.
- `src\TuringSimulator.java`: archivo que contiene el motor de simulación.
- `src\TuringGUI.java`: archivo que contiene la interfaz gráfica.

Si la compilación termina sin mostrar errores, el programa estará listo para ejecutarse.

### 4.5. Ejecución del programa

Desde la carpeta principal del proyecto, ejecutar:

```bash
java -cp out TuringGUI
```

Este comando inicia la interfaz gráfica del simulador.

También es posible ejecutar la versión de consola mediante:

```bash
java -cp out TuringSimulator
```

### 4.6. Uso de la interfaz gráfica

**Paso 1. Cargar la definición de la máquina**

Al iniciar el programa, se mostrará la ventana principal del simulador.

Presionar el botón **Cargar definición…** y seleccionar el archivo `.txt` que contiene las instrucciones de la Máquina de Turing.

Una vez cargado correctamente, se mostrarán los estados, el estado inicial y el alfabeto.

**Paso 2. Ingresar la cadena**

Escribir en el campo de entrada la cadena que se desea procesar.

El programa valida los caracteres ingresados y verifica que pertenezcan al alfabeto definido para la máquina.

**Paso 3. Ejecutar la simulación**

Presionar el botón **Cargar cinta y ejecutar** o la tecla ENTER.

La cadena se colocará sobre la cinta y comenzará el procesamiento automático.

**Paso 4. Observar la cinta**

La cinta muestra los símbolos almacenados en cada posición.

El cabezal señala la posición actual de lectura y escritura, mientras que el indicador superior muestra el estado en el que se encuentra la máquina.

**Paso 5. Observar el diagrama de estados**

El diagrama representa gráficamente los estados de la máquina y las conexiones entre ellos.

Durante la ejecución, el arco correspondiente a la transición utilizada se resalta para identificar visualmente el cambio de estado.

**Paso 6. Consultar la función de transición**

En el panel izquierdo se presenta la función de transición δ.

La regla utilizada durante cada operación se resalta para facilitar la identificación de la instrucción aplicada.

**Paso 7. Consultar el registro de pasos**

En la parte inferior se encuentra una tabla que muestra:

- Paso ejecutado.
- Estado anterior.
- Símbolo leído.
- Símbolo escrito.
- Movimiento del cabezal.
- Nuevo estado.

**Paso 8. Controlar la ejecución**

El simulador dispone de los siguientes controles:

| Control | Función |
|---|---|
| Ejecutar | Inicia el procesamiento automático. |
| Pausar | Detiene temporalmente la ejecución automática. |
| Paso a paso | Ejecuta una transición individual. |
| Reiniciar | Restablece la cinta y comienza nuevamente con la misma cadena. |
| Velocidad | Permite modificar la velocidad de ejecución. |

### 4.7. Interpretación de resultados

Al finalizar la simulación, el programa presenta uno de los siguientes resultados:

**ACEPTACIÓN:** se alcanza un estado definido como estado de aceptación.

**NO ACEPTACIÓN:** se alcanza un estado de rechazo, no existe una transición para la combinación actual de estado y símbolo, o se alcanza el límite máximo de pasos.

El resultado también se muestra mediante colores en la interfaz y queda registrado en la tabla de ejecución.

### 4.8. Validaciones y controles

El programa incorpora diferentes mecanismos para evitar errores durante la simulación:

- Verifica que el archivo de definición pueda leerse.
- Comprueba que exista un conjunto de estados y un estado inicial válido.
- Verifica los estados de aceptación y rechazo.
- Comprueba que los símbolos y las transiciones estén definidos correctamente.
- Evita transiciones duplicadas.
- Valida los caracteres de la cadena de entrada.
- Establece un límite máximo de 100,000 pasos para evitar ciclos infinitos.

### 4.9. Consideraciones finales

Para ejecutar correctamente el programa, ambos archivos Java deben encontrarse en el proyecto y compilarse conjuntamente, ya que `TuringGUI` utiliza las clases definidas en `TuringSimulator`.

El archivo de definición debe respetar el formato establecido por el simulador.
