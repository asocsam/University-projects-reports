#include <iostream>
#include <fstream>
#include <string>
#include <cassert>

#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/internet-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/applications-module.h"
#include "ns3/ipv4-global-routing-helper.h"
#include "ns3/flow-monitor-module.h"

using namespace ns3;

NS_LOG_COMPONENT_DEFINE("FifthScriptExample");
Ptr<PacketSink> sink1;
Ptr<PacketSink> sink2;

class MyApp : public Application
{
public:
  MyApp();
  virtual ~MyApp();

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

MyApp::MyApp()
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

MyApp::~MyApp()
{
  m_socket = 0;
}

void MyApp::Setup(Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate)
{
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_nPackets = nPackets;
  m_dataRate = dataRate;
}

void MyApp::StartApplication(void)
{
  m_running = true;
  m_packetsSent = 0;
  m_socket->Bind();
  m_socket->Connect(m_peer);
  SendPacket();
}

void MyApp::StopApplication(void)
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

void MyApp::SendPacket(void)
{
  Ptr<Packet> packet = Create<Packet>(m_packetSize);
  m_socket->Send(packet);

  if (++m_packetsSent < m_nPackets)
  {
    ScheduleTx();
  }
}

void MyApp::ScheduleTx(void)
{
  if (m_running)
  {
    Time tNext(Seconds(m_packetSize * 8 / static_cast<double>(m_dataRate.GetBitRate())));
    m_sendEvent = Simulator::Schedule(tNext, &MyApp::SendPacket, this);
  }
}

static void CwndTracer(uint32_t oldCwnd, uint32_t newCwnd)
{
  NS_LOG_UNCOND(Simulator::Now().GetSeconds() << "\t" << newCwnd);
}

void CwndConnect()
{
  Config::ConnectWithoutContext("/NodeList/*/$ns3::TcpL4Protocol/SocketList/*/CongestionWindow", MakeCallback(&CwndTracer));
}

static void RxDrop(Ptr<const Packet> p)
{
  NS_LOG_UNCOND("RxDrop at " << Simulator::Now().GetSeconds());
}


int main(int argc, char *argv[])
{
  NodeContainer nodes;
  nodes.Create(6);

  PointToPointHelper link1;
  link1.SetDeviceAttribute("DataRate", StringValue("3Mbps"));
  link1.SetChannelAttribute("Delay", StringValue("10ms"));
  link1.SetQueue("ns3::DropTailQueue", "MaxSize", StringValue("30p"));

  PointToPointHelper link2;
  link2.SetDeviceAttribute("DataRate", StringValue("1.5Mbps"));
  link2.SetChannelAttribute("Delay", StringValue("10ms"));
  link2.SetQueue("ns3::DropTailQueue");

  NodeContainer n0n1 = NodeContainer(nodes.Get(0), nodes.Get(2));
  NodeContainer n2n1 = NodeContainer(nodes.Get(1), nodes.Get(2));
  NodeContainer n1n3 = NodeContainer(nodes.Get(2), nodes.Get(3));
  NodeContainer n3n4 = NodeContainer(nodes.Get(3), nodes.Get(4));
  NodeContainer n3n5 = NodeContainer(nodes.Get(3), nodes.Get(5));

  InternetStackHelper internet;
  internet.Install(nodes);

  NetDeviceContainer devices1 = link1.Install(n0n1);
  NetDeviceContainer devices2 = link1.Install(n2n1);
  NetDeviceContainer devices3 = link2.Install(n1n3);
  NetDeviceContainer devices4 = link1.Install(n3n4);
  NetDeviceContainer devices5 = link1.Install(n3n5);

  Ipv4AddressHelper ipv4;
  ipv4.SetBase("1.0.0.0", "255.0.0.0");
  ipv4.Assign(devices1);

  ipv4.SetBase("2.0.0.0", "255.0.0.0");
  ipv4.Assign(devices2);

  ipv4.SetBase("3.0.0.0", "255.0.0.0");
  ipv4.Assign(devices3);

  ipv4.SetBase("4.0.0.0", "255.0.0.0");
  ipv4.Assign(devices4);

  ipv4.SetBase("5.0.0.0", "255.0.0.0");
  ipv4.Assign(devices5);

  Ipv4GlobalRoutingHelper::PopulateRoutingTables();

  Ptr<RateErrorModel> em = CreateObject<RateErrorModel>();
  em->SetAttribute("ErrorRate", DoubleValue(0.00001));
  devices1.Get(1)->SetAttribute("ReceiveErrorModel", PointerValue(em));

  uint16_t port = 9;
  BulkSendHelper sourceTcp("ns3::TcpSocketFactory",
                           InetSocketAddress(Ipv4Address::GetAny(), port));
  sourceTcp.SetAttribute("MaxBytes", UintegerValue(0));
  sourceTcp.SetAttribute("SendSize", UintegerValue(10000));
  ApplicationContainer sourceAppsTcp = sourceTcp.Install(nodes.Get(0));
  sourceAppsTcp.Start(Seconds(1.0));
  sourceAppsTcp.Stop(Seconds(10.0));

  OnOffHelper sourceUdp("ns3::UdpSocketFactory", InetSocketAddress(Ipv4Address::GetAny(), port));
  sourceUdp.SetConstantRate(DataRate("5Mbps"));
  sourceUdp.SetAttribute("PacketSize", UintegerValue(1000));
  sourceUdp.SetAttribute("MaxBytes", UintegerValue(0));
  ApplicationContainer sourceAppsUdp = sourceUdp.Install(nodes.Get(1));
  sourceAppsUdp.Start(Seconds(1.0));
  sourceAppsUdp.Stop(Seconds(10.0));

  PacketSinkHelper sinkTcp("ns3::TcpSocketFactory",
                           InetSocketAddress(Ipv4Address::GetAny(), port));
  ApplicationContainer sinkAppsTcp = sinkTcp.Install(nodes.Get(4));
  sink1 = StaticCast<PacketSink>(sinkAppsTcp.Get(0));
  sinkAppsTcp.Start(Seconds(0.0));
  sinkAppsTcp.Stop(Seconds(10.0));

  PacketSinkHelper sinkUdp("ns3::UdpSocketFactory",
                           InetSocketAddress(Ipv4Address::GetAny(), port));
  ApplicationContainer sinkAppsUdp = sinkUdp.Install(nodes.Get(5));
  sink2 = StaticCast<PacketSink>(sinkAppsUdp.Get(0));
  sinkAppsUdp.Start(Seconds(0.0));
  sinkAppsUdp.Stop(Seconds(10.0));

  nodes.Get(1)->TraceConnectWithoutContext("PhyRxDrop", MakeCallback(&RxDrop));

  FlowMonitorHelper flowmon;
  Ptr<FlowMonitor> monitor = flowmon.InstallAll();

  Simulator::Stop(Seconds(10));
  Simulator::Run();

  monitor->CheckForLostPackets();
  Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier>(flowmon.GetClassifier());
  std::map<FlowId, FlowMonitor::FlowStats> stats = monitor->GetFlowStats();
  for (std::map<FlowId, FlowMonitor::FlowStats>::const_iterator it = stats.begin(); it != stats.end(); ++it)
  {
    Ipv4FlowClassifier::FiveTuple t = classifier->FindFlow(it->first);

    NS_LOG_DEBUG("Flow " << it->first << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")");

    std::cout << "Flow " << it->first << " (" << t.sourceAddress << " -> " << t.destinationAddress << ")\n";
    std::cout << "  Tx Bytes:   " << it->second.txBytes << "\n";
    std::cout << "  Rx Bytes:   " << it->second.rxBytes << "\n";
    std::cout << "Throughput: " << it->second.rxBytes * 8.0 / (it->second.timeLastRxPacket.GetSeconds() - it->second.timeFirstTxPacket.GetSeconds()) / 1024 / 1024 << " Mbps\n";
    // std::cout << (it->second.delaySum)/(it->second.rxPackets)<<"\n";
  }

  Simulator::Destroy();

  return 0;
}
