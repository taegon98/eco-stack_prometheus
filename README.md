# [Cloud Infrastructure Resource Dashboard for Achieving Low-Carbon Private Cloud]
📎[Product Demo Video](https://youtu.be/jsv8R1l6dMs?si=uT90IfXRjgTiIyDp)

# Team
- **Mentor**: Seong Soo Cho, Openstack Korea Group
- Seok Hwan Lee, Kyungpook National University
- Tae Gon Lee, Kyungpook National University
- ji Pyo Hong, Kyungpook National University
- Hee Rim Hong, Kyungpook National University



## 1. Introduction
In this project, we have adopted a novel approach focusing on the efficient
management of the cloud, diverging from the perspective commonly found in existing
visualization dashboards such as Grafana. While conventional tools primarily
emphasize the visualization of computing resource usage, our goal extends beyond
mere information provision to offer insights into optimizing and managing cloud
resources. Consequently, the new dashboard introduces a perspective that not only
visualizes usage over time but also provides information on usage patterns by
comparing current data with historical data. To achieve this, users are presented with
computing resource usage data based on 24 hours, 7 days, and 30 days, enabling a
comprehensive understanding of usage trends across the entire cloud environment.
Such information is expected to provide tangible value in fine-tuning and optimizing
cloud resources.

## 2. Experimental Environment Setup
In order to analyze cloud usage patterns, it is essential to collect metrics on the
components that make up the cloud. Clouds are primarily based on computing
resources, and the most relevant metric for our ultimate goal of a low-carbon private
cloud is electricity usage. However, collecting electricity usage requires direct
access to physical resources, so in this project, we built an experimental environment
that focuses on collecting three metrics that can indirectly estimate electricity usage:
CPU, Memory, and Disk Usage.

## 3. CPU Metrics in Performance Monitoring: Clarification
In performance monitoring, various key metrics such as CPU utilization, average
multi- core load, per-core usage, context switches, idle threads, queue length,
interrupts, and system calls are considered. It is crucial to note that, despite sharing
the term "CPU utilization," the significance of these metrics varies.
CPU utilization, measured in a frequency-based manner, represents the ratio of time a
logical processor spends executing threads over a specified time period. On the other
hand, CPU usage, measured in a time-based manner, reflects the percentage of time
the logical processor spends executing threads within a given time frame.
When conducting performance monitoring, the fundamental metric to reference is the
CPU core usage (Usage), which indicates the percentage of time a logical processor
dedicates to executing threads within the measurement window. This is because CPU
core utilization (Utilization) represents the theoretical performance achievable by the
logical processor, assuming it continuously executes without entering an idle state. In
such cases, the calculated result may exceed 100%, leading to potential inaccuracies if
relied upon as the sole metric for performance monitoring.
Therefore, for accurate performance monitoring, it is essential to consider CPU core
usage metrics within the specified measurement interval, providing a more reliable
representation of the actual workload executed by logical processors.


## 4. Process for collecting instance-related metrics
The process of collecting and monitoring metrics in the cloud.


- ###### Fig. 1. System Architecture
![KakaoTalk_Photo_2024-01-11-23-27-13](https://github.com/taegon98/eco-stack_prometheus/assets/102223636/ebc6dbb5-1f90-4bf8-ada8-ef85702dc10f)

Monitoring in the cloud is basically collecting metrics to visualize performance trends
and provide them to users and administrators. This service uses Zabbix servers and
agents to monitor the resources of instances and the processes running inside them.
The monitoring metrics collected from Zabbix can be analyzed to provide users with
CPU, memory, and disk usage patterns.

## 5. Process for collecting instance-related metrics
Instance resource monitoring is performed as follows. Zabbix agent are installed on
the instance server to measure metrics, and they are collected by Zabbix agent using
the active method.

- ###### Table 1. Metrics
<img width="787" alt="스크린샷 2024-01-11 오후 11 28 12" src="https://github.com/taegon98/eco-stack_prometheus/assets/102223636/3869b5ae-e9ee-40b0-a0c6-14d782307915">


Metrics can provide cloud users with three main pieces of information that can
help them optimize resources and reduce carbon emissions (see Table 1).

- #### Identify and manage resource-hungry processes within an instance.
Monitoring the CPU, memory, and disk usage of each process within an instance
enables the identification of resource-intensive processes, facilitating their efficient
management. This crucial step contributes to resource optimization and the reduction
of unnecessary energy consumption.

- #### Identify which individual processes in an instance are responsible for increased
CPU usage compared to the previous day(day/week/month).
By meticulously examining processes that exhibit a rise in CPU usage when
juxtaposed with their previous day's metrics, one can adeptly discern and address
performance bottlenecks within those specific processes. This insightful analysis not
only facilitates the efficient allocation of resources but also empowers the
implementation of targeted performance optimization strategies.

- #### Manage excessive performance or unused instances.
By monitoring the CPU, memory, and disk usage of instances, we can identify
instances that either provide excessive performance or remain underutilized. This
enables the prevention of unnecessary hardware resource consumption and facilitates
more efficient management of cloud resources.
Through these methods, cloud users can contribute to reducing their carbon footprint
and improving energy efficiency. Furthermore, enhanced resource management aids
in cost savings and promotes sustainable cloud operations.

## 6. Process for collecting Hypervisor-related metrics
Hypervisor resource monitoring is performed as follows. Libvirt Exporter and Node
Exporter are installed on the hypervisor server to measure metrics, and they are
collected by Prometheus using the pull method.

- ###### Table 2. Metrics 2
<img width="783" alt="스크린샷 2024-01-11 오후 11 28 51" src="https://github.com/taegon98/eco-stack_prometheus/assets/102223636/1d30359a-3bfe-4af5-a4b5-6ec68e724700">


The optimization of overall cloud hypervisor count by consolidating instances with
low resource utilization into hypervisors with higher overcommit ratios contributes to
reducing energy consumption. By collecting usage metrics at the hypervisor level, this
approach aims to efficiently allocate resources, thereby minimizing the number of
hypervisors required while effectively managing the workload distribution across the
cloud infrastructure (see Table 2).
6
With metrics on the resource usage of individual instances and the resource usage
of the hypervisor, administrators can gain the following benefits.
>1. control Overcommitting and Under Committing: For Over committing, you can
make the best use of resources by allocating more virtual machines than available
memory and paging out or sharing memory with other virtual machines as needed .
For Under committing , you can also allocate a smaller number of virtual CPU
cores for each virtual machine to minimize competition for resources and ensure
reliability for a particular virtual machine . This enables efficient resource
allocation in the hypervisor environment by considering resource usage versus
availbale reources.
>2. Leverage preparedness metrics to predict future resource needs: Preparedness
metrics can predict future resource needs, which can be used to optimize
resource allocation.
>3. Detect and respond to abnormal situations early: By continuously monitoring
preparedness metrics, you can detect unexpected changes in resource usage and
detect and respond to overload situations early.

## 7. Process for Obtaining Collected Metrics by Users.
We can see the process of how the system responds to a client's request (see Fig. 2).
The measured metrics are collected by the Data Integration System and stored in
MongoDB. On the server, the metrics stored in MongoDB are analyzed and processed
into meaningful data to be displayed in a dashboard for users to manage resources
efficiently.


- ###### Fig. 2. Client-side System Architecture
<img width="822" alt="스크린샷 2024-01-11 오후 11 20 40" src="https://github.com/taegon98/eco-stack_prometheus/assets/102223636/5937fb21-26fd-40ce-afc6-11bd06ca6b02">


## 8. Results of the system implementation

- ###### Fig. 3. User Interface – Project Overview (Chart)
<img width="868" alt="스크린샷 2024-01-11 오후 11 18 31" src="https://github.com/taegon98/eco-stack_prometheus/assets/102223636/1ccab7a9-981e-4146-81fd-d9a669dbfa9d">

- ###### Fig. 4. User Interface – Project Overview (Top10 Resources Intensive Instances)
<img width="864" alt="스크린샷 2024-01-11 오후 11 19 13" src="https://github.com/taegon98/eco-stack_prometheus/assets/102223636/700b368c-1418-486b-817b-bff1009dfd61">

