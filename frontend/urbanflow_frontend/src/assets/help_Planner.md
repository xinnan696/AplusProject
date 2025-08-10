# Urban Planner Help Document

## 1. System Overview

Welcome to **UrbanFlow**!

UrbanFlow is an advanced platform for traffic data analysis and visualization. As an **Urban Planner**, your primary role is not to conduct real-time traffic interventions but to utilize the macroscopic traffic data provided by this system for long-term observation, analysis, and evaluation. You can review historical traffic data, identify traffic bottlenecks, and assess the long-term effectiveness of traffic strategies (such as signal timing plans) to provide scientific data support for future urban traffic planning.

After logging in, you will first enter the **Dashboard Page**, which is the core area for your data analysis.

You have permission to access the following pages: Login Page, Dashboard Page, and Help Page.

---

## 2. Dashboard Page Functionality Overview

The **Dashboard** page is the data visualization and analysis center of the UrbanFlow system. Its main function is to present complex real-time and historical traffic data in the form of intuitive charts, helping users of different roles to quickly gain insights into traffic conditions, identify problems, and evaluate the effectiveness of strategies.

For an **Urban Planner**, it is a vital data support platform for analyzing long-term traffic patterns, identifying structural traffic bottlenecks, and planning future urban traffic development.

This page analyzes the overall health of the city's traffic from four key dimensions through four core charts: macroscopic trends, congestion duration, traffic flow, and congestion frequency.

### 2.1 Detailed Description of Each Chart

### 1. Congested Junction Count Trend

Chart Meaning:

This line chart displays the changing trend in the total number of intersections in a "congested" state across the entire city over a selected time range (e.g., the last 24 hours as shown in the chart).

**How to Interpret:**

- The **Y-axis** represents the number of congested junctions.
- The **X-axis** represents time.
- The peaks of the curve indicate the busiest periods with the highest number of congested junctions (e.g., around 10:00 AM and 8:00 PM in the chart); the troughs represent periods with relatively smooth traffic flow (e.g., from 6:00 AM to 8:00 AM).

Analytical Value:

This chart provides the "pulse" of the city's traffic, allowing planners to grasp the macroscopic health of the entire traffic network at a glance. It clearly reveals the daily peak and off-peak traffic hours. The small dots on the chart (such as the one at 02:00) are typically used to mark the times of manual interventions or AI-powered optimizations, making it easy to compare the change in the number of congested junctions before and after the intervention to assess its immediate effect.

### 2. Junction Congestion Duration Ranking

Chart Meaning:

This is a horizontal bar chart that ranks individual intersections based on their total cumulative congestion duration within the selected time frame.

**How to Interpret:**

- The **Y-axis** lists the names of the individual junctions.
- The **X-axis** represents the cumulative congestion time (in minutes).
- The longer the bar, the longer the junction has been in a congested state during the statistical period. As shown in the chart, "Park Lane" has the longest cumulative congestion duration, exceeding 80 minutes.

Analytical Value:

This chart accurately pinpoints the most persistent "stubborn" congestion points in the city. These junctions may be congested due to poor road design, fundamentally flawed signal timing plans, or excessive pressure from nearby traffic-attracting points. They are key targets for urban planning and traffic optimization.

### 3. Traffic Flow

Chart Meaning:

This line chart shows the variation of traffic flow over time for a selected intersection (or the entire city).

**How to Interpret:**

- The **Y-axis** represents traffic flow (typically the number of vehicles passing per unit of time).
- The **X-axis** represents time.
- The fluctuations of the curve directly reflect the tidal changes in traffic demand. As shown, a clear evening peak occurs between 4:00 PM and 8:00 PM.

Analytical Value:

This chart is fundamental to understanding traffic demand patterns. By analyzing the flow chart for specific intersections or areas, you can obtain key data support for optimizing signal timing (e.g., adjusting green light times based on traffic flow in different periods), planning road capacity, and designing traffic organization schemes (such as setting up tidal lanes). Users can use the drop-down menu in the upper-left corner to view the macroscopic flow for "All Junctions" or switch to a specific junction for micro-level analysis.

### 4. Top Congested Times (Junction Congestion Frequency Ranking)

Chart Meaning:

This is a vertical bar chart that ranks individual intersections based on the number of times congestion occurred within the selected time frame.

**How to Interpret:**

- The **X-axis** lists the names of the individual junctions.
- The **Y-axis** represents the number of times a congestion event occurred.
- The taller the bar, the more frequently the junction "entered a state of congestion" during the statistical period. As shown in the chart, "Main St" is the most frequent location for congestion to occur.

Analytical Value:

This chart is used to identify the junctions that are most prone to congestion. Unlike the "Junction Congestion Duration Ranking," the junctions listed here may not have long-lasting congestion, but they experience it repeatedly. This often suggests that the traffic condition at the junction is highly unstable and may be very sensitive to sudden traffic surges or minor disturbances. Analyzing this chart in conjunction with the "Junction Congestion Duration Ranking" allows for a more comprehensive diagnosis of an intersection's congestion characteristics.