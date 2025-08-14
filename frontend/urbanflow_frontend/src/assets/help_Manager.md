# Traffic Operator Help Document

## 1. System Overview

Welcome to UrbanFlow!

UrbanFlow is an advanced traffic monitoring and signal intervention system. As a Traffic Operator, you are the core force ensuring the smooth flow of urban traffic. Through this system, you can monitor the traffic conditions in your assigned area in real-time and use AI suggestions or manual operations to intervene with traffic signals to alleviate congestion and respond to emergencies.

After logging in, you will first be directed to the **Control Page** to begin your traffic management work immediately.

You have permission to access the following pages: Login Page, Control Page, Dashboard Page, and Help Page.

---

## 2. Control Page

### 2.1 Page Layout Introduction

The Control Page uses an efficient, partitioned layout designed to allow users to observe the macroscopic traffic situation while performing microscopic operations simultaneously:

- **Left Navigation Bar:** Provides quick access to the system's main pages (e.g., Control, Dashboard, Management).
- **Central Map Display Area:** Occupies the majority of the page, used for real-time visualization of the city's road network and traffic conditions.
- **Right Operations Panel:** Integrates all functional modules related to traffic intervention, serving as the core area for decision-making and execution.
- **Top Bar:** Located at the very top of the page, it contains the **AI Mode** switch for changing modes, a button to view operation history, and a user information button that includes the logout function.

### 2.2 Map Operations Introduction

The map is your primary canvas for observation and interaction, designed to be intuitive and powerful.

- **Map Elements:**
  - **Road Network:** The dense blue lines on the map represent the city's roads. The road color changes dynamically based on real-time traffic flow data: **blue** represents smooth traffic, while **yellow**, **orange**, and **red** indicate progressively severe levels of congestion.
  - **Traffic Light Icons:** The rectangular icons distributed on the map represent traffic signals at various intersections.
- **Map Navigation and Interaction:**
  - **Zoom:** By clicking the **+ / -** buttons in the upper-left corner of the map or using the mouse scroll wheel, you can zoom in or out to view the macroscopic road network or details of a specific intersection.
  - **Pan:** Press and hold the left mouse button on the map and drag to move the map's view.
  - **Search:** Enter the name of an intersection or road in the search box at the top of the map and click the search button. The map will automatically locate, center, and zoom in on the target.
- **Click Interaction:**
  - **Clicking a Road:** Left-click on any road, and the status bar at the bottom of the page (as shown in the image with "Current Location: No element selected") will display the name of that road.
  - **Clicking a Traffic Light:** This is one of the most common interactions. Left-clicking on any traffic light icon on the map will trigger two linked effects:
    1. The name of the junction to which the traffic light belongs will be automatically populated in the **Junction** field of the **Manual Control** section in the right-hand Operations Panel.
    2. On the map, all roads directly connected to this intersection will be highlighted in a special color (usually purple), allowing you to clearly see the scope and impact of the junction.

### 2.3 Detailed Description of the Operations Panel

The Operations Panel is your "cockpit," divided into three distinct functional modules.

**1. Congested Junctions**

- **Function:** This module monitors the city's traffic in real-time and automatically lists the top 6 most congested junctions.
- **Information Display:** Each row displays information for a congested junction, including its **"Location"** (junction name) and **"Queue Length"** (number of queued vehicles). A larger queue length value indicates more severe congestion.
- **Interaction:** This is a rapid response tool. When you click on any junction name in the list, the map will automatically focus and zoom in on that junction. Simultaneously, the junction's name will be auto-filled into the "Manual Control" module below, saving you the step of a manual search and enabling "one-click positioning and intervention readiness" for congested points.

**2. AI Suggestions**

- **Function:** This is one of the system's core intelligent features. UrbanFlow's backend AI continuously analyzes traffic flow data. When it identifies a potential optimization opportunity (e.g., alleviating a forming traffic jam by slightly adjusting signal timing), it will generate a specific optimization suggestion here.
- **Workflow:**
  - When a suggestion is available, this area will display its content (e.g., "Extend the green light for the north-south direction at junction XYZ by 15 seconds").
  - You can click the **"APPLY"** button, and the system will automatically adopt and execute the suggestion.
  - If you deem the suggestion inappropriate or have other higher-priority tasks, you can click the **"IGNORE"** button.
  - As shown in the image, when there are no available suggestions, this area will display "No Available Suggestion," and the buttons will be grayed out and disabled.

**3. Manual Control**

- **Function:** This module grants the user full manual and precise control over traffic signals, intended for handling complex situations not covered by the AI or for executing specific traffic dispatch strategies.
- **Step-by-Step Workflow:**
  1. **Select Junction:** You can auto-fill this field by clicking a traffic light icon on the map or manually search for and select a target junction from the dropdown menu here.
  2. **Select Traffic Light:** Once a junction is selected, this dropdown menu is activated, allowing you to choose the specific traffic flow direction to control (e.g., main road straight, side road left turn).
  3. **Set Light State:** Click the **"RED"** or **"GREEN"** buttons to specify the state you want the signal to switch to.
  4. **Set Duration:** Enter a number in the input box to define how long your new state will last (in seconds).
  5. **Submit Operation:**
     - Click the **"APPLY"** button to confirm and execute your settings. A success message will be shown upon successful operation.
     - Click the **"CANCEL"** button to clear all your selections and inputs in this module, allowing you to start over.

---

## 3. Dashboard Page

The Dashboard page is the data visualization and analysis center of the UrbanFlow system. Its primary function is to present complex real-time and historical traffic data in the form of intuitive charts, helping users of different roles to quickly gain insights into traffic conditions, identify problems, and evaluate the effectiveness of their strategies.

For a **Traffic Operator**, this page serves as a "report card" for evaluating the effectiveness of their signal intervention measures. The page analyzes the city's traffic health from four key dimensions through four core charts: macroscopic trends, congestion duration, traffic volume, and congestion frequency.

### 3.1 Detailed Description of Each Chart

**1. Congested Junction Count Trend**

- **Chart Meaning:** This line chart displays the trend of the total number of "congested" junctions across the city over a selected time frame (e.g., the past 24 hours as shown in the figure).
- **How to Interpret:**
  - The Y-axis represents the number of congested junctions.
  - The X-axis represents time.
  - The peaks of the curve indicate the busiest periods with the highest number of congested junctions (e.g., around 10:00 AM and 8:00 PM in the figure); the troughs represent periods of relatively smooth traffic (e.g., from 6:00 AM to 8:00 AM).
- **Analytical Value:** This chart provides the "pulse" of the city's traffic, allowing managers to grasp the macroscopic health of the entire traffic network at a glance. It clearly reveals the daily traffic peaks and troughs. The small dots on the chart (like the one at 02:00) are typically used to mark the times of manual interventions or AI-driven optimizations, making it easy to compare the change in the number of congested junctions before and after an intervention to assess its immediate effectiveness.

**2. Junction Congestion Duration Ranking**

- **Chart Meaning:** This is a horizontal bar chart that ranks junctions based on their total cumulative congestion duration within a selected time frame.
- **How to Interpret:**
  - The Y-axis lists the names of the junctions.
  - The X-axis represents the cumulative congestion time (in minutes).
  - The longer the bar, the longer that junction was in a state of congestion during the statistical period. As shown in the figure, "Park Lane" has the longest cumulative congestion duration, exceeding 80 minutes.
- **Analytical Value:** This chart precisely pinpoints the most persistent "stubborn" congestion points in the city. These junctions may be problematic due to poor road design, fundamentally flawed signal timing plans, or excessive pressure from nearby traffic-attracting points. They are key targets for urban planning and traffic optimization.

**3. Traffic Flow**

- **Chart Meaning:** This line chart shows how the traffic volume of a selected junction (or the entire city) changes over time.
- **How to Interpret:**
  - The Y-axis represents traffic flow (typically the number of vehicles passing per unit of time).
  - The X-axis represents time.
  - The fluctuations of the curve directly reflect the tidal changes in traffic demand. As shown, a clear evening peak occurred between 4:00 PM and 8:00 PM.
- **Analytical Value:** This chart is fundamental to understanding traffic demand patterns. By analyzing the flow chart for a specific junction or area, it provides key data support for signal timing optimization (e.g., adjusting green light times based on flow at different times of the day), road capacity planning, and traffic organization schemes (such as implementing tidal lanes). Users can use the dropdown menu in the upper-left corner to view the macroscopic flow for "All Junctions" or switch to a specific junction for micro-analysis.

**4. Top Congested Junctions by Frequency**

- **Chart Meaning:** This is a vertical bar chart that ranks junctions based on the number of times they experienced congestion within a selected time frame.
- **How to Interpret:**
  - The X-axis lists the names of the junctions.
  - The Y-axis represents the number of congestion events.
  - The taller the bar, the more frequently that junction entered a "congested state" during the statistical period. As shown in the figure, "Main St" is the junction where congestion occurs most frequently.
- **Analytical Value:** This chart is used to identify junctions that are most prone to congestion. Unlike the "Congestion Duration Ranking," the junctions listed here may not have long-lasting congestion, but it occurs repeatedly. This often suggests that the traffic condition at the junction is very unstable and may be highly sensitive to sudden traffic surges or minor disturbances. Analyzing this chart in conjunction with the "Junction Congestion Duration Ranking" provides a more comprehensive diagnosis of a junction's congestion characteristics.