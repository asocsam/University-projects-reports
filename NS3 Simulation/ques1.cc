#include "ns3/point-to-point-layout-module.h"
#include "ns3/internet-module.h"
#include "ns3/network-module.h"
#include "ns3/applications-module.h"
#include "ns3/core-module.h"
#include "ns3/netanim-module.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/traffic-control-module.h"
using namespace ns3;

NS_LOG_COMPONENT_DEFINE("CustomSimulation");

class CustomApp : public Application
{
public:
    CustomApp();
    virtual ~CustomApp();
    static TypeId GetTypeId(void);
    void Setup(Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate);

private:
    virtual void StartApplication(void);
    virtual void StopApplication(void);
    void ScheduleTx(void);
    void SendPacket(void);

    Ptr<Socket> m_socket;
    Address m_peer;
    uint32_t m_packetSize;
    uint32_t m_nPackets;
    DataRate m_dataRate;
    EventId m_sendEvent;
    bool m_running;
    uint32_t m_packetsSent;
};

CustomApp::CustomApp()
    : m_socket(0),
      m_peer(),
      m_packetSize(0),
      m_nPackets(0),
      m_dataRate(0),
      m_sendEvent(),
      m_running(false),
      m_packetsSent(0)
{
}

CustomApp::~CustomApp()
{
    m_socket = 0;
}

TypeId CustomApp::GetTypeId()
{
    static TypeId tid = TypeId("CustomApp")
                            .SetParent<Application>()
                            .SetGroupName("Custom")
                            .AddConstructor<CustomApp>();
    return tid;
}

void CustomApp::Setup(Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate)
{
    m_socket = socket;
    m_peer = address;
    m_packetSize = packetSize;
    m_nPackets = nPackets;
    m_dataRate = dataRate;
}

void CustomApp::StartApplication(void)
{
    NS_LOG_INFO("Starting Custom Application");
    m_running = true;
    m_packetsSent = 0;
    m_socket->Bind();
    m_socket->Connect(m_peer);
    SendPacket();
}

void CustomApp::StopApplication(void)
{
    m_running = false;
    if (m_sendEvent.IsRunning())
    {
        Simulator::Cancel(m_sendEvent);
    }
    if (m_socket)
    {
        m_socket->Close();
    }
}

void CustomApp::SendPacket(void)
{
    Ptr<Packet> packet = Create<Packet>(m_packetSize);
    m_socket->Send(packet);
    if (++m_packetsSent < m_nPackets)
    {
        ScheduleTx();
    }
}

void CustomApp::ScheduleTx(void)
{
    if (m_running)
    {
        Time tNext(Seconds(m_packetSize * 8 / static_cast<double>(m_dataRate.GetBitRate())));
        m_sendEvent = Simulator::Schedule(tNext, &CustomApp::SendPacket, this);
    }
}

void SendData(Ptr<const Packet> p)
{
    std::cout << "Send Data at " << Simulator::Now().GetSeconds() << std::endl;
}

void ReceiveData(Ptr<const Packet> p)
{
    std::cout << "Receive Data at " << Simulator::Now().GetSeconds() << std::endl;
}

int main(int argc, char *argv[])
{
    LogComponentEnable("CustomSimulation", LOG_LEVEL_INFO);
    std::string tcpType = "NewReno";
    std::string animFile = "CustomSimulation.xml"; // Animation output file name
    Config::SetDefault("ns3::TcpL4Protocol::SocketType", TypeIdValue(TypeId::LookupByName("ns3::Tcp" + tcpType)));
    uint32_t nLeftLeaf = 3;  // Number of leaf nodes on the left side
    uint32_t nRightLeaf = 3; // Number of leaf nodes on the right side
    uint32_t packetSize = 1024; // Packet size in bytes
    uint32_t nPackets = 1000; // Number of packets to be sent
    DataRate dataRate("5Mbps"); // Data rate for packet transmission

    // Link connecting left and right leaf nodes via bottleneck router
    PointToPointHelper bottleneckLink;
    bottleneckLink.SetQueue("ns3::DropTailQueue" , "MaxSize", StringValue("20p")); 
    bottleneckLink.SetDeviceAttribute("DataRate", StringValue("1.5Mbps"));
    bottleneckLink.SetChannelAttribute("Delay", StringValue("10ms"));

    // Links for leaf nodes on the left and right sides of the bottleneck routers
    PointToPointHelper leafLink;
    leafLink.SetQueue("ns3::DropTailQueue");
    leafLink.SetDeviceAttribute("DataRate", StringValue("3Mbps"));
    leafLink.SetChannelAttribute("Delay", StringValue("10ms"));

    PointToPointDumbbellHelper networkHelper(nLeftLeaf, leafLink,
                                              nRightLeaf, leafLink,
                                              bottleneckLink);
    InternetStackHelper stack;
    networkHelper.InstallStack(stack);
    networkHelper.AssignIpv4Addresses(Ipv4AddressHelper("10.1.1.0", "255.255.255.0"),
                                      Ipv4AddressHelper("10.2.1.0", "255.255.255.0"),
                                      Ipv4AddressHelper("10.3.1.0", "255.255.255.0"));

    Ipv4GlobalRoutingHelper::PopulateRoutingTables();
    Address anyAddress, sinkAddress;
    uint16_t sinkPort = 5000;

    anyAddress = InetSocketAddress(Ipv4Address::GetAny(), sinkPort);
    sinkAddress = InetSocketAddress(networkHelper.GetRightIpv4Address(0), sinkPort);

    std::cout << "Sink IP Address: ";
    networkHelper.GetRightIpv4Address(0).Print(std::cout);
    std::cout << std::endl;

    PacketSinkHelper packetSinkHelper("ns3::TcpSocketFactory", anyAddress);
    ApplicationContainer sinkApps = packetSinkHelper.Install(networkHelper.GetRight(0));
    sinkApps.Start(Seconds(0.));
    sinkApps.Stop(Seconds(10.));

    Ptr<Socket> ns3TcpSocket = Socket::CreateSocket(networkHelper.GetLeft(0), TcpSocketFactory::GetTypeId());
    std::cout << "Source IP Address: ";
    networkHelper.GetLeftIpv4Address(0).Print(std::cout);
    std::cout << std::endl;

    BulkSendHelper source("ns3::TcpSocketFactory", InetSocketAddress(networkHelper.GetRightIpv4Address(0), sinkPort));
    source.SetAttribute("MaxBytes", UintegerValue(packetSize * nPackets));
    ApplicationContainer sourceApps = source.Install(networkHelper.GetLeft(0));
    sourceApps.Start(Seconds(0.0));
    sourceApps.Stop(Seconds(10.0));

    networkHelper.BoundingBox(1, 1, 100, 100);
    AnimationInterface anim(animFile);
    anim.EnablePacketMetadata();

    Ptr<Object> traceObjClient = networkHelper.GetLeft(1)->GetDevice(0);
    traceObjClient->TraceConnectWithoutContext("PhyTxBegin", MakeCallback(&SendData));
    traceObjClient->TraceConnectWithoutContext("PhyTxEnd", MakeCallback(&SendData));
    traceObjClient->TraceConnectWithoutContext("MacTx", MakeCallback(&SendData));

    Ptr<Object> traceObjServer = networkHelper.GetRight(1)->GetDevice(0);
    traceObjServer->TraceConnectWithoutContext("PhyRxBegin", MakeCallback(&ReceiveData));
    traceObjServer->TraceConnectWithoutContext("PhyRxEnd", MakeCallback(&ReceiveData));
    traceObjServer->TraceConnectWithoutContext("MacRx", MakeCallback(&ReceiveData));

    FlowMonitorHelper flowmon;
    Ptr<FlowMonitor> monitor = flowmon.InstallAll();

    AsciiTraceHelper ascii;
    bottleneckLink.EnableAsciiAll(ascii.CreateFileStream("CustomSimulationTrace"));
    bottleneckLink.EnablePcapAll("CustomSimulationTrace");

    Simulator::Stop(Seconds(10));
    Simulator::Run();

    monitor->CheckForLostPackets();
    Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier>(flowmon.GetClassifier());
    std::map<FlowId, FlowMonitor::FlowStats> stats = monitor->GetFlowStats();
    for (std::map<FlowId, FlowMonitor::FlowStats>::const_iterator i = stats.begin(); i != stats.end(); ++i)
    {
        Ipv4FlowClassifier::FiveTuple t = classifier->FindFlow(i->first);
        if ((t.sourceAddress == "10.1.1.1" && t.destinationAddress == "10.2.1.1"))
        {
            std::cout << "Packet Size: " << packetSize << " bytes\n";
            std::cout << "Number of Packets: " << nPackets << "\n";
            std::cout << "Data Rate: " << dataRate << "\n";
            std::cout << "Flow is from " << i->first << " (" << t.sourceAddress << " to " << t.destinationAddress << ")\n";
            std::cout << "  Transmitted bytes: " << i->second.txBytes << "\n";
            std::cout << "  Received bytes: " << i->second.rxBytes << "\n";
            std::cout << "  Throughput: " << i->second.rxBytes * 8.0 / (i->second.timeLastRxPacket.GetSeconds() - i->second.timeFirstTxPacket.GetSeconds()) / 1024 / 1024 << " Mbps\n";
        }
    }
    Simulator::Destroy();

    return 0;
}
