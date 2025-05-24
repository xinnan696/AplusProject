# Project Motivation Statement - AI Operator

## Understanding of the Project Requirements 

This project aims to build a local AI browser agent that runs on the user's own device. The user can give natural language instructions through a desktop interface, such as booking tickets, ordering groceries, or filling out online forms. The agent will automatically carry out these tasks in the browser.

Unlike cloud-based agents, this system processes all data locally, which helps protect sensitive information like login credentials and personal data from being exposed or sent to external servers.

The agent will extract key elements from the current browser page, generate a prompt using the page summary and user goal, and send it to an LLM. Based on the model's structured response, the system wll perform actions using tools like Selenium. This loop continues until the task is complete.

We plan to develop the following core modules:

- Frontend: A desktop chat interface (Electron) for input and feedback.
- Backend: Build with Java and Spring Boot to coordinate all tasks.
- Web Scraper and Parser: Uses Selenium and BeautifulSoup to extract useful data.
- LLM Module: Builds prompts and handles structured responses from GPT.
- Action Execution Module: Executes click, type, or navigation commands.
- Task Manager: Supports multi-step goals with future integration of Semantic Kernel.

This modular design keeps the system flexible, secure, and easy to extend.

## Technical Competence and Team Match

Our team is familiar with the full technical stack required for this project ans has hands on experience with many of the tools involved. We believe we are well-matched for both the developmet and planning aspects of the system.

- Product Thinking: One of our team members has over three years of experience as a product manager, which helps us design user-centered features and manage priorities.
- Frontend: We can build desktop apps using Electron to support user interaction and feedback.
- Backend:  One of our team members has over three years of professional experience in Java backend development, including experience with microservices, RESTful APIs, and system deployment.
- Automation: We have experience using Selenium to automate browser tasks.
- HTML Parsing: We are confident with tools like BeautifulSoup and lxml for extracting structured data from raw HTML.
- LLM Integration: We have worked with OpenAI GPT APIs before and understand prompt design, output formatting, and converting results into actions.

With strengths in backend development, full-stack engineering, automation, and product planning, we are confident in delivering a solid and complete solution. We are fully committed to building a useful, secure, and well-designed AI browser agent for this project.