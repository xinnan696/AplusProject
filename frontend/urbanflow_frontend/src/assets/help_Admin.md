# Administrator Help Document

## 1. System Overview

Welcome to UrbanFlow!

UrbanFlow is an advanced traffic monitoring and signal intervention system developed on the SUMO simulation platform. As an **Administrator**, you hold the highest level of authority within this system. You can not only perform real-time monitoring and intervention on city-wide traffic like a Traffic Operator but also manage all user accounts, assign permissions, and view the entire system's operation logs. Your primary responsibilities are to ensure the smooth operation of the system, the correct configuration of user permissions, and a comprehensive audit of all system operations.

After logging in, you will first be directed to the **Control Page** to begin traffic monitoring and management tasks immediately.

## 2. Control Page

### 2.1 Page Layout Introduction

The Control Page utilizes an efficient partitioned layout, designed to allow users to observe the macroscopic traffic situation while performing microscopic operations simultaneously:

- **Left Navigation Bar**: Provides quick access to all major pages within the system (e.g., Control, Dashboard, Administration).
- **Central Map Display Area**: Occupies the majority of the page, used for real-time visualization of the city's road network and traffic conditions.
- **Right Operations Panel**: Integrates all function modules related to traffic intervention, serving as the core area for decision-making and execution.
- **Top Bar**: Located at the very top of the page, it contains the **AI Mode** switch for mode changes, a button to view operation records, and a user information button that includes the logout function.

### 2.2 Map Operations Introduction

The map is your primary canvas for observation and interaction, designed to be intuitive and powerful.

**Map Elements:**

- **Road Network**: The dense blue lines represent the city's roads. The road color changes dynamically based on real-time traffic flow data: blue represents smooth traffic, while yellow, orange, and red indicate progressively severe levels of congestion.
- **Traffic Light Icons**: The rectangular icons distributed on the map represent the traffic lights at various junctions.

**Map Navigation and Interaction:**

- **Zoom**: By clicking the **+ / -** buttons in the upper-left corner of the map or using the mouse scroll wheel, you can zoom in or out to view the macroscopic road network or the details of a specific junction.
- **Pan**: Press and hold the left mouse button on the map and drag to move the map's field of view.
- **Search**: Enter the name of a junction or road in the search box at the top of the map and click the search button. The map will automatically locate, center, and zoom in on the target.

**Click Interaction:**

- **Clicking a Road**: Left-click on any road, and the status bar at the bottom of the page (as shown in the image with "Current Location: No element selected") will display the name of that road.
- **Clicking a Traffic Light**: This is one of the most common interactions. Left-clicking any traffic light icon on the map will trigger two linked effects:
  1. The name of the junction to which the traffic light belongs will be automatically populated in the **Junction** field of the **Manual Control** section in the right-hand Operations Panel.
  2. On the map, all roads directly connected to this junction will be highlighted in a special color (usually purple), allowing you to clearly see the scope and impact of the junction.

### 2.3 Operations Panel Detailed Explanation

The Operations Panel is your "cockpit," divided into three functionally distinct modules.

**1. Congested Junctions**

- **Function**: This module monitors the city's traffic conditions in real-time and automatically lists the current Top 6 most congested junctions.
- **Information Display**: Each row displays information for a congested junction, including its **Location** (junction name) and **Queue Length** (number of queued/congested vehicles). A larger Queue Length value indicates more severe congestion.
- **Interaction**: This is a rapid response tool. When you click on any junction name in the list, the map will automatically focus and zoom in on that junction. Simultaneously, the junction's name will be auto-filled into the "Manual Control" module below. This saves you the step of manual searching, enabling "one-click positioning and intervention readiness" for congestion points.

**2. AI Suggestions**

- **Function**: This is one of the intelligent cores of the system. UrbanFlow's backend AI continuously analyzes traffic flow data. When it identifies a potential optimization opportunity (e.g., alleviating a forming congestion point by slightly adjusting signal timings), it will generate a specific optimization suggestion here.
- **Workflow**:
  - When a suggestion is available, this area will display its content (e.g., "Extend the green light for the north-south direction at junction XYZ by 15 seconds").
  - You can click the **APPLY** button, and the system will automatically adopt and execute the suggestion.
  - If you deem the suggestion inappropriate or have other higher-priority tasks, you can click the **IGNORE** button to dismiss it.
  - As shown in the image, when no suggestions are available, this area will display "No Available Suggestion," and the buttons will be grayed out and inactive.

**3. Manual Control**

- **Function**: This module grants the user full manual and fine-grained control over traffic lights, intended for handling complex situations not covered by the AI or for executing specific traffic dispatching strategies.
- **Step-by-Step Workflow**:
  1. **Select Junction**: You can auto-fill this field by clicking a traffic light icon on the map, or you can manually search for and select a target junction from the dropdown menu here.
  2. **Select Traffic Light**: Once a junction is selected, this dropdown menu becomes active, allowing you to choose the specific traffic flow direction to control (e.g., main road straight, side road left turn).
  3. **Set Light State**: Click the **RED** or **GREEN** button to specify the state you want the traffic light to switch to.
  4. **Set Duration**: Enter a number in the input box to define how long your new state will last (unit: seconds).
  5. **Submit Operation**:
     - Click the **APPLY** button to confirm and execute your settings. A success message will appear upon successful execution.
     - Click the **CANCEL** button to clear all your selections and inputs in this module, allowing you to start over.

## 3. Dashboard Page

The **Dashboard** page is the data visualization and analysis center of the UrbanFlow system. Its primary function is to present complex real-time and historical traffic data in the form of intuitive charts, helping users of different roles to quickly gain insights into traffic conditions, identify problems, and evaluate the effectiveness of their strategies.

For **Administrators**, it is a vital data support platform for long-term traffic pattern analysis, identification of structural traffic bottlenecks, and planning for future urban traffic development.

The page analyzes the city's traffic health comprehensively through four core charts, focusing on four key dimensions: macroscopic trends, congestion duration, traffic flow, and congestion frequency.

### 3.1 Detailed Explanation of Each Chart

**1. Congested Junction Count Trend**

- **Chart Meaning**: This line chart shows the trend of the total number of junctions in a "congested" state across the city over a selected time frame (e.g., the past 24 hours as shown in the image).
- **How to Interpret**:
  - The Y-axis represents the number of congested junctions.
  - The X-axis represents time.
  - The peaks of the curve indicate the busiest periods with the highest number of congested junctions (e.g., around 10 AM and 8 PM in the image); the troughs represent relatively smooth traffic periods (e.g., 6 AM to 8 AM).
- **Analytical Value**: This chart provides the "pulse" of the city's traffic, allowing managers to grasp the macroscopic health of the entire traffic network at a glance. It clearly reveals the daily peak and off-peak traffic hours. The small dots on the chart (like the one at 02:00) are typically used to mark the times of manual interventions or AI optimizations, making it easy to compare the change in the number of congested junctions before and after an intervention and thus evaluate its immediate effect.

**2. Junction Congestion Duration Ranking**

- **Chart Meaning**: This is a horizontal bar chart that ranks individual junctions based on their total accumulated congestion time within the selected period.
- **How to Interpret**:
  - The Y-axis lists the names of the junctions.
  - The X-axis represents the cumulative congestion time (in minutes).
  - The longer the bar, the longer that junction was in a congested state during the statistical period. As shown, "Park Lane" has the longest cumulative congestion duration, exceeding 80 minutes.
- **Analytical Value**: This chart precisely pinpoints the most persistent "stubborn" congestion points in the city. These junctions may be problematic due to poor road design, fundamentally flawed signal timing plans, or excessive pressure from nearby traffic attractors. They are key targets for urban planning and traffic optimization.

**3. Traffic Flow**

- **Chart Meaning**: This line chart displays the changes in traffic flow for a selected junction (or the city as a whole) over time.
- **How to Interpret**:
  - The Y-axis represents traffic flow (typically the number of vehicles passing per unit of time).
  - The X-axis represents time.
  - The fluctuations of the curve directly reflect the tidal changes in traffic demand. As shown, a significant evening peak occurs between 4 PM and 8 PM.
- **Analytical Value**: This chart is fundamental to understanding traffic demand patterns. By analyzing the flow chart of a specific junction or area, it provides crucial data support for optimizing signal timing (e.g., adjusting green light times based on flow at different times of the day), planning road capacity, and designing traffic organization schemes (such as setting up tidal lanes). Users can select "All Junctions" from the top-left dropdown menu for a macroscopic flow view or switch to a specific junction for microscopic analysis.

**4. Top Congested Times**

- **Chart Meaning**: This is a vertical bar chart that ranks junctions based on the number of times they became congested within the selected period.
- **How to Interpret**:
  - The X-axis lists the names of the junctions.
  - The Y-axis represents the number of congestion events.
  - The taller the bar, the more frequently that junction entered a congested state. As shown, "Main St" is the most frequently congested junction.
- **Analytical Value**: This chart is used to identify the junctions most prone to congestion. Unlike the "Congestion Duration Ranking," the junctions here might not stay congested for long, but the congestion occurs repeatedly. This often suggests that the junction's traffic condition is highly unstable and may be very sensitive to sudden traffic surges or minor disturbances. Analyzing this chart in conjunction with the "Junction Congestion Duration Ranking" provides a more comprehensive diagnosis of a junction's congestion characteristics.

## 4. Emergency Vehicle Priority Page

### 4.1 Page Overview and Purpose

The Emergency Vehicle Priority Page is a special operational mode within the UrbanFlow system. It is not a standard navigation page but rather a dedicated command interface that the **Control Page** temporarily transforms into when responding to an emergency event.

The core objective of this page is to create an unobstructed "green wave" for emergency vehicles on a mission (such as ambulances, fire trucks, and police cars). Through a highly focused, distraction-free interface, the operator can quickly and precisely grant green lights for upcoming junctions ahead of the emergency vehicle, minimizing its travel delays and saving precious time for life-saving and emergency response.

### 4.2 Entering Emergency Priority Mode

You do not navigate to this page via the navigation bar. The only way to enter this mode is through an alert trigger:

1. **Alert Trigger**: When an emergency vehicle sends a priority transit request, a red, continuously flashing lightbulb icon will appear in the top-right section of the **Control Page**. This is a high-priority alert prompting your immediate attention.
2. **Responding to the Alert**: Click this flashing lightbulb icon. The system will immediately display a request confirmation window, signaling your entry into the emergency priority mode.

### 4.3 Request Confirmation and Information Display

After clicking the alert icon, a modal window will pop up in the center of the screen, displaying detailed information about the emergency event for your rapid assessment. The window includes:

- **Vehicle License Plate**: The unique identifier of the emergency vehicle.
- **Agency**: The vehicle's organization, e.g., "Dublin Fire Brigade" or "National Ambulance Service."
- **Upcoming Junctions**: A sequential list of all key junctions the vehicle is scheduled to pass through.

Below this information, there are two action buttons:

- **APPLY**: Clicking this button confirms that you accept the request and will provide transit priority. The system will immediately switch to the dedicated priority transit main interface.
- **IGNORE**: Clicking this button means you reject or ignore the request. The pop-up will close, and the system will return to the standard Control Page.

### 4.4 Priority Main Interface Explained

Once you click **APPLY**, the entire Control Page transforms into a new, highly simplified interface:

**Map View:**

- **Highlighted Route**: The map will automatically draw the vehicle's complete intended route from start to finish in a conspicuous color (e.g., bright green).
- **Vehicle Tracking**: A special icon on the map will show the real-time location of the emergency vehicle, updating as it moves.
- **Focus Lock**: The map view will be locked and focused on the emergency vehicle and its surrounding road network. Other irrelevant information will be faded out to keep your attention solely on the mission route.

**Operations Panel View:**

- **Simplified Interface**: The right-hand Operations Panel will hide all standard modules like "Congested Junctions" and "AI Suggestions."
- **Task-Oriented**: The new panel transforms into a task list, sequentially displaying information for the upcoming junctions, typically including **Junction** name, **From** road, and **To** road.
- **Dynamic Activation**: This is the mode's most critical design feature. The signal control options (like the **RED**/**GREEN** buttons for Light State and the **Duration** input) are grayed out and disabled by default. **Only when the emergency vehicle is about to reach a specific junction will the control options for that junction become automatically activated and usable.** This design effectively prevents the operator from causing unnecessary congestion on other roads by activating green lights too early.

### 4.5 Operational Flow

1. On the main interface, closely monitor the emergency vehicle's position on the map and its movement along the task route.
2. As the vehicle approaches the first junction in the list, watch for its corresponding signal control section in the Operations Panel to light up and become active.
3. Immediately select the **GREEN** state for the vehicle's direction of travel and click the **APPLY** button. The system will automatically set that direction to green and all conflicting directions to red.
4. After the vehicle passes through successfully, continue to monitor its progress and repeat the same operation for the next upcoming junction.
5. Provide a green wave for each junction on the route in sequence until the vehicle leaves your jurisdiction or reaches its destination.
6. Once the entire priority event is complete, the page will automatically revert to the standard **Control Page** layout.

## 5. Administration Page

This page is your core area for system management, divided into two sub-pages: **User** management and **Log** management.

### 5.1 User Management (Users)

- **User List**: A centralized display of all user information (ID, Username, Name, Status, Role). Supports searching by username and filtering by status (Active/Inactive).
- **Add User**: Click **+ Add User**, fill in information such as username, name, email, and role, and assign a management area.
- **Edit User**: Click **Edit** to modify a user's name, department, email, phone number, etc.
- **Delete User**: Click **Delete** to remove a single user. Check multiple users and click **Delete Selected** for batch deletion.
- **View Details**: Click **Details** to see a read-only view of all of a user's information.

### 5.2 Log Management (User Logs)

- **Log List**: Records detailed operations of all users in the system, including operation time, username, name, operation type (Manual/AI/Login), module, and operation details.
- **Filter Function**: Allows for precise filtering by time range and username for auditing and traceability.
- **Export Function**: Click the **Export** button to export the currently filtered log results into a file (e.g., CSV/JSON) for offline analysis and archiving.