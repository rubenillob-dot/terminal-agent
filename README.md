# Terminal Agent: Autonomous Software Engineering Agent in Java / Agente Autónomo de Ingeniería de Software en Java

[English Version](#english-version) | [Versión en Español](#versión-en-español) | [Detailed Guide / Guía Detallada](file:///home/alumnot/Documentos/terminal-agent/HOW_IT_WORKS.md)

---

<a name="english-version"></a>
## 🇬🇧 English Version

An enterprise-grade, autonomous software engineering agent system written in modern Java (compatible with OpenJDK 21/25). The agent operates through an autonomous lifecycle: receiving high-level requirements, generating structured execution plans, safely writing source files, invoking native compilers (`javac`), executing test suites, and performing automated self-healing upon detecting compiler errors or runtime exceptions.

### 🏛️ Architecture & Design Patterns

The architecture strictly follows SOLID principles and high-cohesion, low-coupling design:

- **Command Pattern ([`CommandExecutor`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/execution/CommandExecutor.java), [`ProcessCommandExecutor`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/execution/ProcessCommandExecutor.java))**: Decouples process spawning and OS-level execution (`ProcessBuilder`), ensuring thread-safe asynchronous stream consumption to prevent buffer deadlocks, and enforcing execution timeouts.
- **Strategy Pattern ([`TaskPlanner`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/planner/TaskPlanner.java), [`DefaultTaskPlanner`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/planner/DefaultTaskPlanner.java))**: Separates task decomposition and planning logic from the execution engine, allowing pluggable planning algorithms.
- **Sandboxed Workspace ([`FileManager`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/workspace/FileManager.java), [`WorkspaceManager`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/workspace/WorkspaceManager.java))**: Conforms strictly to security sandboxing, neutralizing path traversal attacks (`../`) and providing normalized path resolution.
- **Self-Healing Feedback Loop ([`SelfHealingEngine`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/healing/SelfHealingEngine.java), [`DiagnosticResult`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/healing/DiagnosticResult.java))**: Automatically intercepts `stdout`/`stderr` from failed compiler invocations (`javac`) or test runs, parses stack traces and line numbers via regex, and dynamically patches source files before retrying execution.
- **State & Pipeline Orchestrator ([`AutonomousAgent`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/core/AutonomousAgent.java), [`AgentContext`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/core/AgentContext.java))**: Coordinates the end-to-end lifecycle (`PENDING` -> `PLANNING` -> `EXECUTING` -> `VALIDATING` / `HEALING` -> `COMPLETED`).

### 📁 Project Structure

```
terminal-agent/
├── .gitignore
├── README.md
├── HOW_IT_WORKS.md                        # Complete technical manual and operational guide
└── src/
    └── com/
        ├── agent/
        │   ├── Main.java                  # CLI application launcher & demo
        │   ├── core/
        │   │   ├── AgentContext.java      # Thread-safe DI context & configuration
        │   │   └── AutonomousAgent.java   # Core orchestrator and lifecycle engine
        │   ├── execution/
        │   │   ├── CommandExecutor.java   # Command execution contract
        │   │   ├── ExecutionResult.java   # Command output, exit code, and metrics DTO
        │   │   └── ProcessCommandExecutor.java # ProcessBuilder implementation with timeout
        │   ├── healing/
        │   │   ├── DiagnosticResult.java  # Diagnosis DTO (root cause, line, target file)
        │   │   └── SelfHealingEngine.java # Javac and stacktrace diagnostic & repair engine
        │   ├── model/
        │   │   ├── Task.java              # Immutable task definition
        │   │   ├── TaskResult.java        # Execution outcome DTO
        │   │   └── TaskStatus.java        # Lifecycle state enum
        │   ├── planner/
        │   │   ├── DefaultTaskPlanner.java # Standard step decomposition strategy
        │   │   ├── ExecutionStep.java     # Individual plan step model
        │   │   ├── TaskPlan.java          # Immutable ordered plan container
        │   │   └── TaskPlanner.java       # Planning strategy contract
        │   ├── test/
        │   │   └── AgentTestSuite.java    # Standalone zero-dependency test runner
        │   └── workspace/
        │       ├── FileManager.java       # Sandboxed file manager contract
        │       └── WorkspaceManager.java  # Sandboxed file system implementation
        └── demo/
            └── cache/                     # Generated and validated enterprise cache
                ├── CacheValidationTest.java
                └── ConcurrentLruCache.java
```

### 🚀 Getting Started & Execution

1. **Prerequisites**: OpenJDK 21 or higher installed (`java`, `javac`).
2. **Compile Sources**:
   ```bash
   mkdir -p out
   javac -d out $(find src -name '*.java')
   ```
3. **Execute Test Suite**:
   ```bash
   java -cp out com.agent.test.AgentTestSuite
   ```
4. **Execute Main Agent Demo**:
   ```bash
   java -cp out com.agent.Main
   ```

---

<a name="versión-en-español"></a>
## 🇪🇸 Versión en Español

Un sistema de agente autónomo de ingeniería de software de nivel empresarial desarrollado en Java moderno (compatible con OpenJDK 21/25). El agente opera bajo un ciclo de vida autónomo: recibe especificaciones técnicas, elabora planes de ejecución estructurados, escribe de forma segura archivos fuente en el espacio de trabajo, compila nativamente con `javac`, ejecuta baterías de pruebas y realiza autocorrección (self-healing) automática ante fallos de compilación o excepciones en tiempo de ejecución.

### 🏛️ Arquitectura y Patrones de Diseño

La arquitectura implementa de forma estricta los principios SOLID, alta cohesión y bajo acoplamiento:

- **Patrón Command ([`CommandExecutor`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/execution/CommandExecutor.java), [`ProcessCommandExecutor`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/execution/ProcessCommandExecutor.java))**: Desacopla la invocación de procesos del sistema operativo (`ProcessBuilder`), garantizando consumo asíncrono y seguro de streams (`stdout`/`stderr`) para prevenir bloqueos de buffer y asegurando límites estrictos de tiempo de ejecución (timeouts).
- **Patrón Strategy ([`TaskPlanner`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/planner/TaskPlanner.java), [`DefaultTaskPlanner`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/planner/DefaultTaskPlanner.java))**: Desacopla la descomposición y planificación de pasos de la lógica de ejecución del agente, permitiendo registrar estrategias de planificación personalizadas.
- **Espacio de Trabajo Aislado (Sandbox) ([`FileManager`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/workspace/FileManager.java), [`WorkspaceManager`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/workspace/WorkspaceManager.java))**: Gestiona operaciones de archivos en un entorno seguro, neutralizando ataques de evasión de directorios (`../`) mediante normalización de rutas y validación de prefijos.
- **Bucle de Autocorrección / Self-Healing ([`SelfHealingEngine`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/healing/SelfHealingEngine.java), [`DiagnosticResult`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/healing/DiagnosticResult.java))**: Captura e interpreta diagnósticos de error emitidos por `javac` o trazas de ejecución (stacktraces), identifica la línea exacta y el fichero causante del error, y genera parches de código de manera autónoma antes de reintentar la compilación.
- **Orquestador de Estados y Tubería ([`AutonomousAgent`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/core/AutonomousAgent.java), [`AgentContext`](file:///home/alumnot/Documentos/terminal-agent/src/com/agent/core/AgentContext.java))**: Gestiona las transiciones del ciclo de vida (`PENDING` -> `PLANNING` -> `EXECUTING` -> `VALIDATING` / `HEALING` -> `COMPLETED`).

### 🚀 Requisitos y Puesta en Marcha

1. **Requisitos Previos**: OpenJDK 21 o superior instalado con `java` y `javac` en el `PATH`.
2. **Compilar el Proyecto**:
   ```bash
   mkdir -p out
   javac -d out $(find src -name '*.java')
   ```
3. **Ejecutar la Suite de Pruebas**:
   ```bash
   java -cp out com.agent.test.AgentTestSuite
   ```
4. **Ejecutar la Demostración Principal**:
   ```bash
   java -cp out com.agent.Main
   ```

Para una explicación exhaustiva y detallada de cómo funciona internamente y cómo programar tus propias tareas, consulta [HOW_IT_WORKS.md](file:///home/alumnot/Documentos/terminal-agent/HOW_IT_WORKS.md).