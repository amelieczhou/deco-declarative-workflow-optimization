## **A Declarative Optimization Framework for Scientific Workflows in IaaS Clouds** ##

We propose a flexible and efficient optimization framework named Deco for resource provisioning of scientific
workflows in IaaS clouds. Deco allows users to specify
their constraints and optimizations in specific problems in
an extended declarative language. We develop a series of
optimizations to automatically improve the effectiveness of
finding a good solution for provisioning workflows, with
the consideration on cloud dynamics and user requirements.
Moreover, Deco leverages the power of GPUs to find the
solution in a fast and timely manner. We show the extensibility of Deco by expressing several common provisioning
problems. We integrate Deco into a popular workflow
management system (Pegasus) and show that Deco can
achieve more effective performance/cost optimizations than
the state-of-the-art approaches. The GPU-based acceleration
improves the response time of getting the solution by more
than an order of magnitude over the CPU-based counterpart
on a CPU with six cores.