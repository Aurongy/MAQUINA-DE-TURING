# MANUAL DE COMPILACIÓN Y EJECUCIÓN
## Simulador de Máquina de Turing

# I. REQUISITOS DEL SISTEMA

Para compilar y ejecutar correctamente el programa se necesitan los siguientes elementos:

- Sistema operativo Windows 10/11, Linux o macOS.
- Java Development Kit (JDK) instalado.
- Editor de código o entorno de desarrollo, como IntelliJ IDEA o Visual Studio Code.
- Archivos fuente del proyecto.
- Un archivo de texto con la definición de la Máquina de Turing.

Se recomienda utilizar JDK 17 o una versión posterior.

# II. ARCHIVOS DEL PROYECTO

El proyecto está compuesto principalmente por dos archivos Java:

| Archivo | Descripción |
|---|---|
| `TuringSimulator.java` | Contiene el motor de simulación, el modelo de la máquina, la lectura de archivos, las validaciones y la ejecución de las transiciones. |
| `TuringGUI.java` | Contiene la interfaz gráfica, la cinta animada, el diagrama de estados, los controles y el registro de pasos. |

También se necesita un archivo `.txt` que contenga la definición de la máquina que se desea simular.

# III. PREPARACIÓN DEL ENTORNO

### 3.1. Instalar Java JDK

Descargar e instalar el JDK desde el sitio oficial de Oracle:

https://www.oracle.com/java/technologies/downloads/

### 3.2. Verificar la instalación

Abrir CMD o PowerShell y ejecutar:

```bash
java -version
```

Después ejecutar:

```bash
javac -version
```

Ambos comandos deben mostrar la versión de Java instalada.

### 3.3. Organizar los archivos

Crear una carpeta para el proyecto y colocar los archivos fuente dentro de una carpeta llamada `src`.

Estructura recomendada:

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

# IV. COMPILACIÓN DEL PROGRAMA

Abrir CMD o PowerShell en la carpeta principal del proyecto.

Ejecutar el siguiente comando:

```bash
javac -encoding UTF-8 -d out TuringSimulator.java TuringGUI.java
```

### 4.1. Explicación del comando

- `javac`: compilador de Java.
- `-encoding UTF-8`: permite reconocer correctamente los caracteres especiales.
- `-d out`: indica que los archivos compilados se almacenarán en la carpeta `out`.
- `src\TuringSimulator.java`: archivo que contiene el motor de simulación.
- `src\TuringGUI.java`: archivo que contiene la interfaz gráfica.

Si la compilación termina sin mostrar errores, el programa estará listo para ejecutarse.

<img width="1153" height="67" alt="image" src="https://github.com/user-attachments/assets/45a5ea33-cc2e-43d4-b1dc-ae15313a284e" />

<br><br>

*Figura 1. Verificación de la instalación de Java mediante CMD o PowerShell.*

<img width="1167" height="630" alt="image" src="https://github.com/user-attachments/assets/4fd603c4-99d5-4aa6-8b65-3f1561b12185" />

<br><br>

*Figura 2. Compilación correcta de los archivos TuringSimulator.java y TuringGUI.java.*

# V. EJECUCIÓN DEL PROGRAMA

Desde la carpeta principal del proyecto, ejecutar:

```bash
java -cp out TuringGUI
```

Este comando inicia la interfaz gráfica del simulador.

También es posible ejecutar la versión de consola mediante:

```bash
java -cp out TuringSimulator
```
<img width="1912" height="1016" alt="image" src="https://github.com/user-attachments/assets/2e11c565-c5a6-46d5-8adc-e9635b733f66" />

*Figura 3. Ventana principal del Simulador de Máquina de Turing.*

# VI. USO DE LA INTERFAZ GRÁFICA

### 6.1. Cargar la definición de la máquina

Al iniciar el programa, se mostrará la ventana principal del simulador.

Presionar el botón **Cargar definición…** y seleccionar el archivo `.txt` que contiene las instrucciones de la Máquina de Turing.

Una vez cargado correctamente, se mostrarán los estados, el estado inicial y el alfabeto.

<img width="1905" height="732" alt="image" src="https://github.com/user-attachments/assets/242ebb5f-f750-4fbe-bb4b-8496acd49942" />

<br><br>

*Figura 4. Selección y carga del archivo de definición de la máquina.*

### 6.2. Ingresar la cadena

Escribir en el campo de entrada la cadena que se desea procesar.

El programa valida los caracteres ingresados y verifica que pertenezcan al alfabeto definido para la máquina.

<img width="1907" height="188" alt="image" src="https://github.com/user-attachments/assets/a87eb31f-596a-49a0-aeb2-75c0bb042b4f" />

<br><br>

*Figura 5. Ingreso de una cadena para iniciar la simulación.*

### 6.3. Ejecutar la simulación

Presionar el botón **Cargar cinta y ejecutar** o la tecla ENTER.

La cadena se colocará sobre la cinta y comenzará el procesamiento automático.

### 6.4. Observar la cinta

La cinta muestra los símbolos almacenados en cada posición.

El cabezal señala la posición actual de lectura y escritura, mientras que el indicador superior muestra el estado en el que se encuentra la máquina.

<img width="1541" height="267" alt="image" src="https://github.com/user-attachments/assets/a3aa89bb-0433-4148-a17c-23487e6aaaf7" />

<br><br>

*Figura 6. Representación gráfica de la cinta y posición del cabezal.*

### 6.5. Observar el diagrama de estados

El diagrama representa gráficamente los estados de la máquina y las conexiones entre ellos.

Durante la ejecución, el arco correspondiente a la transición utilizada se resalta para identificar visualmente el cambio de estado.
<img width="1258" height="262" alt="image" src="https://github.com/user-attachments/assets/9ee2bc70-030e-485e-83ca-bfbf615548cc" />

<br><br>

*Figura 7. Diagrama de estados con la transición activa resaltada.*

### 6.6. Consultar la función de transición

En el panel izquierdo se presenta la función de transición δ.

La regla utilizada durante cada operación se resalta para facilitar la identificación de la instrucción aplicada.

<img width="366" height="525" alt="image" src="https://github.com/user-attachments/assets/75b75bca-512f-4854-bce2-244603bbd38c" />

<br><br>

*Figura 8. Panel de transiciones de la Máquina de Turing.*

### 6.7. Consultar el registro de pasos

En la parte inferior se encuentra una tabla que muestra:

- Paso ejecutado.
- Estado anterior.
- Símbolo leído.
- Símbolo escrito.
- Movimiento del cabezal.
- Nuevo estado.

<img width="1912" height="232" alt="image" src="https://github.com/user-attachments/assets/c2d48349-e025-4c91-b299-482aad2bbb0f" />
<img width="1892" height="193" alt="image" src="https://github.com/user-attachments/assets/57df4981-4123-41fd-8966-b2efa9a279f9" />
<img width="1902" height="102" alt="image" src="https://github.com/user-attachments/assets/1e68e060-965a-47ca-9392-cd1bed91a169" />

<br><br>

*Figura 9. Registro de las operaciones realizadas durante la simulación.*

### 6.8. Controlar la ejecución

El simulador dispone de los siguientes controles:

| Control | Función |
|---|---|
| Ejecutar | Inicia el procesamiento automático. |
| Pausar | Detiene temporalmente la ejecución automática. |
| Paso a paso | Ejecuta una transición individual. |
| Reiniciar | Restablece la cinta y comienza nuevamente con la misma cadena. |
| Velocidad | Permite modificar la velocidad de ejecución. |

<img width="928" height="71" alt="image" src="https://github.com/user-attachments/assets/f910717e-1b8c-42cf-b909-5cccfa9f3cf0" />
<img width="207" height="86" alt="image" src="https://github.com/user-attachments/assets/e5cad79e-146e-4416-85da-7853dbdd55b6" />
<img width="202" height="80" alt="image" src="https://github.com/user-attachments/assets/76fa9ff1-29fe-4c96-9a22-759e987e1281" />

<br><br>

*Figura 10. Controles para ejecutar, pausar, avanzar paso a paso y reiniciar la simulación.*

# VII. INTERPRETACIÓN DE RESULTADOS

Al finalizar la simulación, el programa presenta uno de los siguientes resultados:

### 7.1. Aceptación

Se alcanza un estado definido como estado de aceptación. Esto indica que la cadena cumple con las condiciones establecidas por la Máquina de Turing.

### 7.2. No aceptación

Se alcanza un estado de rechazo, no existe una transición para la combinación actual de estado y símbolo, o se alcanza el límite máximo de pasos.

El resultado también se muestra mediante colores en la interfaz y queda registrado en la tabla de ejecución.

<img width="1542" height="497" alt="image" src="https://github.com/user-attachments/assets/91f02e37-6fe3-4939-8058-cbec0545c169" />

<br><br>

*Figura 11. Mensaje de aceptación al finalizar la simulación.*

<img width="1251" height="517" alt="image" src="https://github.com/user-attachments/assets/4351e45e-3e11-419f-8c4d-6294a275df42" />

<br><br>

*Figura 12. Mensaje de rechazo o no aceptación de una cadena.*

# VIII. VALIDACIONES Y CONTROLES

El programa incorpora diferentes mecanismos para evitar errores durante la simulación:

- Verifica que el archivo de definición pueda leerse.
- Comprueba que exista un conjunto de estados y un estado inicial válido.
- Verifica los estados de aceptación y rechazo.
- Comprueba que los símbolos y las transiciones estén definidos correctamente.
- Evita transiciones duplicadas.
- Valida los caracteres de la cadena de entrada.
- Establece un límite máximo de 100,000 pasos para evitar ciclos infinitos.

# IX. CONSIDERACIONES FINALES

Para ejecutar correctamente el programa, ambos archivos Java deben encontrarse en el proyecto y compilarse conjuntamente, ya que `TuringGUI` utiliza las clases definidas en `TuringSimulator`.

El archivo de definición debe respetar el formato establecido por el simulador.

Además, si se desea que el programa cargue automáticamente una definición al iniciar, el archivo `ejemplo_anbn.txt` debe estar ubicado en el directorio de trabajo desde el cual se ejecuta la aplicación.

Es importante comprobar que Java esté correctamente instalado y que los comandos de compilación y ejecución se ejecuten desde la carpeta principal del proyecto.
