# AAAAProject README

This guide provides instructions on how to set up and run this project.

---

## Launching and Running Dependency Services (Docker)

This project uses **Docker Compose** as a one-stop solution to manage and run the entire backend stack. This includes all required dependency services (**MySQL**, **Redis**, **InfluxDB**) as well as all the **Java Spring Boot microservice modules**. This approach guarantees a consistent and reproducible environment.

### **1. Prerequisites**

Before you begin, please ensure you have the following software installed on your system:

* **Docker Desktop**: [Official Download Link](https://www.docker.com/products/docker-desktop/)
* **IntelliJ IDEA Ultimate**: The Ultimate edition includes robust, built-in support for Docker.
* **Apache Maven Version**: 3.9.9
* **Java Version**: JDK 17

### **2. The Two-Step Startup Workflow**

Running the full backend system is a two-step process: first, you package the Java applications locally, and second, you use Docker Compose to build the images and launch everything.

#### Step 1: Package the Java Application

Docker needs the compiled `.jar` files to build the application images. You must run this command anytime you make changes to the Java code.

1. **Navigate to Directory**: Open your terminal and navigate to the Java project's root directory: `AplusProject/backend/urbanflow`.
2. **Run Maven Command**: Execute the following command to compile and package all modules.
   ```bash
   # For macOS / Linux
   mvn clean install -DskipTests

   # For Windows
   mvn clean install -DskipTests
   ```

   This will create the necessary `.jar` files in each module's `/target` directory.

#### Step 2: Launch with Docker Compose

Once the `.jar` files are created, you can start the entire system.

We provide two methods for launching the containers. Using IntelliJ IDEA's integration is highly recommended.

##### **Method 1: Run with IntelliJ IDEA**

This is the easiest and most intuitive approach.

1. **Locate the File**: In the IntelliJ IDEA project view, find and open the `docker-compose.yaml` file located at `AplusProject/backend/urbanflow/docker-compose.yaml`.
2. **One-Click Start**: Click the green **double-arrow icon** in the gutter next to the line numbers, then select **'Run'**.
3. **Manage Services**: After launching, the **"Services"** tool window will automatically open at the bottom of the IDE. Here, you can easily:

* View the running status of all containers.
* Monitor real-time logs for each container.
* Stop all services by clicking the red 'Stop' button at the top of the window.

##### **Method 2: Run with the Terminal**

You can also use the command line, including the terminal integrated within IntelliJ IDEA.

1. **Navigate to Directory**: Open your terminal and ensure you are in the directory containing the `docker-compose.yaml` file (`AplusProject/backend/urbanflow`).
2. **Run Command**: Execute the following command to start all services in the background:
   ```bash
   docker-compose up -d
   ```

### **3. Verifying Status**

* **Via IntelliJ IDEA**: Check the "Services" tool window and confirm that the status for all services is "running".
* **Via Terminal**: In the directory containing `docker-compose.yaml`, run `docker-compose ps` and check that the `State` for all containers is `Up`.

### **4. Stopping the Containers**

* **Via IntelliJ IDEA**: Click the red **'Stop'** button in the "Services" tool window.
* **Via Terminal**: In the directory containing `docker-compose.yaml`, run `docker-compose down`.

---

## Launching SUMO and the TraCI Module

This section guides you through configuring and launching the SUMO traffic simulation environment and the TraCI module.

### **1. Module Structure**

```
traciModel/
├── venv/                           # Virtual environment
├── config.json                     # SUMO executable and map file paths
├── event_manager.py                # Manages special simulation events
├── junction_data_processor.py      # Processes data from junctions
├── junction_flow_relations.sql     # SQL queries for traffic flow
├── test_*.py                       # Unit tests for various modules
└── withRedis.py                    # Main application entrypoint with Redis integration
```

### **2. Prerequisites**

* **Download and Install SUMO**: Get the latest version from the [official SUMO website](https://www.eclipse.org/sumo/) and complete the installation.
* **Copy Map Configuration Files**:

  1. Locate the `data/map_configuration` folder within this project.
  2. Copy the entire `map_configuration` folder into the `tools` directory of your SUMO installation path (e.g., `C:\Program Files\Eclipse\SUMO\tools`).

### **3. Configuration**

You must configure file paths to connect the project with the SUMO executable.

1. **Navigate to the TraCI Module**: Go to the module path in your project directory: `AplusProject/backend/traciModel`.
2. **Modify `config.json`**: Open the `config.json` file and update the values for two keys:

   * `"binary_path"`: This must be the **absolute path** to the `sumo` executable in your SUMO `bin` folder.
     * *Windows Example*: `"C:/Program Files/Eclipse/SUMO/bin/sumo.exe"`
     * *macOS/Linux Example*: `"/usr/local/bin/sumo"`
   * `"config_file_path"`: This must be the **absolute path** to the `osm.sumocfg` file inside the `map_configuration` folder you copied earlier.
     * *Windows Example*: `"C:/Program Files/Eclipse/SUMO/tools/map_configuration/osm.sumocfg"`
     * *macOS/Linux Example*: `"/path/to/sumo/tools/map_configuration/osm.sumocfg"`

   An example `config.json` file:

   ```json
   {
     "binary_path": "C:/Program Files/Eclipse/SUMO/bin/sumo.exe",
     "config_file_path": "C:/Program Files/Eclipse/SUMO/tools/map_configuration/osm.sumocfg"
   }
   ```

### **4. Running or Stopping the Simulation**

1. **Start Redis Container**: Ensure your Docker environment is running and the `redis` container has been started using the instructions in the first section.
2. **Launch TraCI Service**: Open your terminal, navigate to the TraCI module directory, and run the server.
   ```bash
   cd /path/to/your/AplusProject/backend/traciModel
   uvicorn withRedis:app --host="0.0.0.0" --port=8000
   ```
3. **Stop the Simulation**: Press `Ctrl + C` in the terminal to stop the service

---

## AI Module Setup and Usage

### **1. Module Overview**

The system analyzes intersection data including vehicle counts, waiting vehicle counts, and current signal states to predict optimal traffic light control strategies using a Random Forest classifier.

### **2. Module Structure**

```
AI/
├── data/                              # Data directory
│   ├── cleaned_traffic_data_steps_2000_to_3000.csv
│   └── data_with_label.csv
├── data_processing.py                 # Data processing script
├── model.py                          # Model training script
├── main.py                           # FastAPI main application file
├── suggestion_model.pkl              # Trained model file
└── README.md                         # Project documentation
```

### **3. Requirements**

- Python 3.7+
- pip (Python package manager)

### **4. Install Dependencies**

```bash
pip install fastapi uvicorn pandas scikit-learn joblib pydantic
```

### **5. Start Service**

#### Windows

1. Open Command Prompt (CMD) or PowerShell
2. Navigate to project directory:
   ```cmd
   cd C:\path\to\your\AplusProject\backend\AI
   ```
3. Start the service:
   ```cmd
   uvicorn main:app --reload --host 0.0.0.0 --port 8001
   ```

#### macOS

1. Open Terminal
2. Navigate to project directory:
   ```bash
   cd /path/to/your/AplusProject/backend/AI
   ```
3. Start the service:
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8001
   ```

### **6. Development**

#### Retrain Model

To retrain the model:

1. Run data processing script:

   ```bash
   python data_processing.py
   ```
2. Train the model:

   ```bash
   python model.py
   ```

#### Stop Service

Press `Ctrl + C` in the terminal to stop the service

### Troubleshooting

1. **Port in use**: Change port number, e.g., use `--port 8002`
2. **Module not found**: Ensure all dependencies are installed
3. **Permission issues**: May need to run as administrator on Windows
4. **Firewall issues**: Ensure firewall allows access to port 8001

---

## Frontend Setup and Usage

Follow these steps to get the frontend application running.

### **1. Install Dependencies**

Navigate to the frontend module directory and install the required npm packages.

```bash
cd urbanflow_frontend
npm install
```

### **2. Start Development Server**

Run the following command to start the frontend development server.

```bash
npm run dev
```

The application will be available at **http://localhost:5173/** by default.

### **3. Default Login Credentials**

Use the default administrator account to log in.

* **Account**: `admin01`
* **Password**: `admin123`

Other user roles (Traffic Operator, Urban Planner) can be created by the administrator via the **Users -\> Add User** menu.
